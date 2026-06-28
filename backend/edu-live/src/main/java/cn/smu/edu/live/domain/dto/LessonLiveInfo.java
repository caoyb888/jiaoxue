package cn.smu.edu.live.domain.dto;

import lombok.Data;

/**
 * 课堂直播相关只读信息（自 edu-course 的 {@code lesson} 表读取，edu-live 仅读不写）。
 */
@Data
public class LessonLiveInfo {

    /** 直播模式：SLIDE_ONLY / ONLINE_CLASS。 */
    private String liveMode;

    private Long teacherId;

    /** 课堂状态：0-未开始 1-进行中 2-已结束。 */
    private Integer status;
}
