package cn.smu.edu.jwxt.adapter;

/**
 * 适配层归一化记录：把正方/强智各自的响应字段统一成 {@code (dataType, jwxtId, rawJson)}。
 *
 * @param dataType 数据类型 STUDENT/DEPT/COURSE/CLASS
 * @param jwxtId   教务系统主键（字符串）
 * @param rawJson  归一后的原始 JSON（落 jwxt_raw_data.raw_json）
 */
public record JwxtRecord(String dataType, String jwxtId, String rawJson) {
}
