<template>
  <div class="ph-diag-result">
    <van-circle
      :rate="result.score"
      :text="result.score + '%'"
      color="var(--primary-color)"
      layer-color="var(--bg-secondary)"
      size="100"
      :stroke-width="60"
    />
    <span class="ph-diag-label">{{ result.correctCount }} / {{ result.totalQuestions }} 题正确</span>
    <span v-if="result.isFirstDiagnosis" class="ph-diag-change ph-diag-first">首次摸底</span>
    <span
      v-else-if="result.scoreChange > 0"
      class="ph-diag-change ph-diag-up"
    >↑ 比上次 +{{ result.scoreChange }} 分</span>
    <span
      v-else-if="result.scoreChange < 0"
      class="ph-diag-change ph-diag-down"
    >↓ 比上次 {{ result.scoreChange }} 分</span>
    <span v-else class="ph-diag-change ph-diag-same">与上次持平</span>
    <span class="ph-diag-subject">{{ result.subject }}</span>
    <div v-if="result.typeBreakdown" class="ph-diag-type-stats">
      <span
        v-if="result.typeBreakdown.choice?.total"
        class="ph-type-stat"
      >选择/判断 {{ result.typeBreakdown.choice.correct }}/{{
        result.typeBreakdown.choice.total
      }}</span>
      <span
        v-if="result.typeBreakdown.fillIn?.total"
        class="ph-type-stat"
      >填空 {{ result.typeBreakdown.fillIn.correct }}/{{
        result.typeBreakdown.fillIn.total
      }}</span>
      <span
        v-if="result.typeBreakdown.essay?.total"
        class="ph-type-stat ph-type-pending"
      >问答 {{ result.typeBreakdown.essay.pending }}/{{
        result.typeBreakdown.essay.total
      }}
        待评</span>
    </div>
    <div v-if="result.level" class="ph-diag-analysis">
      <div class="ph-diag-level">
        <van-tag :type="result.score >= 60 ? 'success' : 'info'" size="medium" effect="plain">
          {{
            result.level
          }}
        </van-tag>
        <span
          v-if="result.estimatedScore"
          class="ph-diag-estimate"
        >估分约 {{ result.estimatedScore }} 分</span>
      </div>
      <p class="ph-diag-advice">{{ result.advice }}</p>
      <p v-if="result.autoGroupNote" class="ph-diag-autogroup-note">
        <van-icon name="info-o" /> {{ result.autoGroupNote }}
      </p>
    </div>
    <div v-if="result.itemResults?.length" class="ph-item-results">
      <van-collapse v-model="detailOpen" accordion>
        <van-collapse-item title="查看答题详情与参考答案" name="detail" class="ph-detail-collapse">
          <div class="ph-scoring-rule">
            <van-notice-bar
              left-icon="info-o"
              scrollable
              :text="result.scoringRule || '判分规则'"
              background="var(--primary-light)"
              color="var(--primary-color)"
            />
          </div>
          <div
            v-for="(item, ii) in result.itemResults"
            :key="ii"
            class="ph-item-row"
            :class="itemRowClass(item)"
          >
            <div class="ph-item-head">
              <span class="ph-item-num">第{{ ii + 1 }}题</span>
              <span class="ph-item-type-tag">{{ typeLabel(item.questionType) }}</span>
              <van-tag
                v-if="item.isCorrect"
                type="success"
                size="mini"
                effect="plain"
              >
                正确
              </van-tag>
              <van-tag
                v-else-if="item.matchMode === 'ai_graded'"
                type="success"
                size="mini"
                effect="plain"
              >
                AI已评
              </van-tag>
              <van-tag
                v-else-if="item.matchMode === 'ai_suggested'"
                type="warning"
                size="mini"
                effect="plain"
              >
                AI建议·待复核
              </van-tag>
              <van-tag
                v-else-if="item.matchMode === 'pending_review'"
                type="warning"
                size="mini"
                effect="plain"
              >
                待评阅
              </van-tag>
              <van-tag
                v-else-if="item.matchMode === 'unanswered'"
                size="mini"
                effect="plain"
                color="var(--text-disabled)"
              >
                未作答
              </van-tag>
              <van-tag
                v-else
                type="danger"
                size="mini"
                effect="plain"
              >
                错误
              </van-tag>
              <span v-if="item.matchMode === 'fuzzy'" class="ph-match-badge">模糊匹配</span>
            </div>
            <div class="ph-item-body">
              <div class="ph-item-row-ans">
                <span class="ph-ans-label">你的答案：</span>
                <span
                  class="ph-ans-value"
                  :class="{
                    'ph-ans-strikethrough':
                      !item.isCorrect &&
                      item.matchMode !== 'pending_review' &&
                      item.matchMode !== 'unanswered',
                  }"
                  v-html="renderMarkdown(item.studentAnswer || '(空)')"
                ></span>
              </div>
              <div
                v-if="!item.isCorrect && item.correctAnswer"
                class="ph-item-row-ans ph-correct-row"
              >
                <span class="ph-ans-label">参考答案：</span>
                <span class="ph-ans-value ph-ans-correct" v-html="renderMarkdown(item.correctAnswer)"></span>
              </div>
              <div v-if="item.explanation" class="ph-item-explain">
                <span class="ph-explain-label">解析：</span><span v-html="renderMarkdown(item.explanation)" />
              </div>
            </div>
          </div>
        </van-collapse-item>
      </van-collapse>
    </div>
    <van-button
      plain
      hairline
      size="small"
      style="margin-top: 12px"
      @click="$emit('reset')"
    >
      重新诊断
    </van-button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { renderMarkdown } from '@/utils/markdown';
import { typeLabel } from '@/composables/useQuestionHelpers';

const props = defineProps({
  result: { type: Object, required: true },
});

defineEmits(['reset']);

const detailOpen = ref([]);

function itemRowClass(item) {
  return {
    'ph-item-correct': item.isCorrect,
    'ph-item-wrong':
      !item.isCorrect && item.matchMode !== 'pending_review' && item.matchMode !== 'unanswered',
    'ph-item-pending': item.matchMode === 'pending_review',
    'ph-item-empty': item.matchMode === 'unanswered',
  };
}
</script>

<style scoped>
.ph-diag-result {
  text-align: center;
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.ph-diag-label {
  font-size: var(--fs-sm);
  color: var(--text-regular);
}
.ph-diag-subject {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.ph-diag-change {
  font-size: var(--fs-sm);
  font-weight: 500;
  display: block;
  margin-top: 4px;
}
.ph-diag-first {
  color: var(--text-disabled);
}
.ph-diag-up {
  color: var(--el-color-success);
}
.ph-diag-down {
  color: var(--el-color-warning);
}
.ph-diag-same {
  color: var(--text-secondary);
}
.ph-diag-analysis {
  margin-top: 8px;
  text-align: center;
}
.ph-diag-level {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
}
.ph-diag-estimate {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  font-weight: 500;
}
.ph-diag-advice {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  line-height: 1.6;
  margin: 0;
  max-width: 280px;
}
.ph-diag-type-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 4px;
}
.ph-type-stat {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}
.ph-type-pending {
  color: var(--el-color-warning);
  background: var(--primary-light);
}
.ph-diag-autogroup-note {
  font-size: var(--fs-xs);
  color: var(--el-color-warning);
  background: var(--primary-light);
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  margin: 8px 0 0;
  display: flex;
  align-items: center;
  gap: 4px;
}
.ph-item-results {
  margin-top: 16px;
  width: 100%;
  text-align: left;
}
.ph-scoring-rule {
  margin-bottom: 10px;
}
.ph-detail-collapse {
  background: transparent;
}
.ph-detail-collapse :deep(.van-collapse-item__content) {
  background: transparent;
  padding: 8px 0;
}
.ph-item-row {
  padding: 12px 14px;
  margin-bottom: 8px;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--border-base);
  background: var(--bg-card);
}
.ph-item-row.ph-item-correct {
  border-left-color: var(--el-color-success);
  background: #f1f8f1;
}
.ph-item-row.ph-item-wrong {
  border-left-color: var(--el-color-danger);
  background: #fff5f5;
}
.ph-item-row.ph-item-pending {
  border-left-color: var(--el-color-warning);
  background: #fffdf5;
}
.ph-item-row.ph-item-empty {
  border-left-color: var(--text-disabled);
  background: var(--bg-hover);
}
.ph-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.ph-item-num {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--text-primary);
}
.ph-item-type-tag {
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}
.ph-item-body {
  font-size: var(--fs-sm);
  line-height: 1.6;
}
.ph-item-row-ans {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.ph-ans-label {
  color: var(--text-secondary);
  flex-shrink: 0;
}
.ph-ans-value {
  color: var(--text-primary);
  font-weight: 500;
  word-break: break-all;
}
.ph-ans-strikethrough {
  text-decoration: line-through;
  color: var(--el-color-danger);
}
.ph-correct-row {
  margin-top: 2px;
}
.ph-ans-correct {
  color: var(--el-color-success);
  font-weight: 600;
  text-decoration: none;
}
.ph-item-explain {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--border-base);
}
.ph-explain-label {
  font-weight: 600;
  color: var(--text-regular);
}
.ph-match-badge {
  font-size: 10px;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 1px 6px;
  border-radius: var(--radius-full);
}
</style>
