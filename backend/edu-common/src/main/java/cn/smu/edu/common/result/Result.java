package cn.smu.edu.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(int code, String msg, T data) implements Serializable {

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "ok", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
