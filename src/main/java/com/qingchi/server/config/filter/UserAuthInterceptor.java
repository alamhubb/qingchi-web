package com.qingchi.server.config.filter;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.utils.UserUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class UserAuthInterceptor implements HandlerInterceptor {
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object arg2, Exception arg3) {
    }

    /*
     * 处理请求完成后视图渲染之前的处理操作
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) {
    }


    /*
     * 进入controller层之前拦截请求
     * 在请求处理之前进行调用（Controller方法调用之前
     */
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object o) {
        //不记录图片请求
        //不再记录请求记录
        /*String uri = req.getRequestURI();
        if (!uri.contains("/img/talk")) {
            UserDO user = UserUtils.getUser();
            OperateLogDO operateLogDO = new OperateLogDO();
            operateLogDO.setCreateTime(new Date());
            operateLogDO.setUri(uri);
            if (user != null) {
                operateLogDO.setUser(user);
                Optional<UserDetailDO> optionalUserDetailDO = userDetailRepository.findOneByUser(user);
                optionalUserDetailDO.ifPresent(userDetailDO -> {
                    operateLogDO.setDistrict(userDetailDO.getTalkQueryDistrict());
                    operateLogDO.setUseNearby(CommonStatus.useNearbyNum.equals(userDetailDO.getUseNearby()));
                });
            }
            operateLogRepository.save(operateLogDO);
        }*/
        UserDO user = UserUtils.getUserByDB();

        String uri = req.getRequestURI();
        System.out.println("uri:" + uri);

        if ((req.getMethod().equals(RequestMethod.OPTIONS.name())
                || req.getRequestURI().equals("/")
                || req.getRequestURI().contains("test")
                || req.getRequestURI().contains("queryTagTypes")
                || req.getRequestURI().contains("queryTags")
                || req.getRequestURI().contains("queryProvinceDistricts")
                || req.getRequestURI().contains("queryHotProvinceDistricts")
                || req.getRequestURI().contains("queryOtherHomeTypeTalks")
                || req.getRequestURI().contains("readChat")
                || req.getRequestURI().contains("queryChats")
                || req.getRequestURI().contains("refreshKeywords")
                || req.getRequestURI().contains("refreshConfigMap")
                || req.getRequestURI().contains("refreshRedis")
                || req.getRequestURI().contains("qqPayNotify")
                || req.getRequestURI().contains("wxPayNotify")
                || req.getRequestURI().contains("login")
                || req.getRequestURI().contains("user/platformLogin")
                || req.getRequestURI().contains("user/miniAppLogin")
                || req.getRequestURI().contains("user/appLogin")
                || req.getRequestURI().contains("app/checkUpdate")
                || req.getRequestURI().contains("match/queryMatchUsers")
                || req.getRequestURI().contains("appLogin")
                || req.getRequestURI().contains("wxLogin")
                || req.getRequestURI().contains("authentication")
                || req.getRequestURI().contains("sendAuthCode")
                || req.getRequestURI().contains("match/queryUsers")
                || req.getRequestURI().contains("talk/queryTalks")
                || req.getRequestURI().contains("message/queryMessages")
                || req.getRequestURI().contains("/webSocketServer")
                || req.getRequestURI().contains("/myHandler")
                || req.getRequestURI().contains("/myHandler")
                || req.getRequestURI().contains("/queryDistricts")
                || req.getRequestURI().contains("/queryHotDistricts")
                || req.getRequestURI().contains("/queryUserTalks")
                || req.getRequestURI().contains("/queryTalkDetail")
                || req.getRequestURI().contains("/queryUserDetail")
                || req.getRequestURI().contains("/queryAppInitData")
                || req.getRequestURI().contains("/queryAppInitDataLoad")
                || req.getRequestURI().contains("/queryAppInitDataLoad1")
                || req.getRequestURI().contains("/queryAppInitDataReady")
                || req.getRequestURI().contains("/img")
                || req.getRequestURI().contains("/css")
                //这里只查询没被封禁的
                || user != null)
                //不允许读取idcard信息
                && !req.getRequestURI().contains("/img/idCard")
        ) {
            if (user != null) {
                req.setAttribute(AppConfigConst.appUserKey, user);
            }
            return true;
        } else {
            String origin = req.getHeader("Origin");
            res.setHeader("Access-Control-Allow-Origin", origin);
            res.setHeader("Access-Control-Allow-Methods", "*");
            res.setHeader("Access-Control-Allow-Headers", "Origin,Content-Type,Accept,token,X-Requested-With");
            res.setHeader("Access-Control-Allow-Credentials", "true");
            //这里只提示未登录
            res.setStatus(ErrorCode.NOT_LOGGED_ERROR);
            return false;
        }
    }
}