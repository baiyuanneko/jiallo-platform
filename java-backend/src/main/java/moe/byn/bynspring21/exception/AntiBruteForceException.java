package moe.byn.bynspring21.exception;

public class AntiBruteForceException extends BynBaseException {

    public AntiBruteForceException(String msg) {
        super(msg);
    }

    public AntiBruteForceException(String msg, int code) {
        super(msg, code);
    }

    public AntiBruteForceException(String msg, Throwable e) {
        super(msg, e);
    }

    public AntiBruteForceException(String msg, Throwable e, int code) {
        super(msg, e, code);
    }
}
