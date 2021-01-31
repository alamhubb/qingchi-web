package com.qingchi.server.config.filter;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.entity.ErrorLogUtils;
import com.qingchi.base.model.monitoring.ErrorLogDO;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.base.utils.UserUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public class MyResponseBodyAdvice implements ResponseBodyAdvice {
    /**
     * 异常处理，返回自定义的异常对象json
     *
     * @param req
     * @param e
     * @return
     * @throws Exception
     */
    @ExceptionHandler(value = Throwable.class)
    @ResponseBody
    public ResultVO<String> jsonErrorHandler(HttpServletRequest req, Throwable e) {
        QingLogger.logger.error(e.getMessage());
        QingLogger.logger.error("error:", e);
        ErrorLogUtils.save(new ErrorLogDO(UserUtils.getUserId(), "拦截器，系统错误", e.getMessage()));
        ResultVO<String> resultVO = new ResultVO(ErrorCode.SYSTEM_ERROR);
        return resultVO;
    }

    @Override
    public Object beforeBodyWrite(Object result, MethodParameter methodParameter,
                                  MediaType mediaType, Class clas, ServerHttpRequest serverHttpRequest,
                                  ServerHttpResponse serverHttpResponse) {
        ServletServerHttpResponse sshrp = (ServletServerHttpResponse) serverHttpResponse;
        HttpServletResponse response = sshrp.getServletResponse();
        ResultVO responseVO;
        if (response.getStatus() != 200) {
            responseVO = new ResultVO(ErrorCode.SYSTEM_ERROR);
        } else {
            if (result instanceof ResultVO) {
                responseVO = (ResultVO) result;
            } else {
                return result;
            }
        }
        if (responseVO.getErrorCode() > 0) {
            response.setStatus(responseVO.getErrorCode());
        }
        //返回修改后的值
        return responseVO;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Class c) {
        //不拦截
        return true;
    }

    /*@ExceptionHandler
    public ResponseVO handlerServerException(Exception exception) {
        exception.printStackTrace();
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String msg = "server error, please try again later";
        Class exceptionClazz = exception.getClass();
        if (Objects.equals(MissingServletRequestParameterException.class, exceptionClazz)) {
            msg = "incorrect parameter";
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (Objects.equals(HttpRequestMethodNotSupportedException.class, exceptionClazz)) {
            httpStatus = HttpStatus.BAD_REQUEST;
            msg = exception.getMessage();
        }
        return new ResponseEntity(new ResultUtil<String>().setError(httpStatus.value(), msg, ""), httpStatus);
    }*/
}