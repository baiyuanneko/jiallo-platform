package moe.byn.bynspring21.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

public class BynBaseException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 6364841816941364953L;

    @Setter
    @Getter
    private String msg;
    @Setter
    @Getter
    private int code;

    @Setter
    @Getter
    private Object data;

    public BynBaseException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public BynBaseException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }
    public BynBaseException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public BynBaseException(String msg, Throwable e, int code) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }
}
