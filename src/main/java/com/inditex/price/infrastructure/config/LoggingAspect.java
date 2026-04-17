package com.inditex.price.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.inditex.price.application..*(..))")
    public void applicationLayer() {
    }

    @Pointcut("execution(* com.inditex.price.infrastructure.adapter.outbound..*(..))")
    public void persistenceLayer() {
    }

    @Pointcut("execution(* com.inditex.price.infrastructure.adapter.inbound.rest..*(..))")
    public void restLayer() {
    }

    @Around("applicationLayer()")
    public Object logUseCase(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        Object[] args = pjp.getArgs();

        log.info("[USE-CASE] >>> {} args={}", method, args);
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("[USE-CASE] <<< {} result={} duration={}ms",
                    method, result, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("[USE-CASE] !!! {} threw {} in {}ms — message={}",
                    method,
                    ex.getClass().getSimpleName(),
                    System.currentTimeMillis() - start,
                    ex.getMessage());
            throw ex;
        }
    }

    @Around("persistenceLayer()")
    public Object logRepository(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed > 200) {
                log.warn("[REPOSITORY] SLOW QUERY {} — {}ms", method, elapsed);
            } else {
                log.debug("[REPOSITORY] {} — {}ms found={}", method, elapsed, result != null);
            }
            return result;
        } catch (Exception ex) {
            log.error("[REPOSITORY] !!! {} — {}", method, ex.getMessage());
            throw ex;
        }
    }

    @Around("restLayer()")
    public Object logRestCall(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        log.info("[REST] --> {}", method);
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("[REST] <-- {} status=OK duration={}ms",
                    method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.warn("[REST] <-- {} error={} duration={}ms",
                    method, ex.getClass().getSimpleName(),
                    System.currentTimeMillis() - start);
            throw ex;
        }
    }
}