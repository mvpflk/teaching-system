<template>
  <div class="english-weakness-panel">
    <div v-if="engStudents.length" class="eng-block">
      <div class="eng-summary">
        <span>均词汇量 <strong>{{ engData.avgVocab || 0 }}</strong></span>
      </div>
      <div class="eng-toolbar">
        <el-input
          v-model="engRemindMsg"
          size="small"
          placeholder="小P提醒你今天还有英语练习未完成哦~"
          style="width:280px"
          clearable
        />
        <el-button size="small" @click="$emit('remind', engRemindMsg)">提醒打卡</el-button>
      </div>
      <el-table
        :data="engStudents"
        size="default"
        style="margin-top:10px;border:1px solid var(--border-color)"
      >
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column label="阶段" width="70">
          <template #default="{ row }">{{ row.stage }}</template>
        </el-table-column>
        <el-table-column prop="vocabKnown" label="词汇量" width="80" />
        <el-table-column prop="streak" label="连续天" width="80" />
      </el-table>
    </div>
    <EmptyState
      v-else
      title="暂无英语数据"
      description="请选择班级后查看英语偏科情况"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'

defineProps({
  engData: { type: Object, default: () => ({ totalStudents: 0, stageCount: 0, avgVocab: 0 }) },
  engStudents: { type: Array, default: () => [] },
  selectedClassId: { type: [String, Number], default: '' }
})

defineEmits(['remind'])

const engRemindMsg = ref('')
</script>

<style scoped>
.eng-block {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
}
.eng-summary {
  margin-bottom: 10px;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.eng-summary strong {
  color: var(--text-primary);
  font-size: var(--fs-lg);
  margin: 0 4px;
}
.eng-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
}

@media (max-width: 768px) {
  .eng-block {
    padding: 12px;
  }
  .eng-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
  }
  .eng-toolbar .el-input {
    width: 100% !important;
  }
  .eng-toolbar .el-button {
    width: 100%;
  }
}
</style>
