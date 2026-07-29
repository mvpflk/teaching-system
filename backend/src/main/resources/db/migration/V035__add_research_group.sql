 -- v160: 课题研究 — 班级增加课题组别标签（实验班/对照班）
 ALTER TABLE classes ADD COLUMN research_group VARCHAR(20) DEFAULT NULL COMMENT '课题组别: EXPERIMENT=实验班, CONTROL=对照班';
