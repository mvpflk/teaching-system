// check-css.cjs — 教学系统前端 CSS 全面检测脚本 v2.0
const puppeteer = require('puppeteer-core');
const fs = require('fs');

const BASE_URL = 'http://localhost:3000';
const CHROME_PATH = 'C:/Program Files/Google/Chrome/Application/chrome.exe'; // 自行确认路径

const PAGE_LIST = [
  { path: '/login', name: '登录页' },
  { path: '/home', name: '首页' },
  { path: '/homework/list', name: '作业列表' },
  { path: '/exam/list', name: '试卷列表' },
  { path: '/exam/do/1', name: '在线考试(ID=1)' },
  { path: '/exam/result/1', name: '考试成绩(ID=1)' },
  { path: '/credit/index', name: '我的积分' },
  { path: '/credit/shop', name: '积分商城' },
  { path: '/credit/ranking', name: '积分排行' },
  { path: '/credit/admin', name: '积分管理' },
  { path: '/bbs/category/1', name: '论坛首页' },
  { path: '/bbs/post/1', name: '帖子详情(ID=1)' },
  { path: '/class/list', name: '班级列表' },
  { path: '/class/students', name: '学生管理' },
  { path: '/teacher/list', name: '教师列表' },
  { path: '/profile', name: '个人中心' },
  { path: '/wrong-book', name: '错题本' },
  { path: '/question-bank', name: '题库管理' },
  { path: '/notification', name: '消息通知' },
  { path: '/settings', name: '系统设置' },
];

const LOGIN = { username: 'admin', password: 'admin123' };
const issues = [];
const sleep = ms => new Promise(r => setTimeout(r, ms));

function add(page, cat, sev, desc, el, sug) {
  issues.push({ page, category: cat, severity: sev, description: desc, element: el, suggestion: sug });
}

async function detectFull(page, pageName) {
  console.log(`\n🔍 ${pageName}`);

  // ---------- 桌面端检测 (1440x900) ----------
  await page.setViewport({ width: 1440, height: 900 });
  await sleep(500);

  const desktop = await page.evaluate(() => {
    const p = [];
    // 1. 颜色对比度 (更准确，基于WCAG算法)
    const getLuminance = (r, g, b) => {
      const [rs, gs, bs] = [r,g,b].map(c => { c /= 255; return c <= 0.03928 ? c / 12.92 : Math.pow((c+0.055)/1.055, 2.4); });
      return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
    };
    const getContrast = (l1, l2) => (Math.max(l1,l2)+0.05) / (Math.min(l1,l2)+0.05);

    const checkContrast = (el) => {
      const style = window.getComputedStyle(el);
      const bg = style.backgroundColor;
      const color = style.color;
      if (!bg || !color || bg === 'rgba(0,0,0,0)' || color === 'rgba(0,0,0,0)') return;
      const parse = str => (str.match(/[\d.]+/g) || []).map(Number);
      const fg = parse(color), bgRgb = parse(bg);
      if (fg.length >=3 && bgRgb.length >=3) {
        const l1 = getLuminance(fg[0], fg[1], fg[2]);
        const l2 = getLuminance(bgRgb[0], bgRgb[1], bgRgb[2]);
        if (getContrast(l1, l2) < 4.5) {
          const text = el.innerText?.substring(0, 20) || el.textContent?.substring(0,20) || '';
          p.push({ cat:'颜色对比度', sev:'高', desc:`${text} 对比度不足 (${getContrast(l1,l2).toFixed(2)}:1)`, el:el.tagName, sug:'调整文字或背景色，达到至少4.5:1' });
        }
      }
    };

    // 检查文字节点
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
    let count = 0;
    while (walker.nextNode() && count < 10) {
      const node = walker.currentNode;
      const text = node.textContent.trim();
      if (text.length >= 3) {
        checkContrast(node.parentElement);
        count++;
      }
    }

    // 2. 固定宽度导致可能溢出
    const all = document.querySelectorAll('*');
    for (const el of all) {
      const style = window.getComputedStyle(el);
      const w = parseFloat(style.width);
      if (w > 1280 && style.width.endsWith('px') && el.tagName !== 'BODY' && el.tagName !== 'HTML') {
        p.push({ cat:'桌面布局', sev:'中', desc:`固定宽度 ${style.width} > 1280px，大屏可能不居中`, el:el.tagName, sug:'考虑使用 max-width: 1280px; margin: 0 auto; 或百分比宽度' });
        break; // 只报一次
      }
    }

    // 3. 使用ID选择器的数量 (超出正常范围可能代码冗余)
    const ids = document.querySelectorAll('[id]');
    if (ids.length > 50) {
      p.push({ cat:'代码质量', sev:'低', desc:`使用了 ${ids.length} 个 ID 属性，可能存在冗余`, el:'全局', sug:'减少不必要的ID，优先使用CSS类' });
    }

    return p;
  });

  desktop.forEach(d => add(pageName, d.cat, d.sev, d.desc, d.el, d.sug));

  // ---------- 移动端检测 (375x812) ----------
  await page.setViewport({ width: 375, height: 812 });
  await sleep(500);

  const mobile = await page.evaluate(() => {
    const p = [];
    const htmlWidth = document.documentElement.scrollWidth;
    if (htmlWidth > window.innerWidth + 2) {
      p.push({ cat:'响应式布局', sev:'高', desc:`水平溢出 ${htmlWidth - window.innerWidth}px`, el:'html', sug:'使用 max-width:100%; overflow-x:hidden 包裹容器' });
    }

    // 触摸尺寸
    document.querySelectorAll('a, button, input, select, textarea, .el-button, .el-link').forEach(el => {
      const rect = el.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0 && (rect.width < 44 || rect.height < 44)) {
        p.push({ cat:'移动端触控', sev:'中', desc:`触摸目标过小 (${Math.round(rect.width)}x${Math.round(rect.height)}px)`, el:el.tagName, sug:'最小尺寸设为 44x44px' });
      }
    });

    // 文字大小
    const bodyFz = parseFloat(window.getComputedStyle(document.body).fontSize);
    if (bodyFz < 14) p.push({ cat:'字体可读性', sev:'中', desc:`正文字体 ${bodyFz}px < 14px`, el:'body', sug:'移动端最小 16px，桌面端最小 14px' });

    // 表格横向滚动提示
    const table = document.querySelector('table');
    if (table && table.scrollWidth > window.innerWidth) {
      p.push({ cat:'表格适配', sev:'中', desc:'表格在移动端无横向滚动或响应式处理', el:'table', sug:'为表格容器添加 overflow-x:auto 或使用响应式表格组件' });
    }

    return p;
  });

  mobile.forEach(m => add(pageName, m.cat, m.sev, m.desc, m.el, m.sug));
}

(async () => {
  console.log('🚀 启动 Chrome...');
  const browser = await puppeteer.launch({ headless: 'new', executablePath: CHROME_PATH, args: ['--no-sandbox'] });
  const page = await browser.newPage();

  console.log('🔐 登录...');
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle2' });
  await page.type('input[placeholder="请输入用户名"]', LOGIN.username);
  await page.type('input[placeholder="请输入密码"]', LOGIN.password);
  await page.click('button.el-button--primary');
  await page.waitForNavigation({ waitUntil: 'networkidle2' });

  for (const item of PAGE_LIST) {
    try {
      await page.goto(`${BASE_URL}${item.path}`, { waitUntil: 'networkidle2', timeout: 15000 });
      await sleep(800);
      await detectFull(page, item.name);
    } catch (err) {
      add(item.name, '页面加载', '高', `无法打开: ${err.message}`, '-', '检查路由或ID');
    }
  }

  await browser.close();

  // 生成报告
  const order = { '高':1, '中':2, '低':3 };
  issues.sort((a,b) => order[a.severity] - order[b.severity]);
  let report = `# 前端CSS全面检测报告\n生成: ${new Date().toLocaleString()}\n页面: ${PAGE_LIST.length}\n问题: ${issues.length}\n\n---\n`;
  const pages = [...new Set(issues.map(i=>i.page))];
  pages.forEach(p => {
    report += `## ${p}\n`;
    issues.filter(i=>i.page===p).forEach((i,idx) => {
      report += `### ${idx+1}. [${i.severity}] ${i.category}: ${i.description}\n- 元素: \`${i.element}\`\n- 建议: ${i.suggestion}\n\n`;
    });
  });
  report += `---\n## 分批修复指南\n\n`;
  const high = issues.filter(i=>i.severity==='高');
  const mid = issues.filter(i=>i.severity==='中');
  high.length && (report += `### 🔴 第一批（立即修复）\n${high.map(i=>`- ${i.page}: ${i.description}`).join('\n')}\n\n`);
  mid.length && (report += `### 🟡 第二批（本周修复）\n${mid.map(i=>`- ${i.page}: ${i.description}`).join('\n')}\n\n`);

  fs.writeFileSync('CSS_TEST_REPORT.md', report);
  console.log(`\n✅ 报告已生成: CSS_TEST_REPORT.md`);
})();