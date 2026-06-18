package cn.smu.edu.ai.domain.model;

/**
 * AI 模型类型路由枚举
 *   ANALYSIS   → 文心4（逻辑推理/分析类任务）
 *   GENERATION → 通义Qwen（创意生成/文本写作）
 *   REVIEW     → GPT-4o（主观题批改，精度要求最高）
 */
public enum ModelType {
    ANALYSIS,
    GENERATION,
    REVIEW
}
