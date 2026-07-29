<template>
  <div class="session-list">
    <div class="sl-header">
      <span class="sl-title">对话历史</span>
      <el-button text size="small" @click="$emit('new')">＋ 新建</el-button>
    </div>
    <div class="sl-search">
      <el-input
        v-model="keyword"
        placeholder="搜索…"
        size="small"
        clearable
        prefix-icon="Search"
      />
    </div>
    <div v-loading="loading" class="sl-body">
      <template v-if="filteredSessions.length">
        <div v-for="group in grouped" :key="group.label" class="sl-group">
          <div class="sl-group-label">{{ group.label }}</div>
          <div
            v-for="s in group.items"
            :key="s.id"
            :class="['sl-item', { active: s.id === currentSessionId }]"
            @click="$emit('select', s.id)"
            @mouseenter="hoveredId = s.id"
            @mouseleave="hoveredId = null"
          >
            <div class="sl-item-title">{{ s.title || '新对话' }}</div>
            <div class="sl-item-meta">
              <span>{{ fmtTime(s.updatedAt) }} · {{ s.messageCount ?? 0 }} 条</span>
              <el-button
                v-if="hoveredId === s.id"
                text
                size="small"
                type="danger"
                class="sl-del-btn"
                @click.stop="handleDelete(s)"
              >
                ✕
              </el-button>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="sl-empty">暂无对话记录</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import dayjs from 'dayjs';
import { ElMessageBox } from 'element-plus';

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  currentSessionId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
});
const emit = defineEmits(['select', 'new', 'delete']);

const keyword = ref('');
const hoveredId = ref(null);

function handleDelete(s) {
  ElMessageBox.confirm(`确定删除「${s.title || '新对话'}」？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => emit('delete', s.id))
    .catch(() => {});
}

const filteredSessions = computed(() => {
  if (!keyword.value) return props.sessions;
  const kw = keyword.value.toLowerCase();
  return props.sessions.filter((s) => s.title?.toLowerCase().includes(kw));
});

const grouped = computed(() => {
  const groups = [];
  const today = [],
    yesterday = [],
    earlier = [];
  const now = dayjs(),
    todayStart = now.startOf('day');
  const yesterdayStart = now.subtract(1, 'day').startOf('day');

  for (const s of filteredSessions.value) {
    const t = dayjs(s.updatedAt || s.createdAt);
    if (t.isAfter(todayStart)) today.push(s);
    else if (t.isAfter(yesterdayStart)) yesterday.push(s);
    else earlier.push(s);
  }
  if (today.length) groups.push({ label: '今天', items: today });
  if (yesterday.length) groups.push({ label: '昨天', items: yesterday });
  if (earlier.length) groups.push({ label: '更早', items: earlier });
  return groups;
});

function fmtTime(t) {
  if (!t) return '';
  return dayjs(t).format('HH:mm');
}
</script>

<style scoped>
.session-list {
  width: 220px;
  flex-shrink: 0;
  border-right: 0.5px solid var(--border-base);
  background: var(--bg-secondary);
  display: flex;
  flex-direction: column;
}

.sl-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 0.5px solid var(--border-base);
}

.sl-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.sl-search {
  padding: 8px 10px;
}

.sl-search :deep(.el-input__wrapper) {
  background: var(--bg-primary);
  border: 0.5px solid var(--border-base);
  box-shadow: none;
  border-radius: 6px;
}

.sl-body {
  flex: 1;
  overflow-y: auto;
  padding: 6px 8px;
}

.sl-group {
  margin-bottom: 4px;
}

.sl-group-label {
  font-size: 10px;
  color: var(--text-disabled);
  letter-spacing: 0.3px;
  padding: 6px 4px 4px;
  text-transform: uppercase;
}

.sl-item {
  padding: 6px 8px;
  border-radius: 6px;
  border: 0.5px solid transparent;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 1px;
}

.sl-item:hover {
  background: var(--bg-hover);
}

.sl-item.active {
  background: var(--bg-primary);
  border-color: var(--border-base);
}

.sl-item-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sl-item-meta {
  font-size: 10px;
  color: var(--text-secondary);
  margin-top: 2px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sl-del-btn {
  font-size: 10px;
  padding: 0 2px;
  min-height: 0;
  height: auto;
  opacity: 0.5;
  transition: opacity 0.15s;
}
.sl-del-btn:hover {
  opacity: 1;
}

.sl-empty {
  text-align: center;
  font-size: 12px;
  color: var(--text-disabled);
  padding: 32px 0;
}
</style>
