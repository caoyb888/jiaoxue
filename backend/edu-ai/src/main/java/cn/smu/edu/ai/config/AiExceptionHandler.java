package cn.smu.edu.ai.config;

import cn.smu.edu.ai.security.PromptSecurityException;
import cn.smu.edu.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(PromptSecurityException.class)
    public Result<?> handlePromptSecurity(PromptSecurityException e) {
        log.warn("Prompt 安全拦截: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
}
