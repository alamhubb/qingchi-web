package com.qingchi.server.config;

import com.qingchi.base.model.system.OperateLogDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.log.OperateLogRepository;
import com.qingchi.base.utils.IpUtil;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.base.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@Aspect
@Component
@Slf4j
public class LogAspect {
    @Resource
    private OperateLogRepository operateLogRepository;
    //用来记录请求进入的时间，防止多线程时出错，这里用了ThreadLocal
    ThreadLocal<Long> startTime = new ThreadLocal<>();
    ThreadLocal<Long> operateId = new ThreadLocal<>();

    /**
     * 定义切入点，controller下面的所有类的所有公有方法，这里需要更改成自己项目的
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)||@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void requestLog() {
    }

    /**
     * 方法之前执行，日志打印请求信息
     *
     * @param joinPoint joinPoint
     */
    @Before("requestLog()")
    public void DOBefore(JoinPoint joinPoint) {
        Date startDate = new Date();
        startTime.set(startDate.getTime());
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String uri = request.getRequestURI();
        String params = Arrays.toString(joinPoint.getArgs());
        //打印当前的请求路径
        OperateLogDO operateLogDO = new OperateLogDO();
        UserDO user = RequestUtils.getUser(request);
        if (user != null) {
            operateLogDO.setUserId(user.getId());
        }
        operateLogDO.setIp(IpUtil.getIpAddr(request));
        operateLogDO.setCreateTime(startDate);
        operateLogDO.setUri(uri);
        operateLogDO = operateLogRepository.save(operateLogDO);
        long operateDOId = operateLogDO.getId();
        operateId.set(operateDOId);
        QingLogger.logger.debug("------------RequestMapping-----------:[{}:{}]", operateDOId, uri);
        QingLogger.logger.debug("RequestParam:{}", params);
    }

    /**
     * 方法返回之前执行，打印才返回值以及方法消耗时间
     *
     * @param response 返回值
     */
    @AfterReturning(returning = "response", pointcut = "requestLog()")
    public void DOAfterRunning(Object response) {
        String responseStr = "";
        if (response != null) {
            responseStr = response.toString();
        }//打印返回值信息
        Date endDate = new Date();
        long spendTime = endDate.getTime() - startTime.get();
        long operateDOId = operateId.get();
        //打印返回值信息
        QingLogger.logger.debug("Response:[{}]", responseStr);
        //打印请求耗时
        QingLogger.logger.debug("----------Request spend times---------- : [{}:{}ms]", operateDOId, spendTime);
        //打印请求耗时
        Optional<OperateLogDO> operateLogDOOptional = operateLogRepository.findById(operateDOId);
        if (operateLogDOOptional.isPresent()) {
            OperateLogDO operateLogDO = operateLogDOOptional.get();
            operateLogDO.setEndTime(endDate);
            operateLogDO.setSpendTime(spendTime);
            operateLogRepository.save(operateLogDO);
        } else {
            QingLogger.logger.warn("异常，没有记录开始的请求");
        }
    }
}