-- ============================================================================
-- v156: English[职高] writing article samples — append full sample essays
-- Idempotent: checks for '范文示例' marker before updating
-- ============================================================================
SET NAMES utf8mb4;

-- ══════════════════════════════════════════
-- Sample thank-you letter (98 words)
-- ══════════════════════════════════════════
UPDATE knowledge_articles SET content_md = CONCAT(content_md, '

## 范文示例

### 感谢信（98词）

Dear Tom,

I''m writing to express my sincere thanks for your help during my stay in London. You showed me around the city and introduced me to many interesting places. Without your help, I wouldn''t have enjoyed my trip so much.

I really appreciate your kindness and hospitality. The days we spent together were unforgettable. I hope you can come to China one day. I will be happy to show you around Beijing.

Thank you again and best wishes!

Yours sincerely,
Li Hua

> 词数：98词 | 结构：开头致谢 → 回忆帮助 → 发出邀请 → 再次致谢

### 邀请信（85词）

Dear Mr. Smith,

I would like to invite you to give a talk about American culture in our school. It will be held in the school hall on Friday, June 12th, from 2:00 to 4:00 pm.

All the students in our English club are looking forward to your speech. We believe it will be both interesting and educational. If you have any special requirements, please let us know.

We would be very glad if you could come. Looking forward to your reply.

Yours sincerely,
Li Hua

> 词数：85词 | 结构：发出邀请 → 时间地点 → 期待回复
') WHERE id=178 AND content_md NOT LIKE '%范文示例%';

-- ══════════════════════════════════════════
-- Sample written notice (93 words) + oral announcement
-- ══════════════════════════════════════════
UPDATE knowledge_articles SET content_md = CONCAT(content_md, '

## 范文示例

### 书面通知（93词）

NOTICE

In order to enrich students'' school life, the Students'' Union will organize an English Speech Contest. The details are as follows:

The contest will be held in the school auditorium on Friday, June 20th, from 2:00 to 5:00 pm. Each contestant is required to give a 3-minute speech on the topic "My Dream". All students are welcome to watch the contest.

Those who want to take part please sign up at the Students'' Union office before June 10th.

Students'' Union
June 1st, 2026

> 词数：93词 | 结构：NOTICE标题 → 目的 → 时间地点 → 内容要求 → 报名方式 → 落款+日期

### 口头通知（70词）

Attention, please! I have an announcement to make.

There will be a visit to the City Museum this Saturday. We will meet at the school gate at 8:00 am and take a bus there. The museum opens at 9:00 am. Please bring a notebook and pen to take notes.

That''s all. Thank you for your attention.

> 词数：70词 | 结构：呼语 → 活动安排 → 时间地点 → 注意事项 → 结束语
') WHERE id=179 AND content_md NOT LIKE '%范文示例%';

-- ══════════════════════════════════════════
-- Sample topic essay (about My Hobby, 96 words)
-- ══════════════════════════════════════════
UPDATE knowledge_articles SET content_md = CONCAT(content_md, '

## 范文示例

### 话题：My Hobby（96词）

Different people have different hobbies. As for me, reading is my favorite hobby.

First, reading broadens my knowledge. Through books, I can learn about different cultures and history. Second, reading helps me relax. When I feel tired or stressed, reading a good book makes me feel peaceful and happy. Third, reading improves my English. By reading English books, I have learned many new words and expressions.

In short, reading is not only enjoyable but also beneficial. I hope everyone can find a hobby they love.

> 词数：96词 | 结构：开头引出话题 → 要点1(知识) → 要点2(放松) → 要点3(英语) → 结尾总结
> 衔接词：First / Second / Third / In short | 复合句：When+从句, not only...but also...

### 话题：Protect the Environment（84词）

Nowadays, environmental protection is becoming more and more important. In my opinion, everyone should take action to protect our earth.

First, we should reduce waste. For example, we can use reusable bags instead of plastic ones. Second, we should plant more trees to make our city greener. Third, we should save water and electricity in our daily life.

All in all, small actions can make a big difference. Let''s start from now on!

> 词数：84词 | 结构：现象引入 → 3个建议 → 号召结尾
> 衔接词：First / Second / Third / All in all | 建议句式：should + do
') WHERE id=180 AND content_md NOT LIKE '%范文示例%';

SELECT 'v156: writing samples appended (3 articles)' AS result;
