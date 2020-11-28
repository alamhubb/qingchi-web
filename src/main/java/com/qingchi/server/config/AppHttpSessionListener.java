package com.qingchi.server.config;

import com.qingchi.base.config.AppConfigConst;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class AppHttpSessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        HttpSession session = sessionEvent.getSession();
        //从session中获取User
        Object userObj = session.getAttribute(AppConfigConst.appUserKey);
        if (userObj == null) {
//            SessionUtils.setUser(UserUtils.getUserByDB());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {

    }
}
 
