package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 拦截的切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}

    /**
     * 前置通知，在通知中进行公共字段的赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("公共字段自动填充");
        //获取当前被拦截的方法上的数据库操作类型
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();//获取方法签名
        AutoFill autoFill = methodSignature.getMethod().getAnnotation(AutoFill.class);//获取方法签名上的数据库操作类型
        OperationType operationType = autoFill.value();//获取数据库操作类型
        //获取当前被拦截的方法参数，即实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0){
            return;
        }
        Object object = args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据数据库操作类型，为实体对象赋值(反射)
        if (operationType == OperationType.INSERT){
            //为4个公共字段赋值
            try {
                Method setCreateTime = object.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                Method setCreateUser = object.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setUpdateTime = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                //通过反射为对象属性赋值
                setCreateTime.invoke(object,now);
                setCreateUser.invoke(object,currentId);
                setUpdateTime.invoke(object,now);
                setUpdateUser.invoke(object,currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if(operationType == OperationType.UPDATE){
            try {
                Method setUpdateTime = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                //通过反射为对象属性赋值
                setUpdateTime.invoke(object,now);
                setUpdateUser.invoke(object,currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
