<template>
  <div class="ep-hub">
    <div class="ep-header">
      <h2><el-icon><Tickets /></el-icon> AI 智能组卷</h2>
      <p class="ep-sub">选择组卷模式，AI 帮你高效命题</p>
    </div>
    <div class="ep-grid">
      <!-- 第一行：新手推荐 -->
      <el-card shadow="never" class="ep-card ep-card--quick" @click="quickVisible = true">
        <div class="ep-card-badge">⚡ 新手推荐</div>
        <div class="ep-card-icon">⚡</div>
        <h3>一键示例卷</h3>
        <p>自动识别学科，5~8题小测验即时生成，无需任何配置，可编辑后保存</p>
        <div class="ep-card-tags">
          <el-tag size="small" type="warning">一键生成</el-tag>
          <el-tag size="small">自动适配学科</el-tag>
          <el-tag size="small" type="success">可编辑保存</el-tag>
        </div>
        <el-button type="warning" size="large" style="margin-top:16px;width:100%">⚡ 立即生成 →</el-button>
      </el-card>

      <!-- 第二行：仿真组卷 -->
      <el-card shadow="never" class="ep-card" @click="$router.push('/teacher/ai/exam-paper/create?mode=exam')">
        <div class="ep-card-icon">🏫</div>
        <h3>仿真组卷</h3>
        <p>模拟对口升学考试，题型分值对标真题，智能校验考纲覆盖率</p>
        <div class="ep-card-tags">
          <el-tag size="small" type="primary">考点覆盖广</el-tag>
          <el-tag size="small" type="warning">考试标准</el-tag>
          <el-tag size="small">适配真题风格</el-tag>
        </div>
        <el-button type="primary" size="large" style="margin-top:16px;width:100%">开始组卷 →</el-button>
      </el-card>

      <!-- 第三行：专题训练 -->
      <el-card shadow="never" class="ep-card" @click="$router.push('/teacher/ai/exam-paper/create?mode=training')">
        <div class="ep-card-icon">📝</div>
        <h3>专题训练</h3>
        <p>聚焦特定知识点/模块，灵活配置题型和难度，适合日常练习和巩固</p>
        <div class="ep-card-tags">
          <el-tag size="small" type="success">聚焦单点</el-tag>
          <el-tag size="small">自由配置</el-tag>
          <el-tag size="small" type="info">可分批布置</el-tag>
        </div>
        <el-button size="large" style="margin-top:16px;width:100%">创建练习 →</el-button>
      </el-card>

      <!-- 第四行：专业大类综合卷 -->
      <el-card shadow="never" class="ep-card ep-card--major" @click="majorVisible = true">
        <div class="ep-card-badge" style="background:var(--el-color-success, #67c23a)">专业课</div>
        <div class="ep-card-icon">🏢</div>
        <h3>专业大类综合卷</h3>
        <p>按专业大类（计算机/农学/建筑等）合并多门专业课知识点，按考纲比例生成综合试卷</p>
        <div class="ep-card-tags">
          <el-tag size="small" type="success">跨学科组卷</el-tag>
          <el-tag size="small">考纲比例分配</el-tag>
          <el-tag size="small" type="info">综合模拟</el-tag>
        </div>
        <el-button type="success" size="large" style="margin-top:16px;width:100%">🏢 综合组卷 →</el-button>
      </el-card>
    </div>

    <QuickExamDialog v-model="quickVisible" />
    <MajorExamDialog v-model="majorVisible" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Tickets } from '@element-plus/icons-vue'
import QuickExamDialog from '@/components/ai/QuickExamDialog.vue'
import MajorExamDialog from '@/components/ai/MajorExamDialog.vue'

const quickVisible = ref(false)
const majorVisible = ref(false)
</script>

<style scoped>
.ep-hub { max-width: 900px; margin: 0 auto; padding: 24px; }
.ep-header { margin-bottom: 28px; text-align: center; }
.ep-header h2 { font-size: 22px; display: flex; align-items: center; justify-content: center; gap: 8px; margin: 0 0 8px; }
.ep-sub { font-size: var(--fs-sm); color: var(--el-color-info); margin: 0; }
.ep-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
@media (max-width: 640px) {
  .ep-grid { grid-template-columns: 1fr; }
}
.ep-card { cursor: pointer; border: 0.5px solid #e4e7ed; transition: border-color 0.2s, transform 0.15s; text-align: center; padding: 32px 24px; }
.ep-card:hover { border-color: var(--primary-color); transform: translateY(-2px); }
.ep-card-icon { font-size: 48px; margin-bottom: 12px; }
.ep-card h3 { font-size: 17px; margin: 0 0 8px; color: #303133; }
.ep-card p { font-size: var(--fs-sm); color: var(--el-color-info); margin: 0 0 14px; line-height: 1.5; min-height: 36px; }
.ep-card-tags { display: flex; justify-content: center; gap: 6px; flex-wrap: wrap; }

/* 一键示例卷卡片 */
.ep-card--quick {
  position: relative;
  border: 2px dashed var(--el-color-warning, #e6a23c);
  background: linear-gradient(135deg, #fef9f0, #fffdf7);
}
.ep-card--quick:hover { border-color: #d89614; }
.ep-card--quick .ep-card-icon { filter: drop-shadow(0 2px 4px rgba(230, 162, 60, 0.3)); }

.ep-card--major {
  position: relative;
  border: 2px dashed var(--el-color-success, #67c23a);
  background: linear-gradient(135deg, #f0fdf4, #f6ffed);
}
.ep-card--major:hover { border-color: #5daf34; }

.ep-card-badge {
  position: absolute; top: 12px; right: 16px;
  background: var(--el-color-warning, #e6a23c); color: #fff; padding: 2px 12px;
  border-radius: 12px; font-size: var(--fs-xs); font-weight: 700;
}
</style>
