/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.implementation.spring.interceptors;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.interceptors.WaterAbstractInterceptor;
import lombok.Setter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


/**
 * @Author Aristide Cittadino
 */
@Aspect
@Component
public class SpringServiceInterceptor extends WaterAbstractInterceptor<Service> {
    private static Logger log = LoggerFactory.getLogger(SpringServiceInterceptor.class);
    @Setter
    @Autowired
    private ComponentRegistry componentRegistry;

    @Pointcut("execution(* *(..)) && target(it.water.core.api.service.Service+)")
    public void waterServicesPointcut() {
        //do nothing
    }

    @Before("waterServicesPointcut()")
    public void beforeServiceExecution(JoinPoint joinPoint) {
        try {
            Method method = MethodSignature.class.cast(joinPoint.getSignature()).getMethod();
            Object[] args = joinPoint.getArgs();
            Service target = (Service) joinPoint.getTarget();
            //setting original object before executing interception
            this.setService(target);
            this.executeInterceptorBeforeMethod(target, method, args);
        } catch (NoSuchMethodException e) {
            log.error(e.getMessage(), e);
        }
    }

    @AfterThrowing(value = "waterServicesPointcut()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Exception ex) {
        log.error(ex.getMessage(), ex);
    }

    @AfterReturning(value = "waterServicesPointcut()", returning = "result")
    public void afterServiceExecution(JoinPoint joinPoint, Object result) {
        try {
            Method method = MethodSignature.class.cast(joinPoint.getSignature()).getMethod();
            Object[] args = joinPoint.getArgs();
            Service target = (Service) joinPoint.getTarget();
            //setting original object before executing interception
            this.setService(target);
            this.executeInterceptorAfterMethod(target, method, args, result);
        } catch (NoSuchMethodException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected ComponentRegistry getComponentsRegistry() {
        return this.componentRegistry;
    }
}
