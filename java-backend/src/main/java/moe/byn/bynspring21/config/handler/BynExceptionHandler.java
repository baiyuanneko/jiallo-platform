package moe.byn.bynspring21.config.handler;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.base.R;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class BynExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        if (CollectionUtil.isNotEmpty(e.getBindingResult().getFieldErrors())) {
            String validateMessage = e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
            return R.error(validateMessage);
        }
        return R.error("参数错误");
    }

    /**
     * 请求参数key不对
     * @param e
     * @return
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R handlerMissingServletRequestParameterException(Exception e) {
        log.error(e.getMessage(), e);
        return R.error(1001, "参数错误");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public R handlerNoFoundException(Exception e) {
        log.error(e.getMessage(), e);
        return R.error(404, "路径不存在，请检查路径是否正确");
    }


    /**
     * 数据库中已存在该记录
     * @param e
     * @return
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R handleDuplicateKeyException(DuplicateKeyException e) {
        log.error(e.getMessage(), e);
        return R.error(300, "数据库中已存在该记录");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R handleAuthorizationException(AccessDeniedException e) {
        log.error(e.getMessage());
        return R.error(403, "没有权限，请联系管理员授权");
    }

    @ExceptionHandler(AccountExpiredException.class)
    public R handleAccountExpiredException(AccountExpiredException e) {
        log.error(e.getMessage(), e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public R handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.error(e.getMessage(), e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R handleException(Exception e) {
        log.error(e.getMessage(), e);
        String message = e.getMessage();
        return R.error(e.getMessage());
    }

    @ExceptionHandler(SQLException.class)
    public R handlerSqlException(SQLException e) {
        log.error(e.getMessage(), e);
        return R.error(e.getMessage());
    }

//    @ExceptionHandler(ValidateCodeException.class)
//    public R handleValidateCodeException(ValidateCodeException e) {
//        log.error(e.getMessage(), e);
//        return R.error(e.getMessage());
//    }
}
