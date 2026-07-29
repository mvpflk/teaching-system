<template>
  <el-dialog
    v-model="visible"
    title="题目预览"
    width="580px"
    append-to-body
  >
    <div v-if="question.id" class="preview-box">
      <QuestionRenderer
        :question="question"
        mode="display"
        :show-answer="canSeeAnswer"
        :show-explanation="canSeeAnswer"
        :highlight-correct="canSeeAnswer"
        :show-meta="true"
      />
      <div v-if="question.knowledgePoints" class="preview-field">
        <span class="label">知识点：</span>{{ question.knowledgePoints }}
      </div>
      <div v-if="question.subject" class="preview-field">
        <span class="label">学科：</span>{{ question.subject }}
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button
        type="primary"
        @click="
          emit('edit', question);
          visible = false;
        "
      >
        编辑
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue';
import { useUserStore } from '@/stores/user';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';

const props = defineProps({
  modelValue: Boolean,
  question: { type: Object, default: () => ({}) },
});
const emit = defineEmits(['update:modelValue', 'edit']);

const userStore = useUserStore();
const canSeeAnswer = computed(
  () => userStore.isAdmin || userStore.isSuperAdmin || userStore.isTeacher
);
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) });
</script>

<style scoped>
.preview-box {
  line-height: 1.7;
}
.preview-field {
  font-size: var(--fs-sm);
  margin-bottom: 4px;
}
.label {
  color: var(--text-secondary);
}
</style>
