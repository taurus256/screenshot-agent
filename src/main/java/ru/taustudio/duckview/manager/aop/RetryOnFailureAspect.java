package ru.taustudio.duckview.manager.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RetryOnFailureAspect {
   @Around(value = "@annotation(annotation)", argNames = "jp, annotation")
   public Object repeatableCall(ProceedingJoinPoint jp, RetrytOnFailure annotation) throws Throwable {
       int counter = annotation.value();
       for (int i=0; i< counter; i++) {
           try {
               return jp.proceed();
           } catch (Throwable trw) {
               trw.printStackTrace();
               System.out.println("RETRYING...");
           }
       }
       throw new TooManyAttemptsException(annotation.value() + " attempts was failed!");
   }
}
