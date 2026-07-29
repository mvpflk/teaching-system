<template>
  <div class="wrong-book">
    <el-page-header @back="goBack">
      <template #content>
        <span>错题本</span>
        <span v-if="!loading" class="wb-header-stats">共 {{ total }} 道错题</span>
      </template>
    </el-page-header>

    <div class="derived-banner" @click="router.push('/student/derived-practice')">
      <div class="banner-left">
        <el-icon class="banner-icon"><Opportunity /></el-icon>
        <div class="banner-text">
          <div class="banner-title">衍生练习</div>
          <div class="banner-desc">
            <template v-if="stats.unmastered >= 5 && stats.weekPractice === 0">
              你有 {{ stats.unmastered }} 道错题待巩固，点击开始针对性训练
            </template>
            <template v-else-if="stats.weekPractice > 0">
              今日已完成 {{ stats.weekPractice }} 次练习{{
                stats.streak > 1 ? `，连续 ${stats.streak} 天` : ''
              }}
            </template>
            <template v-else>AI 分析薄弱知识点，自动生成针对性练习题</template>
          </div>
        </div>
      </div>
      <el-button type="primary" size="small">
        {{
          stats.unmastered >= 5 && stats.weekPractice === 0 ? '立即练习 →' : '开始练习 →'
        }}
      </el-button>
    </div>

    <WrongBookStats :stats="stats" />

    <el-tabs v-model="activeTab" class="wb-tabs">
      <el-tab-pane label="📋 错题列表" name="list">
        <div class="wb-filter-bar">
          <el-radio-group
            v-model="statusFilter"
            size="small"
            @change="
              page = 1;
              loadList();
            "
          >
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="unmastered">未掌握</el-radio-button>
            <el-radio-button value="mastered">已掌握</el-radio-button>
          </el-radio-group>
          <el-select
            v-model="sourceTypeFilter"
            size="small"
            placeholder="来源筛选"
            clearable
            style="width: 130px"
            @change="
              page = 1;
              loadList();
            "
          >
            <el-option label="全部来源" value="" />
            <el-option
              v-for="(label, key) in SOURCE_TYPE_LABEL"
              :key="key"
              :value="key"
              :label="label"
            />
          </el-select>
          <el-dropdown trigger="click" @command="handleRedoCommand">
            <el-button
              type="primary"
              size="small"
              :disabled="!list.length"
            >
              错题练习<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-item command="redo">
                <el-icon><Edit /></el-icon> 错题重做（原题）
              </el-dropdown-item>
              <el-dropdown-item
                command="derived"
                :disabled="!list.filter((r) => !r.isMastered).length"
              >
                <el-icon><MagicStick /></el-icon> 衍生练习（AI相似题）
              </el-dropdown-item>
            </template>
          </el-dropdown>
          <el-button
            type="danger"
            size="small"
            :disabled="!list.filter((r) => r.isMastered).length"
            @click="batchDelete"
          >
            删除已掌握
          </el-button>
          <el-button
            size="small"
            :type="groupBySubject ? 'primary' : ''"
            @click="groupBySubject = !groupBySubject"
          >
            {{ groupBySubject ? '按学科' : '按时间' }}
          </el-button>
        </div>

        <template v-if="groupBySubject && !loading">
          <div v-for="g in subjectGroups" :key="g.subject" class="wb-group">
            <div class="wb-group-header">
              <span class="wb-group-title">{{ g.subject || '未分类' }}</span>
              <span class="wb-group-count">{{ g.total }} 道错题 · {{ g.mastered }} 已掌握</span>
              <el-button
                size="small"
                type="warning"
                @click="generateDerivedForSubject(g.subject)"
              >
                生成该学科衍生练习
              </el-button>
            </div>
            <div class="wb-group-list">
              <div
                v-for="row in g.items"
                :key="row.id"
                class="wb-group-item"
                @click="showDetail(row)"
              >
                <span class="wb-gi-text">{{ row.questionText?.substring(0, 60)
                }}{{ row.questionText?.length > 60 ? '...' : '' }}</span>
                <el-tag :type="row.isMastered ? 'success' : 'danger'" size="small">
                  {{
                    row.isMastered ? '已掌握' : '未掌握'
                  }}
                </el-tag>
                <span class="wb-gi-count">错 {{ row.wrongCount || 1 }} 次</span>
              </div>
            </div>
          </div>
          <EmptyState v-if="!list.length" title="暂无错题记录" :icon="Document" />
        </template>

        <div v-if="loading && !list.length" class="wb-sk-wrap">
          <div v-for="n in 4" :key="n" class="wb-sk-card">
            <div class="wb-sk-row"><div class="wb-sk-line w-60"></div></div>
            <div class="wb-sk-row"><div class="wb-sk-line w-40"></div></div>
          </div>
        </div>
        <EmptyState
          v-else-if="!loading && list.length === 0"
          :title="statusFilter === 'mastered' ? '全部已掌握' : '暂无错题记录'"
          :icon="Document"
        />

        <template v-if="isMobile">
          <div v-loading="loading" class="wb-card-list">
            <div
              v-for="row in list"
              :key="row.id"
              class="wb-card"
              @click="showDetail(row)"
            >
              <div class="wb-card-header">
                <el-tag :type="row.isMastered ? 'success' : 'danger'" size="small">
                  {{
                    row.isMastered ? '已掌握' : '未掌握'
                  }}
                </el-tag>
                <span class="wb-card-type">{{
                  QUESTION_TYPE_LABEL[row.questionType] || row.questionType
                }}</span>
                <span class="wb-card-subject">{{ row.subject }}</span>
              </div>
              <div class="wb-card-text">
                {{ row.questionText?.substring(0, 80)
                }}{{ row.questionText?.length > 80 ? '...' : '' }}
              </div>
              <div class="wb-card-footer">
                <span class="wrong-count">错 {{ row.wrongCount || 1 }} 次</span>
                <span class="wb-card-action" @click.stop="toggleMastered(row)">{{
                  row.isMastered ? '标记未掌握' : '标记已掌握'
                }}</span>
                <span
                  class="wb-card-action"
                  style="color: var(--el-color-danger); margin-left: 8px"
                  @click.stop="handleDelete(row)"
                >删除</span>
              </div>
              <div v-if="row.sourceType" class="wb-card-source">
                <el-tag size="small" type="info" @click.stop="goToSource(row)">
                  {{
                    SOURCE_TYPE_LABEL[row.sourceType] || row.sourceType
                  }}
                </el-tag>
              </div>
            </div>
          </div>
        </template>

        <el-table
          v-else
          v-loading="loading"
          :data="list"
          stripe
        >
          <el-table-column
            prop="questionText"
            label="题目"
            min-width="280"
            show-overflow-tooltip
          />
          <el-table-column prop="subject" label="学科" width="100" />
          <el-table-column
            label="题型"
            width="90"
          >
            <template #default="{ row }">
              {{
                QUESTION_TYPE_LABEL[row.questionType] || row.questionType
              }}
            </template>
          </el-table-column>
          <el-table-column
            label="错误次数"
            width="80"
          >
            <template #default="{ row }">
              <span class="wrong-count">{{ row.wrongCount || 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="状态"
            width="100"
          >
            <template #default="{ row }">
              <el-tag :type="row.isMastered ? 'success' : 'danger'" size="small">
                {{
                  row.isMastered ? '已掌握' : '未掌握'
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="来源"
            width="100"
          >
            <template #default="{ row }">
              <el-tag
                v-if="row.sourceType"
                size="small"
                type="info"
                style="cursor: pointer"
                @click="goToSource(row)"
              >
                {{ SOURCE_TYPE_LABEL[row.sourceType] || row.sourceType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="wb-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click="showDetail(row)"
                >
                  <el-icon><View /></el-icon> 详情
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  @click="toggleMastered(row)"
                >
                  <el-icon><Switch /></el-icon>
                  {{ row.isMastered ? '未掌握' : '已掌握' }}
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="handleDelete(row)"
                >
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="page"
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          @current-change="loadList"
        />
      </el-tab-pane>

      <el-tab-pane label="📊 薄弱分析" name="weakness">
        <div v-if="weakLoading" style="text-align: center; padding: 40px">
          <el-icon class="is-loading"><Loading /></el-icon> 分析中...
        </div>
        <template v-else-if="weakList.length">
          <div ref="weakChartRef" style="width: 100%; height: 300px; margin-bottom: 16px"></div>
          <el-table :data="weakList" stripe>
            <el-table-column type="selection" width="50" />
            <el-table-column prop="knowledgeNodeName" label="薄弱知识点" min-width="160" />
            <el-table-column
              prop="errorCount"
              label="错误次数"
              width="100"
              sortable
            >
              <template #default="{ row }">
                <el-tag type="danger">{{ row.errorCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="lastErrorTime"
              label="最近错误时间"
              width="180"
            >
              <template #default="{ row }">
                {{
                  row.lastErrorTime ? row.lastErrorTime.substring(0, 10) : '-'
                }}
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="180"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click="generateRemedialForNode(row)"
                >
                  AI针对性练习
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <EmptyState
          v-else
          title="暂无薄弱知识点数据"
          description="多做一些练习后回来查看"
          :icon="TrendCharts"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="detailVisible"
      title="题目详情"
      :width="isMobile ? '95%' : '640px'"
      append-to-body
    >
      <div class="detail-section">
        <div class="detail-label">题目</div>
        <div class="detail-text">{{ detailRow.questionText }}</div>
      </div>
      <div v-if="parsedDetailOptions.length" class="detail-section">
        <div class="detail-label">选项</div>
        <div
          v-for="(opt, oi) in parsedDetailOptions"
          :key="oi"
          class="detail-option"
          :class="{ 'opt-correct': isCorrectOption(oi), 'opt-wrong': isWrongOption(oi) }"
        >
          <span class="opt-letter">{{ String.fromCharCode(65 + oi) }}.</span>
          <span class="opt-text">{{ opt }}</span>
          <el-tag
            v-if="isCorrectOption(oi)"
            size="small"
            type="success"
            class="opt-tag"
          >
            正确答案
          </el-tag>
          <el-tag
            v-if="isWrongOption(oi)"
            size="small"
            type="danger"
            class="opt-tag"
          >
            你的选择
          </el-tag>
        </div>
      </div>
      <div class="detail-section">
        <div class="detail-label">你的答案</div>
        <div
          class="detail-text"
          :class="{ 'answer-correct': detailRow.myAnswer === detailRow.correctAnswer }"
        >
          {{ detailRow.myAnswer || '未作答' }}
        </div>
      </div>
      <div class="detail-section">
        <div class="detail-label">正确答案</div>
        <div class="detail-text correct">{{ detailRow.correctAnswer || '-' }}</div>
      </div>
      <div v-if="detailRow.explanation" class="detail-section">
        <div class="detail-label">解析</div>
        <div class="detail-text">{{ detailRow.explanation }}</div>
      </div>
      <div v-if="detailRow.questionType" class="detail-section">
        <div class="detail-label">题型</div>
        <div class="detail-text">
          {{ QUESTION_TYPE_LABEL[detailRow.questionType] || detailRow.questionType }}
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="remedialDialogVisible"
      :title="'AI针对性练习 - ' + remedialNodeName"
      :width="isMobile ? '95%' : '700px'"
      destroy-on-close
      append-to-body
    >
      <div v-if="remedialLoading" style="text-align: center; padding: 40px">
        <el-icon class="is-loading" style="font-size: 32px"><Loading /></el-icon>
        <p style="margin-top: 12px">AI正在生成针对性练习题...</p>
      </div>
      <template v-else-if="remedialQuestions.length">
        <div v-for="(q, qi) in remedialQuestions" :key="q.id || qi" class="remedial-q">
          <div class="remedial-q-header">
            <el-tag size="small" :type="qTypeTag(q.questionType)">
              {{
                QUESTION_TYPE_LABEL[q.questionType] || q.questionType
              }}
            </el-tag><span class="remedial-q-num">第 {{ qi + 1 }} 题</span>
          </div>
          <div class="remedial-q-text">{{ q.questionText }}</div>
          <div v-if="q.options && parsedRemedialOpts(q.options).length" class="remedial-q-opts">
            <div v-for="(opt, oi) in parsedRemedialOpts(q.options)" :key="oi" class="remedial-opt">
              <span class="opt-badge">{{ String.fromCharCode(65 + oi) }}</span><span>{{ opt }}</span>
            </div>
          </div>
        </div>
        <div style="text-align: center; margin-top: 16px">
          <el-button type="primary" @click="startRemedialPractice">开始练习</el-button>
          <el-button @click="remedialDialogVisible = false">关闭</el-button>
        </div>
      </template>
      <EmptyState v-else title="暂无生成的题目" :icon="Document" />
    </el-dialog>

    <el-dialog
      v-model="redoVisible"
      title="错题重做"
      :width="isMobile ? '100%' : '600px'"
      :fullscreen="isMobile"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body
      @close="clearRedoState"
      @opened="onRedoOpened"
    >
      <div v-if="redoQuestion" class="redo-section">
        <div class="redo-header">第 {{ redoIndex + 1 }} / {{ redoList.length }} 题</div>
        <div v-if="isRedoMulti" class="q-options">
          <div
            v-for="(opt, oi) in parsedRedoOptions"
            :key="oi"
            class="q-opt"
            :class="{ selected: redoMultiSel[oi] }"
            @click="redoMultiSel[oi] = !redoMultiSel[oi]"
          >
            <span class="opt-letter">{{ String.fromCharCode(65 + oi) }}</span><span class="opt-text">{{ opt }}</span>
          </div>
          <div
            v-if="parsedRedoOptions.length"
            style="margin-top: 4px; font-size: var(--fs-xs); color: var(--text-secondary)"
          >
            可多选
          </div>
        </div>
        <div v-else-if="parsedRedoOptions.length" class="q-options">
          <div
            v-for="(opt, oi) in parsedRedoOptions"
            :key="oi"
            class="q-opt"
            :class="{ selected: redoAnswer === String.fromCharCode(65 + oi) }"
            @click="redoAnswer = String.fromCharCode(65 + oi)"
          >
            <span class="opt-letter">{{ String.fromCharCode(65 + oi) }}</span><span class="opt-text">{{ opt }}</span>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">题目</div>
          <div class="detail-text">{{ redoQuestion.questionText }}</div>
        </div>
        <div v-if="!parsedRedoOptions.length" class="detail-section">
          <div class="detail-label">你的答案</div>
          <el-input
            v-model="redoAnswer"
            type="textarea"
            :rows="3"
            placeholder="输入你的答案..."
          />
        </div>
        <div v-if="redoRevealed" class="detail-section">
          <div class="detail-label">正确答案</div>
          <div class="detail-text correct">{{ redoQuestion.correctAnswer || '-' }}</div>
        </div>
        <div v-if="redoRevealed && redoQuestion.explanation" class="detail-section">
          <div class="detail-label">解析</div>
          <div class="detail-text">{{ redoQuestion.explanation }}</div>
        </div>
      </div>
      <div v-else class="redo-empty">
        <EmptyState v-if="redoLoaded" title="加载失败" :icon="WarningFilled" />
        <p v-else>题目加载中...</p>
      </div>
      <div v-if="isMobile" class="wb-review-bar" :style="{ paddingBottom: 'var(--safe-bottom)' }">
        <el-button size="large" style="flex: 1" @click="markMastered">✓ 已掌握</el-button>
        <el-button
          size="large"
          style="flex: 1"
          type="primary"
          @click="retryQuestion"
        >
          ↻ 再练一次
        </el-button>
      </div>
      <template #footer>
        <div style="display: flex; justify-content: space-between; width: 100%">
          <div>
            <el-button v-if="redoIndex > 0" @click="redoPrev">上一题</el-button><el-button v-if="redoIndex < redoList.length - 1" @click="redoNext">下一题</el-button>
          </div>
          <div>
            <el-button
              v-if="!redoChecked"
              type="primary"
              :disabled="isRedoMulti ? !redoMultiSel.some(Boolean) : !redoAnswer.trim()"
              @click="checkRedoAnswer"
            >
              检查答案
            </el-button>
            <template v-else-if="redoPassed">
              <el-tag type="success" size="medium">✓ 回答正确</el-tag>
            </template>
            <template v-else>
              <el-button
                v-if="redoIndex < redoList.length - 1"
                type="primary"
                @click="redoNext"
              >
                下一题
              </el-button><el-button v-else @click="redoVisible = false">完成</el-button>
            </template>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>

</template>

<script setup>
import { useRouter } from 'vue-router';
import {
  Opportunity,
  Loading,
  MagicStick,
  View,
  Switch,
  Delete,
  ArrowDown,
  Edit,
  Document,
  TrendCharts,
  WarningFilled,
} from '@element-plus/icons-vue';
import { useWrongBook } from '@/composables/useWrongBook';
import WrongBookStats from './components/WrongBookStats.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';

const router = useRouter();

const {
  isMobile,
  list,
  loading,
  page,
  total,
  pageSize,
  statusFilter,
  sourceTypeFilter,
  groupBySubject,
  activeTab,
  stats,
  detailVisible,
  detailRow,
  weakList,
  weakLoading,
  weakChartRef,
  remedialDialogVisible,
  remedialLoading,
  remedialQuestions,
  remedialNodeName,
  redoVisible,
  redoList,
  redoIndex,
  redoAnswer,
  redoRevealed,
  redoChecked,
  redoPassed,
  redoLoaded,
  redoMultiSel,
  redoQuestion,
  isRedoMulti,
  parsedRedoOptions,
  parsedDetailOptions,
  subjectGroups,
  isCorrectOption,
  isWrongOption,
  SOURCE_TYPE_LABEL,
  goBack,
  goToSource,
  loadList,
  showDetail,
  toggleMastered,
  handleDelete,
  batchDelete,
  handleRedoCommand,
  startRedo,
  redoNext,
  redoPrev,
  checkRedoAnswer,
  clearRedoState,
  markMastered,
  retryQuestion,
  onRedoOpened,
  generateRemedialForNode,
  startRemedialPractice,
  qTypeTag,
  parsedRemedialOpts,
  generateDerivedForSubject,
} = useWrongBook();
</script>

<style scoped>
.wrong-book {
  margin: 0 auto;
}
.wb-header-stats {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-left: 10px;
  font-weight: 400;
}
.derived-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  margin: 12px 0;
  background: var(--primary-light);
  border: 1px solid var(--primary-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: box-shadow var(--transition-fast);
}
.derived-banner:hover {
  box-shadow: var(--shadow-base);
}
.banner-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.banner-icon {
  font-size: 28px;
}
.banner-title {
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--primary-color);
}
.banner-desc {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 2px;
}
.wb-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: var(--spacing-md) 0;
  flex-wrap: wrap;
}
.wb-sk-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px 0;
}
.wb-sk-card {
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.wb-sk-row {
  display: flex;
  gap: 12px;
}
.wb-sk-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--skeleton-bg) 25%,
    var(--skeleton-highlight) 50%,
    var(--skeleton-bg) 75%
  );
  background-size: 200% 100%;
  animation: sk-shimmer 1.5s infinite;
}
.w-40 {
  width: 40%;
}
.w-60 {
  width: 60%;
}
@keyframes sk-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
@media (max-width: 768px) {
  .derived-banner {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
  .banner-left {
    flex-direction: column;
  }
}
.wrong-count {
  font-weight: 600;
  color: var(--el-color-danger);
}
.detail-section {
  margin-bottom: 16px;
}
.detail-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-bottom: 4px;
  font-weight: 500;
}
.detail-text {
  font-size: var(--fs-base);
  color: var(--text-primary);
  padding: 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  white-space: pre-wrap;
  word-break: break-word;
}
.detail-text.correct {
  background: var(--bg-success-light);
  color: var(--el-color-success);
  font-weight: 600;
}
.answer-correct {
  color: var(--el-color-success) !important;
}
.detail-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 4px;
  background: var(--bg-section);
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
}
.detail-option.opt-correct {
  background: var(--bg-success-light);
  border: 1px solid var(--el-color-success);
}
.detail-option.opt-wrong {
  background: var(--bg-danger-light);
  border: 1px solid var(--el-color-danger);
}
.opt-letter {
  font-weight: 600;
  color: var(--text-secondary);
  min-width: 20px;
}
.opt-text {
  flex: 1;
  color: var(--text-primary);
}
.opt-tag {
  flex-shrink: 0;
}
.wb-card-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}
.wb-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px;
  cursor: pointer;
}
.wb-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.wb-card-type {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.wb-card-subject {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.wb-card-text {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 8px;
}
.wb-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.wb-card-action {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  cursor: pointer;
}
.wb-card-source {
  margin-top: 6px;
}
.wb-group {
  margin-bottom: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.wb-group-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: var(--bg-secondary, #f5f7fa);
  font-size: var(--fs-md);
  flex-wrap: wrap;
}
.wb-group-title {
  font-weight: 600;
  color: var(--text-primary);
}
.wb-group-count {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  flex: 1;
}
.wb-group-list {
  display: flex;
  flex-direction: column;
}
.wb-group-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-light);
  font-size: var(--fs-sm);
}
.wb-group-item:last-child {
  border-bottom: none;
}
.wb-group-item:hover {
  background: var(--bg-secondary, #f0f2f5);
}
.wb-gi-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}
.wb-gi-count {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  min-width: 50px;
  text-align: right;
}
.q-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.q-opt {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.q-opt:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.q-opt.selected {
  border-color: var(--primary-color);
  background: var(--primary-light);
  font-weight: 600;
}
.redo-empty {
  text-align: center;
  padding: 20px;
  color: var(--text-secondary);
}
.remedial-q {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-section);
  border-radius: var(--radius-sm);
}
.remedial-q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.remedial-q-num {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.remedial-q-text {
  font-size: var(--fs-md);
  color: var(--text-primary);
  margin-bottom: 8px;
}
.remedial-q-opts {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.remedial-opt {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.opt-badge {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--primary-color);
  flex-shrink: 0;
}
.wb-tabs {
  margin-top: 12px;
}
.wb-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}
.wb-actions .el-button {
  padding: 4px 8px;
  font-size: var(--fs-xs);
}
@media (max-width: 768px) {
  .wb-review-bar {
    position: fixed;
    bottom: 56px;
    left: 0;
    right: 0;
    display: flex;
    gap: 10px;
    padding: 12px 16px;
    background: var(--bg-card);
    border-top: 0.5px solid var(--border-color);
    z-index: 100;
  }
}
</style>
