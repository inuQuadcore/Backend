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
        String methodName = joinPoint.getSignature().toShortString();
        String userId = getCurrentUserId();

        log.info("[컨트롤러 호출] {} - 사용자: {}", methodName, userId);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[컨트롤러 완료] {} - 실행시간: {}ms", methodName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[컨트롤러 실패] {} - 실행시간: {}ms, 예외: {}",
                methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    @Around("serviceLayer()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        log.info("[서비스 호출] {}", methodName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[서비스 완료] {} - 실행시간: {}ms", methodName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[서비스 실패] {} - 실행시간: {}ms, 예외: {}",
                methodName, executionTime, e.getMessage());
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
