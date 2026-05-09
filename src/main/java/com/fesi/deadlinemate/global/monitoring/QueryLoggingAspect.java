package com.fesi.deadlinemate.global.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Profile("local")
@Slf4j
public class QueryLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController) && execution(public * *(..))")
    public Object logQueryCount(ProceedingJoinPoint pjp) throws Throwable {
        QueryCountContext.initialize();
        try {
            return pjp.proceed();
        } finally {
            logAndClear();
        }
    }

    private void logAndClear() {
        try {
            QueryCountContext ctx = QueryCountContext.current();
            if (ctx == null || !ctx.hasN1()) return;

            String url = resolveUrl();
            log.debug("Query Statistics: URL = {}, Query Count = {}, Query Time = {}(ms)",
                    url, ctx.getTotalCount(), ctx.getTotalTimeMs());
        } finally {
            QueryCountContext.clear();
        }
    }

    private String resolveUrl() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            return req.getMethod() + " " + req.getRequestURI();
        } catch (IllegalStateException e) {
            return "unknown";
        }
    }
}
