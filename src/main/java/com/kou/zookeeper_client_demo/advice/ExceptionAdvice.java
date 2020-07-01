package com.kou.zookeeper_client_demo.advice;

import com.google.common.collect.ImmutableMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(Exception.class)
    public Map<String, String> sendErrorMsg(Exception e) {
        e.printStackTrace();
        return ImmutableMap.of("code", "-2", "errorMsg", "服务异常");
    }
}