-- v84: 英语分级阅读短文池
-- Applied: 2026-06-05
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS precision_english_reading_passages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL COMMENT '短文内容',
    word_count INT DEFAULT 0,
    difficulty_level INT DEFAULT 1 COMMENT '1-5',
    min_vocab_size INT DEFAULT 0 COMMENT '阅读该文所需最低词汇量',
    grammar_tags VARCHAR(200) COMMENT '涉及的语法标签',
    new_word_list VARCHAR(500) COMMENT '目标生词列表',
    question_ids JSON COMMENT '[{"qid":101,"order":1},...]',
    source VARCHAR(20) DEFAULT 'MANUAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_difficulty (difficulty_level),
    INDEX idx_vocab (min_vocab_size)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语分级阅读短文池';

-- 初始 8 篇短文 (30-80词，分级 level 1-5)
INSERT INTO precision_english_reading_passages (title, content, word_count, difficulty_level, min_vocab_size, grammar_tags, new_word_list) VALUES
('My Daily Routine',
 'Every day, Tom wakes up at 7:00. He brushes his teeth and washes his face. Then he eats breakfast. He usually has bread and milk. After breakfast, he goes to school by bus. At school, he studies English, math, and science. He goes home at 4:00 in the afternoon. In the evening, he does his homework and watches TV. He goes to bed at 10:00.',
 72, 1, 200, '一般现在时', 'routine,brushes,science'),
('A Busy Weekend',
 'Last Saturday, Lisa got up early. She cleaned her room first. Then she went to the supermarket with her mother. They bought fruits, vegetables, and some snacks. In the afternoon, Lisa visited her grandmother. She helped her cook dinner. They had a wonderful time together. On Sunday, Lisa finished her homework and watched a movie.',
 66, 2, 250, '一般过去时', 'supermarket,snacks,visited,wonderful'),
('My Future Job',
 'When I grow up, I want to be a nurse. I will help sick people get better. I will work in a hospital. My mother is a nurse too. She says the job is hard but meaningful. I will study hard at school to make my dream come true. I believe I can do it!',
 56, 2, 250, '一般将来时', 'nurse,meaningful,believe'),
('A Phone Call',
 'Right now, I am doing my homework at home. My phone suddenly rings. It is my friend Mark. He is calling to ask about the math test tomorrow. I tell him that I am studying for it too. We decide to study together at the library. I feel much better now!',
 53, 3, 300, '现在进行时', 'suddenly,rings,library'),
('The Lost Wallet',
 'Yesterday, when I was walking home from school, I found a wallet on the ground. I picked it up and looked inside. There was some money and a student card. The name on the card was Li Wei. I remembered that Li Wei is in Class 3. I went to find him and returned the wallet. He was very happy and thanked me.',
 66, 3, 320, '过去进行时', 'wallet,ground,returned'),
('A Gift from My Father',
 'My father has given me many gifts over the years. But the most special one is a diary. He gave it to me on my 12th birthday. I have written in it almost every day since then. It has become my best friend. I have recorded all my happy moments and sad times in that diary.',
 58, 4, 380, '现在完成时', 'diary,recorded,moments'),
('The Boy Who Loved Books',
 'There was once a boy who loved reading. The library was his favorite place. The librarian, who knew him well, always saved new books for him. One day, he read a book about robots that changed his life. He decided to become an engineer. His parents, who had always supported him, were very proud.',
 58, 4, 420, '定语从句/状语从句', 'librarian,robots,engineer,supported,proud'),
('A Letter to Myself',
 'Dear future me, I hope you have finished your studies. I hope you have found a job that you love. Remember the promises you made to yourself. Do not forget to call mom and dad often. I am writing this letter to remind you of what really matters in life. Be kind, be brave, and never stop learning.',
 60, 5, 460, '现在完成时/不定式', 'promises,remind,matters,brave');
