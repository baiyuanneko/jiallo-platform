package moe.byn.bynspring21.entity.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Schema(description = "通用响应结构")
@Data
@ToString
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    public static final int SUCCESS_CODE = 0;
    public static final int FAIL_CODE = -1;

    public static final String SUCCESS = "操作成功";
    public static final String FAIL = "操作失败";

    @Serial
    private static final long serialVersionUID = -1177181094534263527L;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @Schema(description = "状态码，0表示成功，其他表示失败", example = "0")
    private int code;

    // ========== 泛型方法（推荐使用，类型安全） ==========

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.setMessage(SUCCESS);
        r.setCode(SUCCESS_CODE);
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setMessage(SUCCESS);
        r.setData(data);
        r.setCode(SUCCESS_CODE);
        return r;
    }

    public static <T> R<T> ok(int code, T data) {
        R<T> r = new R<>();
        r.setMessage(SUCCESS);
        r.setCode(code);
        r.setData(data);
        return r;
    }

    public static <T> R<T> ok(int code, String message, T data) {
        R<T> r = new R<>();
        r.setMessage(message);
        r.setData(data);
        r.setCode(code);
        return r;
    }

    public static <T> R<T> error() {
        R<T> r = new R<>();
        r.setMessage(FAIL);
        r.setCode(FAIL_CODE);
        return r;
    }

    public static <T> R<T> error(String message) {
        R<T> r = new R<>();
        r.setMessage(message);
        r.setCode(FAIL_CODE);
        return r;
    }

    public static <T> R<T> error(String message, T data) {
        R<T> r = new R<>();
        r.setMessage(message);
        r.setData(data);
        r.setCode(FAIL_CODE);
        return r;
    }

    public static <T> R<T> error(int code, String message) {
        R<T> r = new R<>();
        r.setMessage(message);
        r.setCode(code);
        return r;
    }

    public static <T> R<T> error(int code, String message, T data) {
        R<T> r = new R<>();
        r.setMessage(message);
        r.setData(data);
        r.setCode(code);
        return r;
    }

}
