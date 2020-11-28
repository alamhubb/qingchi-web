package com.qingchi.server.store;

import com.qingchi.base.model.user.UserLogDO;
import com.qingchi.base.repository.log.UserLogRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class UserLogStoreUtils {

    private static UserLogRepository userLogRepository;

    @Resource
    public void setUserLogRepository(UserLogRepository userLogRepository) {
        UserLogStoreUtils.userLogRepository = userLogRepository;
    }

    public static void save(UserLogDO userLogDO) {
        userLogRepository.save(userLogDO);
    }
}

