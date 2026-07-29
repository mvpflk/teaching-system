import { ref, computed, onBeforeUnmount } from 'vue';
import { createEventSource } from '@/utils/sseTicket';
import { submitAiOutput, getAiOutputResult } from '@/api/aiOutput';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export function useAiGeneration({
  contentType,
  designStyle,
  designFocus,
  questionCounts,
  questionTier,
  questionDim,
  scorePresets,
  isQuestionType,
  cascade,
  getSubjectName,
  deriveStageHint,
  sourceOutputId,
  onSuccess,
}) {
  const generating = ref(false);
  const genResult = ref(null);
  const genError = ref('');
  const genId = ref(null);
  const progressMessage = ref('');
  let sseAbortController = null;
  let progressTimer = null;
  let progressStartTime = 0;

  const typeLabel = computed(() => {
    const map = {
      TEACHING_DESIGN: '教学设计',
      KNOWLEDGE_CHECKLIST: '知识清单',
      PRACTICE_PLAN: '实训方案',
      COMPREHENSIVE_EXERCISES: '综合练习',
      CLASSROOM_QUESTIONS: '课堂提问',
      KNOWLEDGE_PRACTICE: '配套练习',
      EXAM_PAPER: '试卷',
      DIAGNOSIS: '诊断报告',
      CONSOLIDATION_MATERIAL: '巩固材料',
    };
    return map[contentType.value] || '内容';
  });

  const startProgressAnimation = () => {
    progressStartTime = Date.now();
    progressMessage.value = '正在提交生成请求...';
    const label = typeLabel.value;
    progressTimer = setInterval(() => {
      const elapsed = Math.floor((Date.now() - progressStartTime) / 1000);
      if (elapsed < 5) progressMessage.value = '正在提交生成请求...';
      else if (elapsed < 15) progressMessage.value = `正在读取知识库内容，准备生成${label}...`;
      else if (elapsed < 45) progressMessage.value = `AI 正在生成${label}（约需 30-60 秒）...`;
      else if (elapsed < 75) progressMessage.value = `${label}即将完成，正在整理结果...`;
      else progressMessage.value = `生成${label}时间较长，请耐心等待...`;
    }, 2000);
  };

  const stopProgressAnimation = () => {
    if (progressTimer) {
      clearInterval(progressTimer);
      progressTimer = null;
    }
    progressMessage.value = '生成完成';
  };

  const waitForTaskViaSSE = (taskId, signal) =>
    new Promise((resolve, reject) => {
      createEventSource(`/api/ai-output/stream/${taskId}`, 'ai-generation')
        .then((es) => {
          let settled = false;
          const doResolve = (v) => {
            if (!settled) {
              settled = true;
              resolve(v);
            }
          };
          const doReject = (e) => {
            if (!settled) {
              settled = true;
              reject(e);
            }
          };
          const cleanup = () => {
            clearTimeout(timeout);
            clearTimeout(fallbackTimer);
            es.close();
          };
          const timeout = setTimeout(() => {
            cleanup();
            doReject(new Error('AI处理超时，建议缩短知识库内容或稍后重试'));
          }, 1_200_000); // 20分钟超时，支持55-100题分批生成

          let received = false;
          es.addEventListener('task-result', (e) => {
            received = true;
            if (settled) return;
            try {
              const data = JSON.parse(e.data);
              if (data.status === 'COMPLETED') {
                clearTimeout(timeout);
                es.close();
                doResolve(data.result);
              } else if (data.status === 'FAILED') {
                clearTimeout(fallbackTimer);
                cleanup();
                doReject(new Error(data.error || 'AI处理失败'));
              } else if (data.status === 'TIMEOUT') {
                clearTimeout(fallbackTimer);
                cleanup();
                doReject(new Error('AI处理超时'));
              }
            } catch (err) {
              cleanup();
              doReject(err);
            }
          });

          const fallbackTimer = setTimeout(() => {
            if (!received) {
              es.close();
              console.warn('SSE 5秒无响应，降级为轮询模式');
              doReject(new Error('SSE_DEGRADE'));
            }
          }, 5000);

          es.onerror = () => {
            cleanup();
            doReject(new Error('SSE连接中断'));
          };

          if (signal) {
            signal.addEventListener('abort', () => {
              cleanup();
              doReject(new Error('SSE_ABORTED'));
            });
          }
        })
        .catch(reject);
    });

  const waitForTaskViaPoll = async (taskId) => {
    const start = Date.now();
    let pollInterval = 3000;
    while (Date.now() - start < 1_200_000) {
      // 20分钟轮询超时，支持55-100题
      await sleep(pollInterval);
      pollInterval = Math.min(pollInterval * 1.5, 8000);
      const pollRes = await getAiOutputResult(taskId);
      if (pollRes.code !== 200) continue;
      const { status, result, error } = pollRes.data;
      if (status === 'COMPLETED') {
        genResult.value = result;
        genId.value = result?.id;
        onSuccess?.();
        return result;
      }
      if (status === 'FAILED')
        throw new Error(error || 'AI处理失败，可尝试减少知识点内容或更换生成类型后重试');
      if (status === 'TIMEOUT') throw new Error('AI处理超时，建议缩短知识库内容或稍后重试');
    }
    throw new Error('AI 生成超时，请尝试减少选中的知识点数量或缩短知识库内容后重试');
  };

  const generateSingle = async (categoryId) => {
    const params = {
      contentType: contentType.value,
      categoryId,
      subject: getSubjectName(),
      stageHint: deriveStageHint(),
    };
    if (cascade.categoryIds?.length > 0) params.categoryIds = cascade.categoryIds;
    if (cascade.subKpList?.length > 0) params.subKpList = cascade.subKpList;
    if (contentType.value === 'TEACHING_DESIGN') {
      params.style = designStyle.value;
      params.focus = designFocus.value;
    }
    if (sourceOutputId?.value) {
      params.sourceOutputId = sourceOutputId.value;
    }
    if (isQuestionType.value) {
      params.typeCounts = { ...questionCounts };
      params.tierFocus = questionTier.value;
      params.knowledgeDim = questionDim.value;
      params.scorePresets = { ...scorePresets };
    }
    const submitRes = await submitAiOutput(params);
    if (submitRes.code !== 200) throw new Error(submitRes.message || '提交失败');
    const taskId = submitRes.data?.taskId;
    if (!taskId) throw new Error('未获取到任务ID');

    sseAbortController = new AbortController();
    let sseOk = false;
    try {
      const sseResult = await waitForTaskViaSSE(taskId, sseAbortController.signal);
      genResult.value = sseResult;
      genId.value = sseResult?.id;
      onSuccess?.();
      sseOk = true;
    } catch (err) {
      // 所有 SSE 错误（超时/中断/解析失败/5秒静默）均降级轮询，不吞错误信息
      // 仅用户主动中止不降级（页面卸载场景无需轮询）
      if (err.message === 'SSE_ABORTED') throw err;
      console.warn('SSE 不可用，降级为轮询模式:', err.message);
    } finally {
      sseAbortController = null;
    }
    if (!sseOk && !genResult.value) {
      await waitForTaskViaPoll(taskId);
    }
  };

  const doGenerate = async (canGenerate) => {
    if (!canGenerate) return;
    generating.value = true;
    genResult.value = null;
    genError.value = '';
    genId.value = null;
    startProgressAnimation();
    try {
      await generateSingle(cascade.kpId || cascade.taskId || cascade.chapterId);
    } catch (e) {
      stopProgressAnimation();
      genError.value = e.message || '生成失败';
    } finally {
      stopProgressAnimation();
      generating.value = false;
    }
  };

  onBeforeUnmount(() => {
    if (sseAbortController) {
      sseAbortController.abort();
      sseAbortController = null;
    }
    stopProgressAnimation();
  });

  return {
    generating,
    genResult,
    genError,
    genId,
    progressMessage,
    doGenerate,
    generateSingle,
  };
}
