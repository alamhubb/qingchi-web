package com.qingchi.server.service;

import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonConst;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ErrorMsg;
import com.qingchi.base.constant.status.UserStatus;
import com.qingchi.base.model.account.AuthenticationDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.UserLogDO;
import com.qingchi.base.repository.user.AuthenticationRepository;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.utils.AuthCode;
import com.qingchi.base.utils.ErrorMsgUtil;
import com.qingchi.base.utils.IntegerUtils;
import com.qingchi.base.utils.IpUtil;
import com.qingchi.server.store.UserLogStoreUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

/**
 * @author qinkaiyuan
 * @date 2019-02-17 14:14
 */
@Service
public class AuthCodeService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private AuthenticationRepository authRepository;
    @Resource
    private UserRepository userRepository;

    @Value("${config.qq.sms.appId}")
    private int appId;
    @Value("${config.qq.sms.appKey}")
    private String appKey;
    @Value("${config.qq.sms.templateId}")
    private int templateId;
    @Value("${config.qq.sms.smsSign}")
    private String smsSign;

    public ResultVO<String> verifyPhoneNum(String phoneNum, UserDO user) {
        if (IntegerUtils.strHasNoNumber(phoneNum)) {
            return new ResultVO<>("请输入正确的手机号");
        }
        if (user != null) {
            //判断用户是否已绑定手机号
            if (StringUtils.isNotEmpty((user.getPhoneNum()))) {
                UserLogStoreUtils.save(new UserLogDO("您已绑定手机号，不可重复绑定", user, phoneNum));
                logger.warn("您已绑定手机号，不可重复绑定：{}", user.getId());
                return new ResultVO<>("您已绑定手机号，不可重复绑定");
            }
        }
        //校验手机号是否已被使用
        Optional<UserDO> userDOOptional = userRepository.findFirstByPhoneNumOrderByIdAsc(phoneNum);
        if (userDOOptional.isPresent()) {
            UserDO userDO = userDOOptional.get();
            if (userDO.getStatus().equals(UserStatus.violation)) {
                return new ResultVO<>(ErrorMsgUtil.getErrorCode605ContactServiceValue(userDO.getViolationEndTime()));
            }
        }
        return new ResultVO<>();
    }


    public ResultVO<String> verifyAuthCode(String phoneNum, String authCode, UserDO user) {
        //1.为空 2. 包含非数字 3.不为4位 ，返回
        if (StringUtils.isEmpty(authCode) || IntegerUtils.strHasNoNumber(authCode) || authCode.length() != 4) {
            logger.error("有人跳过前端，直接访问后台，错误的验证码");
            return new ResultVO<>("请输入正确的验证码");
        }
        //获取数据库的认证信息的验证码
        Optional<AuthenticationDO> authenticationDOOptional = authRepository.findFirstByPhoneNumOrderByCreateTimeDescIdAsc(phoneNum);
        //如果发送过验证码
        if (authenticationDOOptional.isPresent()) {
            //校验验证码是否过期，如果当前时间晚于30分钟后则验证码失效
            AuthenticationDO authenticationDO = authenticationDOOptional.get();
            Integer authCodeValidMinute = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.authCodeValidMinuteKey);
            long canTime = authenticationDO.getCreateTime().getTime() + authCodeValidMinute * CommonConst.minute;
            long curTime = new Date().getTime();
            if (curTime > canTime) {
                logger.warn("验证码超时：{}", authCode);
                UserLogStoreUtils.save(new UserLogDO("验证码超时", user, phoneNum, authCode));
                return new ResultVO<>("验证码超时，请重新获取");
            }
            //用户输入的验证码和数据库的一致
            if (authCode.equals(authenticationDO.getAuthCode())) {
                return new ResultVO<>();
            } else {
                //验证码错误，提示
                logger.info("验证码错误");
                return new ResultVO<>("验证码错误");
            }
        } else {
            logger.info("没有验证码记录，未发送过验证码，此手机号没有对应的验证码记录");
            return new ResultVO<>("此手机号未发送过验证码");
        }
    }

    public ResultVO<String> sendAuthCodeHandle(String phoneNum, UserDO user, HttpServletRequest request) {
        //要防的是同1个ip无线刷验证码
        //发送验证码时要记录ip，记录用户id，记录请求内容
        //限制手机号，同1手机号做多2条，
        String userIp = IpUtil.getIpAddr(request);
        //h5登录也需要防止
        if (StringUtils.isEmpty(userIp)) {
            logger.error("获取不到ip信息");
            UserLogStoreUtils.save(new UserLogDO("获取不到用户ip信息", user, phoneNum));
            return new ResultVO<>("用户信息错误，无法发送验证码");
        }

        ResultVO<String> resultVO = verifyPhoneNum(phoneNum, user);
        if (resultVO.hasError()) {
            return resultVO;
        }

        //然后查ip总次数，大于2也不行
        //然后查userId 大于2也不行
        //时间，必须大于30秒，同一个手机号
        //获取数据库的认证信息的验证码,校验时间10分钟可以发一次验证码
        Optional<AuthenticationDO> authenticationDOOptional = authRepository.findFirstByPhoneNumOrderByCreateTimeDescIdAsc(phoneNum);
        if (authenticationDOOptional.isPresent()) {
            AuthenticationDO authenticationDO = authenticationDOOptional.get();
            Date lastDate = authenticationDO.getCreateTime();
            Integer authCodeInterval = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.authCodeIntervalKey);
            long canDate = lastDate.getTime() + authCodeInterval * CommonConst.second;
            long curDate = new Date().getTime();
            if (curDate < canDate) {
                return new ResultVO<>("获取验证码过于频繁，请稍候重试");
            }
        }

        final Integer userLimitCount = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.authCodeCountKey);
        final Integer ipLimitCount = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.authCodeIpCountKey);
        final Integer phoneLimitCount = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.authCodePhoneCountKey);
        //首先查手机号总次数，如果大于1，则不行
        Integer phoneNumCount = authRepository.countByPhoneNum(phoneNum);

        if (user != null) {
            Integer userCount = authRepository.countByUserId(user.getId());
            if (userCount >= userLimitCount) {
                UserLogStoreUtils.save(new UserLogDO("用户获取达到获取验证码次数上限", user, phoneNum));
                return new ResultVO<>("获取验证码次数已达到上线，" + ErrorMsg.CONTACT_SERVICE);
            }
        }
        Integer ipCount = authRepository.countByIp(userIp);
        if (phoneNumCount >= phoneLimitCount || ipCount >= ipLimitCount) {
            if (phoneNumCount >= phoneLimitCount) {
                UserLogStoreUtils.save(new UserLogDO("手机号获取达到获取验证码次数上限", user, phoneNum));
            } else {
                UserLogStoreUtils.save(new UserLogDO("用户IP获取达到获取验证码次数上限", user, phoneNum));
            }
            return new ResultVO<>("获取验证码次数已达到上线，" + ErrorMsg.CONTACT_SERVICE);
        }
        return cosSendAuthCode(phoneNum, user, userIp);
    }

    private ResultVO<String> cosSendAuthCode(String phoneNum, UserDO user, String userIp) {
        String authCode = AuthCode.getAuthCode();
        String authCodeValidTime = ((Integer) AppConfigConst.appConfigMap.get(AppConfigConst.authCodeValidMinuteKey)).toString();
        //多少分钟内有效
        String[] params = {authCode, authCodeValidTime};
        SmsSingleSender ssender = new SmsSingleSender(appId, appKey);
        // 签名
        SmsSingleSenderResult result = null;  // 签名参数未提供或者为空时，会使用默认签名发送短信
        try {
            result = ssender.sendWithParam("86", phoneNum,
                    templateId, params, smsSign, "", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        AuthenticationDO authenticationDO = new AuthenticationDO(user, phoneNum, authCode, userIp);
        /*authenticationDO.setStatus(CommonStatus.success);
        authRepository.save(authenticationDO);
        return new ResultVO<>();*/
        if (result != null && result.result == 0) {
            authenticationDO.setStatus(CommonStatus.success);
            authRepository.save(authenticationDO);
            return new ResultVO<>();
        } else {
            authenticationDO.setStatus(CommonStatus.fail);
            authRepository.save(authenticationDO);
            return new ResultVO<>("验证码发送失败，请稍候重试，" + ErrorMsg.CONTACT_SERVICE);
        }
    }
}
