package com.stocking.infra.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    private static final String USER_MISSING = "of type FirebaseUser";

    @ExceptionHandler(value = {Exception.class, RuntimeException.class})
    @ResponseBody
    public Map<String, Object> doCommonExceptionProcess(HttpServletResponse httpServletResponse, Exception exception) {
        log.error(getPrintStackTrace(exception));
        
        Map<String, Object> resultMap = new HashMap<>();
        
        httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String errorMessage = exception.getLocalizedMessage();

        if (exception instanceof ServletRequestBindingException) {
            if(errorMessage.contains(USER_MISSING)) {
                resultMap.put("detailMessage", "헤더에 JWT 토큰을 추가해주세요. 발급: https://keehyun2.github.io/google-login.html");
            }
        }
        resultMap.put("errorMessage", errorMessage);
        
        return resultMap;
    }
    
    public static String getPrintStackTrace(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

}