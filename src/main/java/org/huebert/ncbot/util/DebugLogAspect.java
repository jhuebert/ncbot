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

    private static final int MAX_ITEMS = 5;
    private static final int MAX_LENGTH = 500;

    private String format(Object... args) {
        if (args == null) {
            return "";
        }
        return Arrays.stream(args)
                .map(DebugLogAspect::formatValue)
                .collect(Collectors.joining(", "));
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        java.util.List<Object> items;
        int total;
        if (value instanceof Iterable<?> iterable) {
            items = new java.util.ArrayList<>();
            int n = 0;
            for (Object item : iterable) {
                n++;
                if (items.size() <= MAX_ITEMS) {
                    items.add(item);
                }
            }
            total = n;
        } else if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            total = len;
            items = new java.util.ArrayList<>();
            for (int i = 0; i < len && items.size() <= MAX_ITEMS; i++) {
                items.add(java.lang.reflect.Array.get(value, i));
            }
        } else {
            String s = String.valueOf(value);
            if (s.length() > MAX_LENGTH) {
                s = s.substring(0, MAX_LENGTH) + "...";
            }
            return s;
        }

        StringBuilder sb = new StringBuilder("[");
        int shown = Math.min(items.size(), MAX_ITEMS);
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.valueOf(items.get(i)));
        }
        if (total > shown) {
            sb.append(", ... ").append(total - shown).append(" more");
        }
        return sb.append("]").toString();
    }

}
