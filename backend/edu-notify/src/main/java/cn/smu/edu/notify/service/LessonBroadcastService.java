package cn.smu.edu.notify.service;

/**
 * 课堂广播服务（统一管理所有 STOMP /topic/ 广播）
 */
public interface LessonBroadcastService {

    /** 广播课件翻页 */
    void broadcastSlideChange(Long lessonId, int slideIndex);

    /** 广播签到人数更新 */
    void broadcastAttendCount(Long lessonId, long count);

    /** 广播弹幕（前台匿名内容） */
    void broadcastBarrage(Long lessonId, String content, String style);

    /** 广播随机点名结果 */
    void broadcastRollCall(Long lessonId, Object rollCallResult);

    /** 广播 AI 任务完成通知 */
    void broadcastAiDone(Long lessonId, String taskType, String message);

    /** 向指定用户发送单播消息 */
    void sendToUser(Long userId, String type, Object payload);
}
