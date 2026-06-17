package moe.byn.bynspring21.exception;

/**
 * 内部控制流异常：用户显式取消聊天流时的信号。
 * 不继承 BynBaseException，避免被全局异常处理器捕获返回给客户端。
 */
public class ChatStreamCancelledException extends RuntimeException {

    public ChatStreamCancelledException(String msg) {
        super(msg);
    }

    public ChatStreamCancelledException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
