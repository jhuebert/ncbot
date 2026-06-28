package org.huebert.ncbot.util;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class DebugLogAspect {

    @Around("@annotation(org.huebert.ncbot.util.DebugLog)")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String shortName = sig.getDeclaringType().getSimpleName();
        String methodName = sig.getName();
        String args = format(pjp.getArgs());

        log.debug("{}.{}({})", shortName, methodName, args);

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("{}.{}({} ms) -> {}", shortName, methodName, elapsed, format(result));
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.debug("{}.{}({} ms) -> {}", shortName, methodName, elapsed, t.getClass().getSimpleName());
            throw t;
        }
    }

    private String format(Object... args) {
        if (args == null) {
            return "";
        }
        return Arrays.stream(args)
                .map(a -> a == null ? "null" : a.toString())
                .collect(Collectors.joining(", "));
    }

}
