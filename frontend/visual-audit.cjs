// visual-audit.cjs — 教学系统前端视觉设计审计脚本（生成纯文本报告给不支持图像的AI）
const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:3000';
const CHROME_PATH = 'C:/Program Files/Google/Chrome/Application/chrome.exe'; // 自行确认路径

const PAGE_LIST = [
  { path: '/login', name: '登录页' },
  { path: '/home', name: '首页' },
  { path: '/exam/list', name: '试卷列表' },
  { path: '/exam/do/1', name: '考试答题页' },
  { path: '/credit/ranking', name: '积分排行' },
  { path: '/bbs/category/1', name: '论坛首页' },
  { path: '/class/list', name: '班级列表' },
  { path: '/settings', name: '系统设置' },
];

const LOGIN = { username: 'admin', password: 'admin123' };
const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  console.log('🚀 启动 Chrome...');
  const browser = await puppeteer.launch({ headless: 'new', executablePath: CHROME_PATH, args: ['--no-sandbox'] });
  const page = await browser.newPage();

  // 登录
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle2' });
  await page.type('input[placeholder="请输入用户名"]', LOGIN.username);
  await page.type('input[placeholder="请输入密码"]', LOGIN.password);
  await page.click('button.el-button--primary');
  await page.waitForNavigation({ waitUntil: 'networkidle2' });

  let report = '# 视觉设计审计报告\n';
  report += `生成时间: ${new Date().toLocaleString()}\n\n`;
  report += '本报告以数据形式描述页面视觉设计，用于向不支持图像的AI提供优化依据。\n\n---\n\n';

  for (const item of PAGE_LIST) {
    try {
      await page.goto(`${BASE_URL}${item.path}`, { waitUntil: 'networkidle2', timeout: 15000 });
      await sleep(1000);
      await page.setViewport({ width: 1440, height: 900 });

      const audit = await page.evaluate(() => {
        const data = {};

        // 1. 颜色统计
        const colors = new Set();
        const bgColors = new Set();
        const allEls = document.querySelectorAll('*');
        allEls.forEach(el => {
          const style = window.getComputedStyle(el);
          const color = style.color;
          const bg = style.backgroundColor;
          if (color && color !== 'rgba(0, 0, 0, 0)') colors.add(color);
          if (bg && bg !== 'rgba(0, 0, 0, 0)') bgColors.add(bg);
        });
        // 限制数量避免过长
        data.textColors = [...colors].slice(0, 30);
        data.bgColors = [...bgColors].slice(0, 20);

        // 2. 字体大小统计
        const fontSizes = new Set();
        allEls.forEach(el => {
          const fs = window.getComputedStyle(el).fontSize;
          if (fs) fontSizes.add(fs);
        });
        data.fontSizes = [...fontSizes].sort((a, b) => parseFloat(a) - parseFloat(b));

        // 3. 主要阴影、圆角
        const shadows = new Set();
        const borderRadii = new Set();
        document.querySelectorAll('.el-card, .el-button, .el-table, .el-dialog, .el-menu, .el-input__inner, .el-select .el-input__inner, [class*="card"], [class*="panel"], [class*="box"], [style*="shadow"], [style*="border-radius"]').forEach(el => {
          const style = window.getComputedStyle(el);
          if (style.boxShadow && style.boxShadow !== 'none') shadows.add(style.boxShadow);
          if (style.borderRadius && style.borderRadius !== '0px') borderRadii.add(style.borderRadius);
        });
        data.shadows = [...shadows].slice(0, 10);
        data.borderRadii = [...borderRadii].slice(0, 10);

        // 4. 间距抽样（取几个关键元素的内边距/外边距）
        const spacingSamples = [];
        const sampleEls = document.querySelectorAll('.el-card__body, .el-table, .el-form-item, .el-dialog__body, .page-container, .main-content, [class*="container"]');
        sampleEls.forEach(el => {
          const style = window.getComputedStyle(el);
          spacingSamples.push({
            tag: el.tagName + (el.className ? '.' + el.className.split(' ')[0] : ''),
            padding: style.padding,
            margin: style.margin
          });
        });
        data.spacingSamples = spacingSamples.slice(0, 10);

        // 5. 主色调（使用最多的文字色和背景色）
        const freqColor = [...colors].map(c => ({ color: c, count: 0 }));
        allEls.forEach(el => {
          const c = window.getComputedStyle(el).color;
          freqColor.forEach(f => { if (f.color === c) f.count++; });
        });
        freqColor.sort((a, b) => b.count - a.count);
        data.dominantTextColor = freqColor[0]?.color;

        const freqBg = [...bgColors].map(c => ({ color: c, count: 0 }));
        allEls.forEach(el => {
          const bg = window.getComputedStyle(el).backgroundColor;
          freqBg.forEach(f => { if (f.color === bg) f.count++; });
        });
        freqBg.sort((a, b) => b.count - a.count);
        data.dominantBgColor = freqBg[0]?.color;

        return data;
      });

      // 格式化页面报告
      report += `## ${item.name}\n`;
      report += `- **主要文字颜色**: ${audit.dominantTextColor || '未检测到'}\n`;
      report += `- **主要背景颜色**: ${audit.dominantBgColor || '未检测到'}\n`;
      report += `- **使用的文字颜色 (最多30种)**: ${audit.textColors.join(', ')}\n`;
      report += `- **使用的背景色 (最多20种)**: ${audit.bgColors.join(', ')}\n`;
      report += `- **字体大小分布**: ${audit.fontSizes.join(', ')}\n`;
      report += `- **阴影样式 (最多10种)**: ${audit.shadows.length > 0 ? audit.shadows.join(' ; ') : '无明显阴影'}\n`;
      report += `- **常用圆角**: ${audit.borderRadii.join(', ') || '无统一圆角'}\n`;
      report += `- **间距抽样**:\n`;
      audit.spacingSamples.forEach(s => {
        report += `  - ${s.tag}: padding=${s.padding}, margin=${s.margin}\n`;
      });
      report += '\n';
      console.log(`✅ 已审计: ${item.name}`);
    } catch (err) {
      report += `## ${item.name}\n- **错误**: ${err.message}\n\n`;
      console.error(`❌ ${item.name} 审计失败: ${err.message}`);
    }
  }

  // 综
  report += '---\n\n';
  report += '## 全局设计特点总结\n';
  report += '(请AI分析上述数据，重点指出：颜色是否过多、字体大小层级是否缺失、阴影与圆角是否统一、间距是否拥挤或过于松散)\n';

  fs.writeFileSync('VISUAL_AUDIT_REPORT.md', report);
  console.log('✅ 审计报告已生成: VISUAL_AUDIT_REPORT.md');
  await browser.close();
})();