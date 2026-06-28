package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiGroupDiscussion;
import cn.smu.edu.common.event.DiscussionMessageEvent;

/**
 * 分组讨论 AI 汇总服务（S8-04）。
 */
public interface DiscussionService {

    /** 收集一条讨论发言（upsert 文档并追加消息）。 */
    void appendMessage(DiscussionMessageEvent event);

    /** 触发某组讨论的 LLM 汇总分析（讨论结束时调用）。 */
    AiGroupDiscussion summarize(Long lessonId, Long groupId);

    /** 读取某组讨论汇总（无则返回 {@code null}）。 */
    AiGroupDiscussion getDiscussion(Long lessonId, Long groupId);
}
