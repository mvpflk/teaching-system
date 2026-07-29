# Flyway Migration Mapping

| Flyway | Filename | Original | Category |
|--------|----------|----------|----------|
| V000 | V000__baseline_schema.sql | init.sql | BASELINE |
| V001 | V001__classroom_question_fields.sql | v13_classroom_question_fields.sql | DDL_ONLY |
| V002 | V002__a_classroom_question_sync.sql | v13a_classroom_question_sync.sql | DDL_ONLY |
| V003 | V003__reflection.sql | v15_reflection.sql | DDL_ONLY |
| V004 | V004__absent_students.sql | v16_absent_students.sql | DDL_ONLY |
| V005 | V005__credit_biz_key.sql | v18_credit_biz_key.sql | DDL_ONLY |
| V006 | V006__classroom_ai_fields.sql | v19_classroom_ai_fields.sql | DDL_ONLY |
| V007 | V007__exam_papers.sql | v82_exam_papers.sql | DDL_ONLY |
| V008 | V008__fix_credit_bbs.sql | v82_fix_credit_bbs.sql | DDL_ONLY |
| V009 | V009__english_credit_rules.sql | v84_english_credit_rules.sql | DDL_ONLY |
| V010 | V010__english_reading_passages.sql | v84_english_reading_passages.sql | MIXED |
| V011 | V011__knowledge_nodes_grammar_extension.sql | v84_knowledge_nodes_grammar_extension.sql | MIXED |
| V012 | V012__optimizations.sql | v84_optimizations.sql | DDL_ONLY |
| V013 | V013__classroom_question_task_id.sql | v88_classroom_question_task_id.sql | DDL_ONLY |
| V014 | V014__syllabus_aging_fields.sql | v89_syllabus_aging_fields.sql | DDL_ONLY |
| V015 | V015__precision_migration.sql | v92_precision_migration.sql | MIXED |
| V016 | V016__precision_fixes.sql | v99_precision_fixes.sql | DDL_ONLY |
| V017 | V017__classroom_security_hardening.sql | v100_classroom_security_hardening.sql | DDL_ONLY |
| V018 | V018__simulation.sql | v100_simulation.sql | DDL_ONLY |
| V019 | V019__practice_wizard.sql | v123_practice_wizard.sql | DDL_ONLY |
| V020 | V020__async_task.sql | v124_async_task.sql | DDL_ONLY |
| V021 | V021__edit_history.sql | v125_edit_history.sql | DDL_ONLY |
| V022 | V022__checkpoint_system.sql | v126_checkpoint_system.sql | MIXED |
| V023 | V023__syllabus_structured_meta.sql | v145_syllabus_structured_meta.sql | DDL_ONLY |
| V024 | V024__user_events.sql | v146_user_events.sql | DDL_ONLY |
| V025 | V025__fix_ai_outputs_fk.sql | v147_fix_ai_outputs_fk.sql | DDL_ONLY |
| V026 | V026__knowledge_learning_resources.sql | v150_knowledge_learning_resources.sql | MIXED |
| V027 | V027__english_vocab_drill.sql | v151_english_vocab_drill.sql | MIXED |
| V028 | V028__resource_status.sql | v151_resource_status.sql | DDL_ONLY |
| V029 | V029__ai_call_log_status.sql | v152_ai_call_log_status.sql | DDL_ONLY |
| V030 | V030__knowledge_base.sql | v153_knowledge_base.sql | MIXED |
| V031 | V031__knowledge_quiz_results.sql | v154_knowledge_quiz_results.sql | DDL_ONLY |
| V032 | V032__cheat_event_log.sql | v155_cheat_event_log.sql | DDL_ONLY |
| V033 | V033__task_submission_grading_message.sql | v156_task_submission_grading_message.sql | DDL_ONLY |
| V034 | V034__typing_competition_duration.sql | v158_typing_competition_duration.sql | DDL_ONLY |
| V035 | V035__add_research_group.sql | v160_add_research_group.sql | DDL_ONLY |
| V036 | V036__add_teacher_activity_log.sql | v161_add_teacher_activity_log.sql | DDL_ONLY |
| V037 | V037__research_paper_standardization.sql | v162_research_paper_standardization.sql | DDL_ONLY |
| V038 | V038__answer_sheet_ocr.sql | v163_answer_sheet_ocr.sql | DDL_ONLY |
| V039 | V039__teacher_research.sql | v163_teacher_research.sql | DDL_ONLY |
| V040 | V040__flashcard_enhancements.sql | v167_flashcard_enhancements.sql | DDL_ONLY |
| V041 | V041__checkpoint_major_isolation.sql | v168_checkpoint_major_isolation.sql | DDL_ONLY |
| V042 | V042__card_profile_group.sql | v169_card_profile_group.sql | DDL_ONLY |
| V043 | V043__task_retake.sql | v200_task_retake.sql | DDL_ONLY |

Total migrations: 44

---

## ⚠️ Baseline 约定（2026-07-13 校准）

- **两个环境的 Flyway baseline-version 统一为 45**（local `application.yml` + prod `application-prod.yml`）。
- 生产 `flyway_schema_history` 只有一条 `BASELINE (version=45)` 记录，**V000~V045 全部视为"已应用"，不会被 Flyway 自动执行**。
- **下一个新迁移必须从 `V046__` 开始**。编号 ≤45 的迁移在生产会被静默跳过（这是历史踩坑：V044 因 prod baseline=45 从未执行）。
- **V044__ai_feature_flags.sql 特例**：它想做的两个 feature flag（`feature.sse_enabled` / `feature.ai_content_enabled`）已在 `init.sql` 种子数据中为 `true`，生产库现值也是 `true`，故其未执行无功能影响。**不要重命名或删除该文件**——本地已应用它的库会触发 Flyway 校验错误。
