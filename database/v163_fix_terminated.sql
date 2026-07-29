-- ============================================================
-- v163 fix: 修正 flushPending 假计数导致的误终止提交
-- 根因: useCheatMonitor.flushPending() 每30秒调用 reportCheatWarning
--       API，后端无条件递增 cheat_warnings，导致学生在60秒内
--       被误判为作弊终止（3次警告 = 30s×2次flush + 1次真事件）
-- 影响: 5个任务，24个提交被误终止（17超时终止+7作弊终止）
-- 修复: TERMINATED→SUBMITTED，清除作弊标记，注明修正原因
-- 日期: 2026-06-20
-- ============================================================

UPDATE task_submissions
SET
    status = 'SUBMITTED',
    cheat_terminated = 0,
    cheat_warnings = 0,
    score = NULL,
    grading_message = CONCAT('[系统bug误终止已自动修正] ', IFNULL(grading_message, '')),
    submitted_at = CASE WHEN submitted_at IS NULL THEN NOW() ELSE submitted_at END
WHERE status = 'TERMINATED'
  AND task_id IN (164, 165, 166, 167, 168);
