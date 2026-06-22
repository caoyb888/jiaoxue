-- S6-05：课堂摘要 key_points 抽取。lesson_report 增加 AI 关键点字段（JSON 数组字符串）。
ALTER TABLE lesson_report
    ADD COLUMN ai_key_points JSON NULL COMMENT 'AI抽取的课堂关键点（JSON字符串数组，ASR→LLM）' AFTER ai_summary;
