<template>
  <div class="page-card">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title">试卷库</h3>
      </div>
      <div class="header-actions" style="display:flex;gap:8px">
        <el-button @click="$router.push('/paper-import')">
          <el-icon><Upload /></el-icon> 导入试卷
        </el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <el-form :inline="true" class="search-form">
      <el-form-item label="学科">
        <el-input
          v-model="filters.subject"
          placeholder="输入学科名称"
          size="small"
          clearable
          style="width:200px"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="search"><el-icon><Search /></el-icon> 搜索</el-button>
        <el-button size="small" @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表加载 -->
    <div v-loading="loading" class="paper-grid">
      <div v-if="!loading && !list.length" class="empty-state">
        <el-empty description="暂无试卷">
          <el-button type="primary" @click="$router.push('/paper-import')">导入第一份试卷</el-button>
        </el-empty>
      </div>

      <div v-for="paper in list" :key="paper.id" class="paper-card">
        <div class="paper-card__header">
          <span class="paper-title">
            <el-icon v-if="paper.lockedAt" style="color:var(--primary-color);margin-right:4px"><Lock /></el-icon>
            {{ paper.title }}
          </span>
          <div class="paper-tags">
            <el-tag v-if="paper.subject" size="small">{{ paper.subject }}</el-tag>
            <el-tag v-if="paper.isStandardized === 1" size="small" type="warning">
              {{ paper.paperRole === 'PRETEST' ? '前测' : paper.paperRole === 'POSTTEST' ? '后测' : paper.paperRole === 'MIDTEST' ? '中期' : '标准化' }}
            </el-tag>
          </div>
        </div>
        <div class="paper-card__stats">
          <span class="stat-item">{{ paper.questionCount || 0 }} 题</span>
          <span class="stat-item">{{ paper.totalScore }} 分</span>
          <span class="stat-item">{{ paper.durationMinutes || '--' }} 分钟</span>
        </div>
        <div class="paper-card__meta">
          <span class="meta-date">{{ formatDate(paper.createdAt) }}</span>
          <span class="meta-usage">使用 {{ paper.useCount || 0 }} 次</span>
        </div>
        <div class="paper-card__actions">
          <el-popover trigger="click" :width="360" placement="bottom-start">
            <template #reference>
              <el-button size="small" type="primary" :disabled="creating === paper.id || paper.lockedAt">
                {{ creating === paper.id ? '创建中...' : '创建任务' }}
              </el-button>
            </template>
            <template #default>
              <div class="create-task-pop">
                <p style="margin:0 0 10px;font-weight:500">{{ paper.title }}</p>
                <el-select
                  v-model="targetMap[paper.id]"
                  multiple
                  filterable
                  placeholder="选择目标班级"
                  style="width:100%;margin-bottom:10px"
                >
                  <el-option
                    v-for="c in classes"
                    :key="c.id"
                    :label="c.className || c.name"
                    :value="c.id"
                  />
                </el-select>
                <div style="display:flex;gap:8px;justify-content:flex-end">
                  <el-button size="small" @click="targetMap[paper.id] = []">取消</el-button>
                  <el-button size="small" type="primary" @click="createTask(paper)">创建任务</el-button>
                </div>
              </div>
            </template>
          </el-popover>
          <el-dropdown v-if="!paper.lockedAt" @command="(cmd) => handleResearchAction(cmd, paper)" trigger="click">
            <el-button size="small" text>
              <el-icon><Document /></el-icon> 研究
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="mark-pretest">标记为前测试卷</el-dropdown-item>
                <el-dropdown-item command="mark-posttest">标记为后测试卷</el-dropdown-item>
                <el-dropdown-item command="mark-midtest">标记为中期测试卷</el-dropdown-item>
                <el-dropdown-item command="mark-common">标记为标准化试卷</el-dropdown-item>
                <el-dropdown-item divided command="lock">锁定试卷</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button
            v-if="!paper.lockedAt"
            size="small"
            text
            type="danger"
            @click="confirmDelete(paper)"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        small
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, Document } from '@element-plus/icons-vue'
import { listPapers, deletePaper, createTaskFromPaper } from '@/api/paperImport'
import { markStandardized, lockPaper } from '@/api/research'
import { getMyClasses } from '@/api/classes'

// ── 状态 ──
const loading = ref(false)
const creating = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const classes = ref([])
const targetMap = reactive({})

const filters = reactive({
  subject: ''
})

// ── 生命周期 ──
onMounted(async () => {
  await loadData()
  try {
    const res = await getMyClasses()
    if (res.code === 200) classes.value = res.data || []
  } catch { /* ignore */ }
})

// ── 方法 ──
const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filters.subject) params.subject = filters.subject
    const res = await listPapers(params)
    if (res.code === 200) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      list.value = []
      total.value = 0
    }
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const search = () => { page.value = 1; loadData() }
const resetFilters = () => { filters.subject = ''; page.value = 1; loadData() }

const createTask = async (paper) => {
  const ids = targetMap[paper.id]
  if (!ids || !ids.length) { ElMessage.warning('请选择目标班级'); return }
  creating.value = paper.id
  try {
    const res = await createTaskFromPaper(paper.id, { targetIds: ids, publishNow: false })
    if (res.code === 200) {
      ElMessage.success('任务已创建')
      targetMap[paper.id] = []
      loadData()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.message || e))
  } finally {
    creating.value = null
  }
}

const confirmDelete = (paper) => {
  ElMessageBox.confirm(`确定删除试卷《${paper.title}》？`, '确认删除', { type: 'warning' })
    .then(async () => {
      try {
        const res = await deletePaper(paper.id)
        if (res.code === 200) {
          ElMessage.success('已删除')
          loadData()
        } else {
          ElMessage.error(res.message || '删除失败')
        }
      } catch (e) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

const formatDate = (d) => {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
}

// ── P0-1: 标准化试卷标记 ──
const roleMap = {
  'mark-pretest': 'PRETEST',
  'mark-posttest': 'POSTTEST',
  'mark-midtest': 'MIDTEST',
  'mark-common': 'COMMON'
}
const roleLabelMap = {
  'mark-pretest': '前测试卷',
  'mark-posttest': '后测试卷',
  'mark-midtest': '中期测试卷',
  'mark-common': '标准化试卷'
}

const handleResearchAction = async (command, paper) => {
  if (command === 'lock') return handleLockPaper(paper)
  const role = roleMap[command]
  const label = roleLabelMap[command]
  try {
    await ElMessageBox.confirm(
      `确定将《${paper.title}》标记为"${label}"？标记后试卷内容不可修改。`,
      '确认标记',
      { type: 'warning' }
    )
    const res = await markStandardized(paper.id, role)
    if (res.code === 200) {
      ElMessage.success(`已标记为${label}`)
      loadData()
    } else {
      ElMessage.error(res.message || '标记失败')
    }
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error('标记失败: ' + (e?.message || e))
    }
  }
}

const handleLockPaper = async (paper) => {
  try {
    await ElMessageBox.confirm(
      `确定锁定《${paper.title}》？锁定后试卷内容不可修改，仅管理员可解锁。`,
      '确认锁定',
      { type: 'error' }
    )
    const res = await lockPaper(paper.id)
    if (res.code === 200) {
      ElMessage.success('试卷已锁定')
      loadData()
    } else {
      ElMessage.error(res.message || '锁定失败')
    }
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error('锁定失败: ' + (e?.message || e))
    }
  }
}
</script>

<style scoped>
.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 16px 0;
}
.paper-card {
  border: 0.5px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  background: var(--bg-card);
  transition: border-color 0.15s;
}
.paper-card:hover { border-color: var(--primary-color); }
.paper-card__header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.paper-title { font-weight: 600; font-size: var(--fs-md); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: flex; align-items: center; }
.paper-tags { display: flex; gap: 4px; flex-wrap: wrap; }
.paper-card__stats { display: flex; gap: 16px; margin-bottom: 8px; font-size: var(--fs-sm); color: var(--text-regular); }
.paper-card__meta { display: flex; gap: 16px; font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 12px; }
.paper-card__actions { display: flex; gap: 8px; justify-content: flex-end; border-top: 1px solid var(--border-light); padding-top: 10px; }
.empty-state { grid-column: 1 / -1; padding: 60px 0; }
.pagination-wrapper { display: flex; justify-content: center; padding: 20px 0; }
.create-task-pop p { font-size: var(--fs-md); }
.search-form { margin-bottom: 8px; }
</style>
