package com.everybuddy.global.util;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.everybuddy.domain.*.controller.*.*(..))")
    public void controllerLayer() {}

    @Pointcut("execution(* com.everybuddy.domain.*.service.*.*(..))")
    public void serviceLayer() {}

    @Around("controllerLayer()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "컨트롤러", getCurrentUserId());
    }

    @Around("serviceLayer()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "서비스", null);
    }

    private Object logExecution(ProceedingJoinPoint joinPoint, String layer, String userId) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        if (userId != null) {
            log.info("[{} 호출] {} - 사용자: {}", layer, methodName, userId);
        } else {
            log.info("[{} 호출] {}", layer, methodName);
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{} 완료] {} - 실행시간: {}ms", layer, methodName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{} 실패] {} - 실행시간: {}ms, 예외: {}",
                layer, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonymous";
        }

        return authentication.getName();
    }

}
