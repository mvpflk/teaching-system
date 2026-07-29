<template>
  <div class="qp-body">
    <el-tabs v-model="questionSource" class="qp-lab-tabs">
      <el-tab-pane label="系统题库" name="system">
        <div class="qp-filter-card">
          <div class="qp-filter-row">
            <el-select v-model="sysFilter.subjectId" placeholder="学科" size="default" style="width:150px" clearable @change="$emit('sysSubjectChange', $event)">
              <el-option v-for="s in mySubjects" :key="s.id" :label="s.subjectName" :value="s.id" />
            </el-select>
            <el-select :model-value="sysChapterId" placeholder="章节" size="default" style="width:150px" clearable :disabled="!sysFilter.subjectId" @update:model-value="$emit('update:sysChapterId', $event)" @change="$emit('sysChapterChange', $event)">
              <el-option v-for="c in sysChapters" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select :model-value="sysTaskId" placeholder="任务" size="default" style="width:160px" clearable :disabled="!sysChapterId" @update:model-value="$emit('update:sysTaskId', $event)" @change="$emit('sysTaskChange', $event)">
              <el-option v-for="t in sysTasks" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
            <el-select v-model="sysFilter.categoryId" placeholder="知识点" size="default" style="width:160px" clearable filterable :disabled="!sysTaskId || sysKps.length === 0">
              <el-option v-for="k in sysKps" :key="k.id" :label="k.name" :value="k.id" />
            </el-select>
          </div>
          <div class="qp-filter-row">
            <el-select v-model="sysFilter.questionType" placeholder="题型" size="default" style="width:120px" clearable>
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTI_CHOICE" />
              <el-option label="判断题" value="TRUE_FALSE" />
              <el-option label="填空题" value="FILL_BLANK" />
              <el-option label="简答题" value="SHORT_ANSWER" />
            </el-select>
            <el-select v-model="sysFilter.difficultyLevel" placeholder="难度" size="default" style="width:100px" clearable>
              <el-option v-for="d in [1,2,3,4,5]" :key="d" :label="d+'级'" :value="d" />
            </el-select>
            <div class="qp-filter-search">
              <el-input v-model="sysFilter.keyword" placeholder="搜索题目..." clearable @keyup.enter="$emit('loadSys')">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-button type="primary" @click="$emit('loadSys')">搜索</el-button>
            </div>
          </div>
        </div>
        <div v-loading="sysLoading" class="qp-qlist">
          <div
            v-for="q in sysQuestions"
            :key="'s'+q.id"
            class="qp-qitem"
            :class="{ selected: selectedId === q.id }"
            @click="$emit('selectSystem', q)"
          >
            <span class="qp-qi-type" :class="'type-' + (q.questionType || '').toLowerCase()">{{ typeLabel(q.questionType) }}</span>
            <span class="qp-qi-text" v-html="renderMath(q.questionText?.slice(0, 120) || '无内容')" />
            <el-icon class="qp-qi-check"><CircleCheck /></el-icon>
          </div>
          <el-empty v-if="!sysLoading && !sysQuestions.length" description="系统题库暂无题目" :image-size="50" />
        </div>
        <div v-if="sysTotal > sysPageSize" class="qp-pagination">
          <el-pagination :current-page="sysPage" :page-size="sysPageSize" :total="sysTotal" layout="prev, pager, next" size="small" @current-change="$emit('pageChange', 'sys', $event)" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="本地题库" name="local">
        <div class="qp-filter-card">
          <div class="qp-filter-row">
            <el-select v-model="localFilter.subject" placeholder="学科" size="default" style="width:150px" clearable filterable @change="$emit('loadLocal')">
              <el-option v-for="s in localFilterOpts.subjects" :key="s" :label="s" :value="s" />
            </el-select>
            <el-select v-model="localFilter.tag" placeholder="类别" size="default" style="width:150px" clearable filterable @change="$emit('loadLocal')">
              <el-option v-for="t in localFilterOpts.tags" :key="t" :label="t" :value="t" />
            </el-select>
            <div class="qp-filter-search">
              <el-input v-model="localFilter.keyword" placeholder="搜索题目..." clearable @keyup.enter="$emit('loadLocal')">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-button type="primary" @click="$emit('loadLocal')">搜索</el-button>
            </div>
            <el-button type="success" @click="$emit('openImport')">
              <el-icon><Upload /></el-icon> 导入题目
            </el-button>
          </div>
        </div>
        <div v-loading="localLoading" class="qp-qlist">
          <div
            v-for="q in localQuestions"
            :key="'l'+q.id"
            class="qp-qitem"
            :class="{ selected: selectedId === q.id }"
          >
            <div style="flex:1;cursor:pointer;min-width:0" @click="$emit('selectLocal', q)">
              <span v-if="q.tag" class="qp-qi-tag">{{ q.tag }}</span>
              <span class="qp-qi-text" v-html="renderMath(q.content)" />
            </div>
            <div class="qp-qi-actions" @click.stop>
              <el-button text size="small" @click="$emit('editQuestion', q)"><el-icon><Edit /></el-icon></el-button>
              <el-button text size="small" type="danger" @click="$emit('deleteQuestion', q)"><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-icon class="qp-qi-check"><CircleCheck /></el-icon>
          </div>
          <el-empty v-if="!localLoading && !localQuestions.length" description="本地题库为空，请先上传题目" :image-size="50">
            <el-button type="primary" size="small" @click="$emit('openImport')"><el-icon><Upload /></el-icon> 导入题目</el-button>
          </el-empty>
        </div>
        <div v-if="localTotal > localPageSize" class="qp-pagination">
          <el-pagination :current-page="localPage" :page-size="localPageSize" :total="localTotal" layout="prev, pager, next" size="small" @current-change="$emit('pageChange', 'local', $event)" />
        </div>
      </el-tab-pane>

      <el-tab-pane name="ai">
        <template #label>🤖 AI推荐</template>
        <slot name="ai-tab" />
      </el-tab-pane>
    </el-tabs>

    <div class="qp-actions">
      <template v-if="selectedText">
        <div class="qp-actions-selected">
          <div class="qp-actions-check"><el-icon><CircleCheck /></el-icon></div>
          <span>已选：{{ selectedText.slice(0, 60) }}{{ selectedText.length > 60 ? '...' : '' }}</span>
          <el-button text size="small" type="danger" @click="$emit('clearSelection')">清除</el-button>
        </div>
        <button class="qp-go-btn" @click="$emit('confirmSelection')">确认选题，开始抽人 →</button>
      </template>
      <template v-else>
        <div class="qp-actions-hint">
          <el-icon class="qp-actions-hint-icon"><Pointer /></el-icon>
          <span>请从上方题库中点击选择一道题目</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search, Upload, CircleCheck, Edit, Delete, Pointer } from '@element-plus/icons-vue'
import { renderMath } from '@/composables/useQuestionHelpers'

const TYPE_CN = {
  SINGLE_CHOICE: '单选', MULTI_CHOICE: '多选', TRUE_FALSE: '判断',
  FILL_BLANK: '填空', SHORT_ANSWER: '简答'
}
const typeLabel = (t) => TYPE_CN[t] || t || '?'

// ── 内部 UI 状态（标签页切换 + 级联下拉由父组件 prop 驱动以跨阶段保持） ──
const questionSource = ref('system')

const props = defineProps({
  sysChapterId: [String, Number],
  sysTaskId: [String, Number],
  mySubjects: { type: Array, default: () => [] },
  sysChapters: { type: Array, default: () => [] },
  sysTasks: { type: Array, default: () => [] },
  sysKps: { type: Array, default: () => [] },
  sysQuestions: { type: Array, default: () => [] },
  sysLoading: Boolean,
  sysPage: Number,
  sysPageSize: Number,
  sysTotal: Number,
  sysFilter: Object,
  localQuestions: { type: Array, default: () => [] },
  localLoading: Boolean,
  localPage: Number,
  localPageSize: Number,
  localTotal: Number,
  localFilter: Object,
  localFilterOpts: Object,
  selectedText: String,
  selectedId: [String, Number]
})

const emit = defineEmits([
  'sysSubjectChange', 'sysChapterChange', 'sysTaskChange',
  'loadSys', 'loadLocal', 'openImport', 'pageChange',
  'selectSystem', 'selectLocal', 'editQuestion', 'deleteQuestion',
  'clearSelection', 'confirmSelection',
  'update:sysChapterId', 'update:sysTaskId', 'tabAi'
])

// 切换标签页到 AI 时通知父组件加载推荐题目
watch(questionSource, (val) => { if (val === 'ai') emit('tabAi') })
</script>

<style scoped>
.qp-body { flex: 1; overflow-y: auto; }
.qp-lab-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.qp-lab-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; }
.qp-filter-card { background: var(--bg-card); border: 0.5px solid var(--border-color); border-radius: var(--radius-lg); padding: 16px 18px; margin-bottom: 14px; }
.qp-filter-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.qp-filter-row + .qp-filter-row { margin-top: 10px; }
.qp-filter-search { display: flex; gap: 6px; flex: 1; min-width: 200px; }
.qp-filter-search .el-input { flex: 1; }
.qp-qlist { min-height: 100px; max-height: 380px; overflow-y: auto; }
.qp-pagination { display: flex; justify-content: center; padding: 14px 0 0; }
.qp-qitem { padding: 12px 16px; border: 0.5px solid var(--border-color); border-radius: var(--radius-md); margin-bottom: 8px; cursor: pointer; display: flex; gap: 10px; align-items: center; transition: all 0.2s ease; }
.qp-qitem:hover { border-color: var(--primary-color); background: rgba(var(--primary-color-rgb, 67,97,238), 0.03); }
.qp-qitem.selected { border-color: var(--primary-color); background: var(--primary-light); }
.qp-qi-type { font-size: var(--fs-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-xs); white-space: nowrap; flex-shrink: 0; background: var(--bg-secondary); color: var(--text-secondary); }
.qp-qi-tag { font-size: var(--fs-xs); background: var(--bg-secondary); color: var(--text-secondary); padding: 2px 8px; border-radius: var(--radius-xs); white-space: nowrap; flex-shrink: 0; }
.qp-qi-text { flex: 1; font-size: var(--fs-sm); color: var(--text-regular); line-height: 1.5; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.qp-qi-check { font-size: var(--fs-lg); color: var(--primary-color); flex-shrink: 0; opacity: 0; transition: opacity 0.2s; }
.qp-qitem.selected .qp-qi-check, .qp-qitem:hover .qp-qi-check { opacity: 0.4; }
.qp-qitem.selected .qp-qi-check { opacity: 1; }
.qp-qi-actions { flex-shrink: 0; display: flex; gap: 2px; opacity: 0; transition: opacity 0.2s; }
.qp-qitem:hover .qp-qi-actions { opacity: 1; }
.qp-actions { margin-top: 16px; padding: 16px 20px; background: var(--bg-card); border: 0.5px solid var(--border-color); border-radius: var(--radius-lg); display: flex; flex-direction: column; gap: 12px; align-items: center; }
.qp-actions-selected { display: flex; align-items: center; gap: 10px; font-size: var(--fs-sm); color: var(--primary-color); width: 100%; }
.qp-actions-selected span { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
.qp-actions-check { width: 28px; height: 28px; border-radius: 50%; background: var(--primary-light); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.qp-actions-check .el-icon { font-size: var(--fs-md); color: var(--primary-color); }
.qp-actions-hint { display: flex; align-items: center; gap: 8px; font-size: var(--fs-md); color: var(--text-disabled); padding: 4px 0; }
.qp-actions-hint-icon { font-size: var(--fs-xl); opacity: 0.4; }
.qp-go-btn { display: inline-flex; align-items: center; gap: 8px; padding: 10px 32px; border: none; border-radius: var(--radius-lg); cursor: pointer; color: #fff; font-size: var(--fs-md); font-weight: 600; background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 100%); box-shadow: 0 2px 8px rgba(var(--primary-color-rgb, 67,97,238), 0.3); transition: all 0.2s ease; }
.qp-go-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(var(--primary-color-rgb, 67,97,238), 0.4); }
.qp-go-btn:active { transform: translateY(0); }
</style>