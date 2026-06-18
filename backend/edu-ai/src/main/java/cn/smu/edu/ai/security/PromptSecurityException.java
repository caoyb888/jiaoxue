package cn.smu.edu.ai.security;

import cn.smu.edu.common.result.ErrorCode;

public class PromptSecurityException extends RuntimeException {

    private final int code;

    public PromptSecurityException(String message) {
        super(message);
        this.code = ErrorCode.PROMPT_SECURITY_BLOCKED.getCode();
    }

    public int getCode() {
        return code;
    }
}
