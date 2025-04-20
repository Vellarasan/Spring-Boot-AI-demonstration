package io.vels.ai.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(public * io.vels.ai.*.*.*(..))")
    private void allPublicMethods() {
    }

    @Around(
            value = "allPublicMethods()"
    )
    private Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        log.info("Entering Method >> {}() - with Args :: {}", methodName, Arrays.toString(args));
        Object result = joinPoint.proceed();
        log.info("Exiting Method << {}()", methodName);
        return result;
    }
}
