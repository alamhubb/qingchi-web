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
        /*String uri = uri;
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

        if ((req.getMethod().equals(RequestMethod.OPTIONS.name())
                || uri.equals("/")
                || uri.contains("test")
                || uri.contains("queryTagTypes")
                || uri.contains("queryTags")
                || uri.contains("queryProvinceDistricts")
                || uri.contains("queryHotProvinceDistricts")
                || uri.contains("queryOtherHomeTypeTalks")
                || uri.contains("readChat")
                || uri.contains("queryChats")
                || uri.contains("refreshKeywords")
                || uri.contains("refreshConfigMap")
                || uri.contains("refreshRedis")
                || uri.contains("qqPayNotify")
                || uri.contains("wxPayNotify")
                || uri.contains("login")
                || uri.contains("user/platformLogin")
                || uri.contains("user/miniAppLogin")
                || uri.contains("user/appLogin")
                || uri.contains("app/checkUpdate")
                || uri.contains("match/queryMatchUsers")
                || uri.contains("appLogin")
                || uri.contains("wxLogin")
                || uri.contains("authentication")
                || uri.contains("sendAuthCode")
                || uri.contains("match/queryUsers")
                || uri.contains("talk/queryTalks")
                || uri.contains("message/queryMessages")
                || uri.contains("/webSocketServer")
                || uri.contains("/myHandler")
                || uri.contains("/queryDistricts")
                || uri.contains("/queryHotDistricts")
                || uri.contains("/queryUserTalks")
                || uri.contains("/queryTalkDetail")
                || uri.contains("/queryUserDetail")
                || uri.contains("/queryAppInitData")
                || uri.contains("/queryAppInitDataLoad")
                || uri.contains("/queryAppInitDataLoad1")
                || uri.contains("/queryAppInitDataReady")
                || uri.contains("/img")
                || uri.contains("/css")
                //这里只查询没被封禁的
                || user != null)
                //不允许读取idcard信息
                && !uri.contains("/img/idCard")
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