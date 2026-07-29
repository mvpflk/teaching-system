<template>
  <div class="qpc-idle-new">
    <div class="qpc-in-hero">
      <div class="qpc-in-badge">
        <div class="qpc-in-badge-ring"></div>
        <el-icon><Aim /></el-icon>
      </div>
      <h2 class="qpc-in-title">课堂抽问</h2>
      <p class="qpc-in-sub">随机选题 · 随机抽人 · 即时评分</p>
    </div>

    <div class="qpc-in-config">
      <div class="qpc-in-section">
        <div class="qpc-in-section-head">
          <el-icon><Collection /></el-icon>
          <span>选择题库</span>
        </div>
        <div class="qpc-in-steps">
          <div class="qpc-in-step" :class="{ active: sel.subjectId, done: sel.taskId }">
            <div class="qpc-in-step-dot">1</div>
            <div class="qpc-in-step-body">
              <span class="qpc-in-step-label">学科</span>
              <el-select v-model="sel.subjectId" placeholder="选择学科" size="large" style="width:100%" @change="$emit('subjectChange', $event)">
                <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
              </el-select>
            </div>
          </div>
          <div class="qpc-in-step" :class="{ active: sel.chapterId, done: sel.taskId }">
            <div class="qpc-in-step-dot">2</div>
            <div class="qpc-in-step-body">
              <span class="qpc-in-step-label">章节</span>
              <el-select v-model="sel.chapterId" placeholder="选择章节" size="large" style="width:100%" :disabled="!sel.subjectId" @change="$emit('chapterChange', $event)">
                <el-option v-for="ch in chapters" :key="ch.id" :label="ch.name" :value="ch.id" />
              </el-select>
            </div>
          </div>
          <div class="qpc-in-step" :class="{ active: sel.taskId }">
            <div class="qpc-in-step-dot">3</div>
            <div class="qpc-in-step-body">
              <span class="qpc-in-step-label">任务</span>
              <el-select v-model="sel.taskId" placeholder="选择任务" size="large" style="width:100%" :disabled="!sel.chapterId" @change="$emit('taskChange', $event)">
                <el-option v-for="t in tasks" :key="t.id" :label="t.name" :value="t.id" />
              </el-select>
            </div>
          </div>
        </div>
      </div>

      <div class="qpc-in-stats">
        <div class="qpc-in-stat-card">
          <div class="qpc-in-stat-icon students"><el-icon><UserFilled /></el-icon></div>
          <div class="qpc-in-stat-body">
            <span class="qpc-in-stat-num">{{ studentPool.length }}</span>
            <span class="qpc-in-stat-label">可用学生</span>
          </div>
          <span v-if="absentCount" class="qpc-in-stat-note">缺席 {{ absentCount }}</span>
        </div>
        <div class="qpc-in-stat-card">
          <div class="qpc-in-stat-icon questions"><el-icon><Notebook /></el-icon></div>
          <div class="qpc-in-stat-body">
            <span class="qpc-in-stat-num">{{ questionPool.length }}</span>
            <span class="qpc-in-stat-label">题目数量</span>
          </div>
          <div class="qpc-in-stat-actions">
            <el-button size="small" @click="$emit('openImport')"><el-icon><Upload /></el-icon> 导入</el-button>
            <el-button v-if="sel.taskId && questionPool.length" size="small" text type="danger" @click="$emit('clearPool')">清空</el-button>
          </div>
        </div>
      </div>
    </div>

    <button class="qpc-go-btn qpc-go-btn--big" :disabled="!questionPool.length || !studentPool.length" @click="$emit('start')">
      <span class="qpc-go-icon"><el-icon><Aim /></el-icon></span>
      <span class="qpc-go-text">开始抽题抽人</span>
    </button>
    <p v-if="!sel.taskId" class="qpc-in-hint">请选择学科 → 章节 → 任务以加载题库</p>
    <p v-else-if="!questionPool.length" class="qpc-in-hint">该任务下暂无题库，请导入题目</p>
  </div>
</template>

<script setup>
import { Aim, Collection, UserFilled, Notebook, Upload } from '@element-plus/icons-vue'

defineProps({
  sel: Object,
  subjects: { type: Array, default: () => [] },
  chapters: { type: Array, default: () => [] },
  tasks: { type: Array, default: () => [] },
  studentPool: { type: Array, default: () => [] },
  questionPool: { type: Array, default: () => [] },
  absentCount: Number
})

defineEmits(['subjectChange', 'chapterChange', 'taskChange', 'openImport', 'clearPool', 'start'])
</script>

<style scoped>
.qpc-idle-new { max-width: 680px; margin: 0 auto; padding: 48px 24px 40px; text-align: center; }
.qpc-in-hero { margin-bottom: 36px; }
.qpc-in-badge { width: 88px; height: 88px; margin: 0 auto 18px; border-radius: 50%; background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 100%); display: flex; align-items: center; justify-content: center; position: relative; }
.qpc-in-badge .el-icon { font-size: 40px; color: #fff; position: relative; z-index: 1; }
.qpc-in-badge-ring { position: absolute; inset: -6px; border-radius: 50%; border: 2px solid rgba(var(--primary-color-rgb, 67,97,238), 0.2); animation: badgePulse 2.5s ease-in-out infinite; }
@keyframes badgePulse { 0%, 100% { transform: scale(1); opacity: 0.6; } 50% { transform: scale(1.08); opacity: 1; } }
.qpc-in-title { font-size: 28px; font-weight: 800; color: var(--text-primary); margin: 0 0 6px; }
.qpc-in-sub { font-size: var(--fs-md); color: var(--text-secondary); margin: 0; letter-spacing: 0.08em; }
.qpc-in-config { background: var(--bg-card); border: 0.5px solid var(--border-color); border-radius: var(--radius-xl); padding: 28px; text-align: left; margin-bottom: 32px; }
.qpc-in-section { margin-bottom: 20px; }
.qpc-in-section-head { display: flex; align-items: center; gap: 6px; font-size: var(--fs-sm); font-weight: 600; color: var(--text-secondary); margin-bottom: 16px; text-transform: uppercase; letter-spacing: 0.06em; }
.qpc-in-section-head .el-icon { font-size: var(--fs-md); }
.qpc-in-steps { display: flex; flex-direction: column; }
.qpc-in-step { display: flex; align-items: flex-start; gap: 14px; position: relative; padding-bottom: 20px; }
.qpc-in-step::before { content: ''; position: absolute; left: 13px; top: 28px; bottom: 0; width: 2px; background: var(--border-light); transition: background 0.3s ease; }
.qpc-in-step:last-child::before { display: none; }
.qpc-in-step.active::before, .qpc-in-step.done::before { background: var(--primary-color); opacity: 0.3; }
.qpc-in-step-dot { width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: var(--fs-xs); font-weight: 700; background: var(--bg-secondary); color: var(--text-disabled); border: 2px solid var(--border-light); transition: all 0.3s ease; }
.qpc-in-step.active .qpc-in-step-dot { background: var(--primary-color); color: #fff; border-color: var(--primary-color); }
.qpc-in-step.done .qpc-in-step-dot { background: var(--el-color-success); color: #fff; border-color: var(--el-color-success); }
.qpc-in-step-body { flex: 1; min-width: 0; padding-top: 2px; }
.qpc-in-step-label { display: block; font-size: var(--fs-xs); font-weight: 600; color: var(--text-secondary); margin-bottom: 6px; }
.qpc-in-step.active .qpc-in-step-label { color: var(--primary-color); }
.qpc-in-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.qpc-in-stat-card { display: flex; align-items: center; gap: 14px; padding: 16px 18px; border-radius: var(--radius-lg); background: var(--bg-section); border: 0.5px solid var(--border-light); }
.qpc-in-stat-card:hover { border-color: var(--border-color); }
.qpc-in-stat-icon { width: 44px; height: 44px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.qpc-in-stat-icon .el-icon { font-size: var(--fs-xl); }
.qpc-in-stat-icon.students { background: rgba(var(--primary-color-rgb, 67,97,238), 0.1); color: var(--primary-color); }
.qpc-in-stat-icon.questions { background: rgba(var(--primary-color-rgb), 0.1); color: var(--primary-color); }
.qpc-in-stat-body { flex: 1; min-width: 0; }
.qpc-in-stat-num { display: block; font-size: var(--fs-2xl); font-weight: 800; color: var(--text-primary); line-height: 1.2; }
.qpc-in-stat-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; display: block; }
.qpc-in-stat-note { font-size: var(--fs-xs); color: var(--el-color-warning); font-weight: 500; flex-shrink: 0; }
.qpc-in-stat-actions { flex-shrink: 0; display: flex; gap: 4px; }
.qpc-go-btn--big { padding: 0 56px; height: 64px; font-size: var(--fs-xl); gap: 12px; }
.qpc-go-btn { display: inline-flex; align-items: center; gap: 10px; border: none; border-radius: var(--radius-xl); cursor: pointer; color: #fff; font-weight: 700; background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 100%); box-shadow: 0 4px 16px rgba(var(--primary-color-rgb, 67,97,238), 0.35); transition: all 0.25s ease; }
.qpc-go-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(var(--primary-color-rgb, 67,97,238), 0.45); }
.qpc-go-btn:active:not(:disabled) { transform: translateY(0) scale(0.98); }
.qpc-go-btn:disabled { opacity: 0.35; cursor: not-allowed; box-shadow: none; transform: none; }
.qpc-go-icon { display: flex; align-items: center; }
.qpc-in-hint { margin-top: 14px; font-size: var(--fs-sm); color: var(--text-disabled); }
</style>
