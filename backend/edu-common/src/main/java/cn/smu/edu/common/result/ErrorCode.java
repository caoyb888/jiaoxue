package cn.smu.edu.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用
    SUCCESS(200, "ok"),
    SYSTEM_ERROR(500, "系统内部错误"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // 认证模块 1001xx
    SMS_CODE_ERROR(100101, "验证码错误或已过期"),
    SMS_SEND_TOO_FREQUENT(100102, "短信发送过于频繁"),
    WECHAT_AUTH_FAIL(100103, "微信授权失败"),
    TOKEN_INVALID(100104, "Token 无效"),
    TOKEN_EXPIRED(100105, "Token 已过期"),
    ACCOUNT_DISABLED(100106, "账号已被禁用"),

    // 用户模块 1002xx
    USER_NOT_FOUND(100201, "用户不存在"),
    DEPT_NOT_FOUND(100202, "院系不存在"),
    USER_ALREADY_EXISTS(100203, "用户已存在"),

    // 课程模块 1003xx
    COURSE_NOT_FOUND(100301, "课程不存在"),
    CLASS_NOT_FOUND(100302, "教学班不存在"),
    LESSON_NOT_FOUND(100303, "课堂不存在"),
    LESSON_NOT_STARTED(100304, "课堂未开始"),
    LESSON_ALREADY_ENDED(100305, "课堂已结束"),
    MATERIAL_CONVERT_FAIL(100306, "课件转换失败"),

    // 互动模块 1004xx
    ATTEND_CODE_INVALID(100401, "签到码无效或已过期"),
    ATTEND_ALREADY(100402, "已签到，请勿重复操作"),
    NOT_IN_CLASS(100403, "您不在该教学班中"),

    // 考试模块 1005xx
    EXAM_NOT_FOUND(100501, "考试不存在"),
    EXAM_NOT_STARTED(100502, "考试未开始"),
    EXAM_ALREADY_ENDED(100503, "考试已结束"),
    EXAM_PASSWORD_WRONG(100504, "考试密码错误"),
    EXAM_ALREADY_SUBMITTED(100505, "已提交，请勿重复提交"),
    FACE_VERIFY_FAIL(100506, "人脸核验未通过"),

    // 成绩模块 1006xx
    GRADE_RULE_WEIGHT_ERROR(100601, "成绩权重之和必须等于100"),
    GRADE_NOT_FOUND(100602, "成绩记录不存在"),

    // AI 模块 2007xx
    AI_TASK_NOT_FOUND(200701, "AI任务不存在"),
    PROMPT_SECURITY_BLOCKED(200702, "输入内容包含违规信息，已被拦截"),
    AI_SERVICE_UNAVAILABLE(200703, "AI服务暂时不可用，请稍后重试"),

    // 文件模块 1008xx
    FILE_NOT_FOUND(100801, "文件不存在"),
    FILE_UPLOAD_FAIL(100802, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(100803, "不支持的文件类型"),

    // 教务对接 1009xx
    JWXT_SYNC_FAIL(100901, "教务系统同步失败"),
    JWXT_CONNECT_FAIL(100902, "教务系统连接失败");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
