<template>
  <!-- 4列布局：有任务层的新数据 -->
  <div v-if="hasTaskLevel" class="cc-cascade">
    <el-select
      v-model="localSubjectId"
      placeholder="学科"
      clearable
      class="cc-sel"
      data-cc-label="学科"
      @change="onSubjectChange"
    >
      <el-option
        v-for="s in subjects"
        :key="s.id"
        :label="s.subjectName"
        :value="s.id"
      />
    </el-select>
    <span class="cc-arrow">→</span>
    <el-select
      v-model="localChapterIds"
      placeholder="章节"
      :multiple="multiple"
      :clearable="true"
      collapse-tags
      collapse-tags-tooltip
      class="cc-sel"
      :class="{ 'cc-sel-multi': multiple }"
      data-cc-label="章节"
      :disabled="!localSubjectId"
      @change="onChapterChange"
    >
      <el-option
        v-for="c in chapters"
        :key="c.id"
        :label="c.name"
        :value="c.id"
      />
    </el-select>
    <!-- 多选模式下，任务层基于最后选择的章节展示（可选细化） -->
    <template v-if="!multiple || (multiple && localChapterIds.length === 1)">
      <span class="cc-arrow">→</span>
      <el-select
        v-model="localTaskId"
        placeholder="任务(可选)"
        clearable
        class="cc-sel"
        data-cc-label="任务（可选）"
        :disabled="!lastChapterId"
        @change="onTaskChange"
      >
        <el-option
          v-for="t in tasks"
          :key="t.id"
          :label="t.name"
          :value="t.id"
        >
          <span>{{ t.name }}</span>
          <el-icon
            v-if="t.hasContent"
            style="margin-left: 4px; color: var(--el-color-warning)"
          >
            <StarFilled />
          </el-icon>
        </el-option>
      </el-select>
      <span class="cc-arrow">→</span>
      <el-select
        v-model="localKpId"
        placeholder="知识点(可选)"
        clearable
        class="cc-sel"
        data-cc-label="知识点（可选）"
        :disabled="!localTaskId"
        @change="onKpChange"
      >
        <el-option
          v-for="k in knowledgePoints"
          :key="k.id"
          :label="k.name"
          :value="k.id"
        >
          <span
            :class="{
              'text-aged': k.status === 'LEGACY' || k.status === 'DEPRECATED',
              'text-obsolete': k.status === 'OBSOLETE',
            }"
          >{{ k.name }}</span>
          <el-tag
            v-if="k.status === 'LEGACY'"
            size="small"
            type="warning"
            effect="plain"
            style="margin-left: 4px; transform: scale(0.75)"
          >
            陈旧
          </el-tag>
          <el-tag
            v-else-if="k.status === 'DEPRECATED'"
            size="small"
            type="info"
            effect="plain"
            style="margin-left: 4px; transform: scale(0.75)"
          >
            过时
          </el-tag>
          <el-tag
            v-else-if="k.status === 'OBSOLETE'"
            size="small"
            type="danger"
            effect="plain"
            style="margin-left: 4px; transform: scale(0.75)"
          >
            淘汰
          </el-tag>
          <el-icon
            v-if="k.hasContent"
            style="margin-left: 4px; color: var(--el-color-warning)"
          >
            <StarFilled />
          </el-icon>
        </el-option>
      </el-select>
    </template>
    <!-- 多选模式下选中多章时，提示已覆盖的章节数 -->
    <span v-if="multiple && localChapterIds.length > 1" class="cc-multi-hint">
      已选 {{ localChapterIds.length }} 个章节 · {{ totalSubKpCount }} 个子知识点
    </span>
  </div>

  <!-- 3列布局：旧数据（无任务层，知识点直挂章节下） -->
  <div v-else class="cc-cascade">
    <el-select
      v-model="localSubjectId"
      placeholder="学科"
      clearable
      class="cc-sel"
      data-cc-label="学科"
      @change="onSubjectChangeLegacy"
    >
      <el-option
        v-for="s in subjects"
        :key="s.id"
        :label="s.subjectName"
        :value="s.id"
      />
    </el-select>
    <span class="cc-arrow">→</span>
    <el-select
      v-model="localChapterIds"
      placeholder="章节"
      :multiple="multiple"
      :clearable="true"
      collapse-tags
      collapse-tags-tooltip
      class="cc-sel"
      :class="{ 'cc-sel-multi': multiple }"
      data-cc-label="章节"
      :disabled="!localSubjectId"
      @change="onChapterChangeLegacy"
    >
      <el-option
        v-for="c in chapters"
        :key="c.id"
        :label="c.name"
        :value="c.id"
      />
    </el-select>
    <!-- 单章模式下显示知识点级联 -->
    <template v-if="!multiple || (multiple && localChapterIds.length === 1)">
      <span class="cc-arrow">→</span>
      <el-select
        v-model="localKpId"
        placeholder="知识点(可选)"
        clearable
        class="cc-sel"
        data-cc-label="知识点（可选）"
        :disabled="!lastChapterId"
        @change="onKpChange"
      >
        <el-option
          v-for="k in legacyKps"
          :key="k.id"
          :label="k.name"
          :value="k.id"
        >
          <span
            :class="{
              'text-aged': k.status === 'LEGACY' || k.status === 'DEPRECATED',
              'text-obsolete': k.status === 'OBSOLETE',
            }"
          >{{ k.name }}</span>
          <el-tag
            v-if="k.status === 'LEGACY'"
            size="small"
            type="warning"
            effect="plain"
            style="margin-left: 4px; transform: scale(0.75)"
          >
            陈旧
          </el-tag>
          <el-tag
            v-else-if="k.status === 'DEPRECATED'"
            size="small"
            type="info"
            effect="plain"
            style="margin-left: 4px; transform: scale(0.75)"
          >
            过时
          </el-tag>
          <el-tag
            v-else-if="k.status === 'OBSOLETE'"
            size="small"
            type="danger"
            effect="plain"
            style="margin-left: 4px; transform: scale(0.75)"
          >
            淘汰
          </el-tag>
          <el-icon
            v-if="k.hasContent"
            style="margin-left: 4px; color: var(--el-color-warning)"
          >
            <StarFilled />
          </el-icon>
        </el-option>
      </el-select>
    </template>
    <span v-if="multiple && localChapterIds.length > 1" class="cc-multi-hint">
      已选 {{ localChapterIds.length }} 个章节 · {{ totalSubKpCount }} 个子知识点
    </span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { StarFilled } from '@element-plus/icons-vue';
import { useKnowledgeTree } from '@/composables/useKnowledgeTree';

const props = defineProps({
  multiple: { type: Boolean, default: false },
});

const { treeData, subjects, loadData } = useKnowledgeTree();

const emit = defineEmits([
  'update:subjectId',
  'update:chapterId',
  'update:taskId',
  'update:kpId',
  'change',
]);
const chapters = ref([]);
const tasks = ref([]);
const knowledgePoints = ref([]);

const localSubjectId = ref(null);
const localChapterIds = ref(props.multiple ? [] : null);
const localTaskId = ref(null);
const localKpId = ref(null);

// 多选模式下，任务/KP 基于最后选择的章节
const lastChapterId = computed(() => {
  if (!props.multiple) return localChapterIds.value;
  const arr = localChapterIds.value || [];
  return arr.length === 1 ? arr[0] : null;
});

// 多选模式下，统计所有选中章节的子知识点总数
const totalSubKpCount = computed(() => {
  if (!props.multiple) return 0;
  const ids = localChapterIds.value;
  if (!ids || ids.length === 0) return 0;
  let count = 0;
  for (const id of ids) {
    const ch = chapters.value.find((c) => c.id === id);
    if (!ch) continue;
    const children = ch.children || [];
    // 新格式：章节→任务→知识点，统计两层
    let hasTasks = false;
    for (const task of children) {
      if (task.children && task.children.length > 0) {
        hasTasks = true;
        count += task.children.length;
      }
    }
    // 旧格式：章节直接挂知识点
    if (!hasTasks) count += children.length;
  }
  return count;
});

// 检测当前选中章节下是否有任务层
const hasTaskLevel = computed(() => {
  if (!lastChapterId.value) return false;
  const chapter = chapters.value.find((c) => c.id === lastChapterId.value);
  if (!chapter) return false;
  return (chapter.children || []).some((c) => c.children && c.children.length > 0);
});

// 旧数据模式：章节下的知识点（无任务层）
const legacyKps = computed(() => {
  if (!lastChapterId.value || hasTaskLevel.value) return [];
  const chapter = chapters.value.find((c) => c.id === lastChapterId.value);
  return chapter?.children || [];
});

// ── 新格式4列 handler ──
const onSubjectChange = (id) => {
  emit('update:subjectId', id);
  localChapterIds.value = props.multiple ? [] : null;
  localTaskId.value = null;
  localKpId.value = null;
  chapters.value = [];
  tasks.value = [];
  knowledgePoints.value = [];
  if (id) {
    const node = treeData.value.find((n) => n.subjectId === id && n.level === 1);
    chapters.value = node?.children || [];
  }
  emitChange();
};

const onChapterChange = (val) => {
  const ids = Array.isArray(val) ? val : val ? [val] : [];
  emit('update:chapterId', ids.length === 1 ? ids[0] : null);
  localTaskId.value = null;
  localKpId.value = null;
  tasks.value = [];
  knowledgePoints.value = [];
  // 基于最后选择的章节展开任务
  if (ids.length === 1) {
    const chapter = chapters.value.find((c) => c.id === ids[0]);
    const children = chapter?.children || [];
    tasks.value = children.filter((c) => c.children && c.children.length > 0);
  }
  emitChange();
};

const onTaskChange = (id) => {
  emit('update:taskId', id);
  localKpId.value = null;
  knowledgePoints.value = [];
  if (id) {
    const task = tasks.value.find((t) => t.id === id);
    knowledgePoints.value = task?.children || [];
  }
  emitChange();
};

// ── 旧格式3列 handler ──
const onSubjectChangeLegacy = (id) => {
  emit('update:subjectId', id);
  localChapterIds.value = props.multiple ? [] : null;
  localKpId.value = null;
  chapters.value = [];
  if (id) {
    const node = treeData.value.find((n) => n.subjectId === id && n.level === 1);
    chapters.value = node?.children || [];
  }
  emitChange();
};

const onChapterChangeLegacy = (val) => {
  const ids = Array.isArray(val) ? val : val ? [val] : [];
  emit('update:chapterId', ids.length === 1 ? ids[0] : null);
  localKpId.value = null;
  // 补全任务数据
  if (ids.length === 1) {
    const chapter = chapters.value.find((c) => c.id === ids[0]);
    const children = chapter?.children || [];
    tasks.value = children.filter((c) => c.children && c.children.length > 0);
    localTaskId.value = null;
  }
  emitChange();
};

// ── 共同 ──
const onKpChange = (id) => {
  emit('update:kpId', id);
  emitChange();
};

const emitChange = () => {
  const subject = subjects.value.find((s) => s.id === localSubjectId.value);
  let kpName = '',
    chapterName = '';

  // 收集所有选中章节的子知识点
  const allSubKps = [];
  if (props.multiple) {
    const ids = localChapterIds.value || [];
    for (const cid of ids) {
      const ch = chapters.value.find((c) => c.id === cid);
      if (!ch) continue;
      const children = ch.children || [];
      let hasTasks = false;
      for (const task of children) {
        if (task.children && task.children.length > 0) {
          hasTasks = true;
          for (const kp of task.children) {
            allSubKps.push({ id: kp.id, name: kp.name || '' });
          }
        }
      }
      if (!hasTasks) {
        for (const kp of children) {
          allSubKps.push({ id: kp.id, name: kp.name || '' });
        }
      }
    }
  }

  if (lastChapterId.value) {
    const ch = chapters.value.find((c) => c.id === lastChapterId.value);
    chapterName = ch?.name || '';
  }

  const payload = {
    subjectId: localSubjectId.value,
    chapterId: lastChapterId.value,
    categoryIds: props.multiple && localChapterIds.value?.length ? [...localChapterIds.value] : [],
    taskId: hasTaskLevel.value ? localTaskId.value : null,
    kpId: localKpId.value,
    kpName,
    chapterName,
    subjectName: subject?.subjectName || '',
    kpList:
      hasTaskLevel.value && localTaskId.value
        ? knowledgePoints.value.map((k) => ({ id: k.id, name: k.name }))
        : [],
    subKpList: allSubKps,
  };
  emit('change', payload);
};

onMounted(() => loadData());

defineExpose({ refresh: loadData });
</script>

<style scoped>
.cc-cascade {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.cc-sel {
  width: 140px;
}
.cc-sel-multi {
  width: 220px;
}
.cc-arrow {
  color: var(--text-disabled);
  font-size: var(--fs-md);
}
.cc-multi-hint {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 4px 12px;
  border-radius: 12px;
  white-space: nowrap;
}
.text-aged {
  opacity: 0.65;
}
.text-obsolete {
  opacity: 0.4;
  text-decoration: line-through;
}

@media (max-width: 767px) {
  .cc-cascade {
    flex-direction: column;
    align-items: stretch;
  }
  .cc-sel,
  .cc-sel-multi {
    width: 100%;
  }
  .cc-arrow {
    display: none;
  }
  /* 每个选择器前增加语义标签，替代被隐藏的箭头 */
  .cc-sel::before {
    content: attr(data-cc-label);
    display: block;
    font-size: var(--fs-xs);
    color: var(--text-secondary);
    font-weight: 500;
    margin-bottom: 4px;
  }
  .cc-multi-hint {
    text-align: center;
    margin-top: 4px;
  }
}
</style>
