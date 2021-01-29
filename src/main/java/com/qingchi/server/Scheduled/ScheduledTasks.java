package com.qingchi.server.Scheduled;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.common.ViolationKeywordsService;
import com.qingchi.base.config.websocket.WebsocketServer;
import com.qingchi.base.constant.CommonConst;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.status.UserStatus;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.utils.QingLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class ScheduledTasks {

    @Resource
    private ViolationKeywordsService violationKeywordsService;

    @Resource
    private UserRepository userRepository;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.initialize();
        return taskScheduler;
    }

    @Scheduled(fixedRate = 3600000)
    public void queryFaceValueList() {
        Integer[] boysFaceValues = userRepository.queryUserFaceValueByBoy();
        Arrays.sort(boysFaceValues);
        AppConfigConst.setBoysFaceValues(boysFaceValues);
        Integer[] girlsFaceValues = userRepository.queryUserFaceValueByGirl();
        Arrays.sort(girlsFaceValues);
        AppConfigConst.setGirlsFaceValues(girlsFaceValues);
        //更新一下用户的在线状态，上次在线时间超过1小时，且还为在线的标识改为已下线
        Date curDate = new Date();
        Date oneHourBefore = new Date(curDate.getTime() - CommonConst.hour);
        Integer offLineNum = userRepository.updateUsersOnlineFlag(oneHourBefore);
        WebsocketServer.subOnlineCount(offLineNum);
    }

    //20分刷新一次
    @Scheduled(fixedRate = 1200000)
    public void queryIllegalWordDOS() {
        violationKeywordsService.refreshKeywords();
    }

    //10分刷新一次，用户解封
    @Scheduled(fixedRate = 600000)
    public void updateUserStatus() {
        Date curDate = new Date();
//        Integer count = userRepository.updateUserVioStatus(UserStatus.violation, UserStatus.enable, curDate);
        List<UserDO> users = userRepository.findByStatusAndViolationEndTimeBefore(UserStatus.violation, curDate);
        for (UserDO user : users) {
            user.setUpdateTime(curDate);
            user.setStatus(UserStatus.enable);
            userRepository.save(user);
        }
//        userRepository.saveAll(users);
//        redisUtil.del(WebsocketServer.onlineUsersCountKey);
        QingLogger.logger.info("今日时间{}，解封用户数量：{}", curDate, users.size());
    }

    //24小时执行一次，不对应该每天0点执行
    @Scheduled(cron = "0 0 0 * * *")
    public void updateUserVipFlag() {
        Date curDate = new Date();
        Integer count = userRepository.updateUserVipFlag(curDate);
        QingLogger.logger.info("今日时间{}，到期会员数量：{}", curDate, count);
    }
}