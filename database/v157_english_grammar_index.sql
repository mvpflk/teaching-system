-- ============================================================================
-- v157: English[职高] grammar quick-reference index article
-- Idempotent: INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;

INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '语法速查', '语法规则速查表',
'## 时态语态

| 时态 | 构成 | 标志词 | 例句 |
|------|------|--------|------|
| 一般现在时 | 动词原形/三单-s | every day, always, often | She goes to school by bus. |
| 一般过去时 | 动词过去式 | yesterday, last week, ago | I visited my grandma last Sunday. |
| 一般将来时 | will + do / be going to | tomorrow, next week, soon | We will have a test tomorrow. |
| 现在进行时 | am/is/are + doing | now, at the moment, Listen! | The boys are playing football now. |
| 过去进行时 | was/were + doing | at that time, when + 过去时 | I was reading when he came. |
| 现在完成时 | have/has + done | already, yet, ever, since, for | I have lived here for 5 years. |
| 过去完成时 | had + done | by the time, before + 过去时 | She had finished before I arrived. |
| 被动语态 | be + 过去分词 | 动作承受者作主语 | The bridge was built in 2020. |

## 非谓语动词

| 形式 | 功能 | 常用搭配 |
|------|------|----------|
| to do | 目的/将来 | want/hope/decide/plan + to do |
| V-ing | 主动/进行/习惯 | enjoy/finish/mind/avoid + V-ing |
| done | 被动/完成 | 过去分词作定语/表语/宾补 |

## 定语从句

| 关系词 | 先行词 | 用法 |
|--------|--------|------|
| who/that | 人 | 作主语/宾语 |
| which/that | 物 | 作主语/宾语 |
| whose | 人/物 | 作定语（…的） |
| when/where/why | 时间/地点/原因 | 作状语 |

## 名词性从句

| 类型 | 引导词 | 例句 |
|------|--------|------|
| 宾语从句 | that/whether/if/wh- | I think (that) he is right. |
| 主语从句 | That/Whether/Wh- | It is important that we study hard. |
| 表语从句 | that/whether/wh- | The problem is that we have no time. |

## 状语从句

| 类型 | 引导词 |
|------|--------|
| 时间 | when, while, before, after, until |
| 条件 | if, unless, as long as |
| 原因 | because, since, as |
| 让步 | although, though, even though |

## 主谓一致

语法一致（主语单数→谓语单数）/ 就近原则（either...or / neither...nor / there be）/ 意义一致（family/team/class 视作整体用单数）

## 情态动词

can(能力)/could(委婉请求)/must(必须)/mustn''t(禁止)/may(允许/可能)/might(更委婉)
推测：must be（一定）, can''t be（不可能）, may be（可能）

## 虚拟语气

与现在相反：If + 过去式(were) → would + do
与过去相反：If + had done → would have done

## 情景交际

邀请：Would you like to...? / 建议：How about...? Why not...? You''d better... / 请求：Could you please...? Would you mind...? / 感谢回应：You''re welcome. My pleasure. / 道歉回应：It doesn''t matter. That''s OK.',
'九大时态+非谓语+三大从句+主谓一致+情态动词+虚拟语气+情景交际速查表。一张表覆盖语法专项全部考点。',
2, '["中等","语法","速查"]', 'PUBLISHED', NOW(), NOW());

SELECT 'v157: grammar index article inserted' AS result;
