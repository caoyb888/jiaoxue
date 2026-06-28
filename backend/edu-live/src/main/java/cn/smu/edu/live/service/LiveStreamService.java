package cn.smu.edu.live.service;

/**
 * SRS 流媒体回调处理（S8-02）——推流生命周期 + DVR 录制文件落 MinIO 生成回放。
 */
public interface LiveStreamService {

    /** SRS on_publish：推流开始 → live_record 置推流中。 */
    void onPublish(String streamKey);

    /** SRS on_unpublish：推流结束 → live_record 置已结束并计算时长。 */
    void onUnpublish(String streamKey);

    /** SRS on_dvr：录制文件落盘 → 上传 MinIO 回放桶，置已生成回放。 */
    void onDvr(String streamKey, String dvrFilePath);
}
