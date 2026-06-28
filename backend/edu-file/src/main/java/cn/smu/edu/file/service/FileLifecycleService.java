package cn.smu.edu.file.service;

/**
 * 文件生命周期治理（S8-06）。
 *
 * <p>直播回放（biz_type=VIDEO）超 N 天转冷存储；考试附件（EXAM_ATTACH）超 M 天删除。
 */
public interface FileLifecycleService {

    /** 执行一次生命周期检查。 */
    Result runLifecycleCheck();

    /**
     * @param coldCount    转冷存储数量
     * @param deletedCount 删除数量
     */
    record Result(int coldCount, int deletedCount) {
    }
}
