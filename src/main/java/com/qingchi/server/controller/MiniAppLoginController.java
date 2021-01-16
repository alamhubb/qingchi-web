package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.constant.PlatformType;
import com.qingchi.base.model.account.AccountDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.platform.weixin.WxDecode;
import com.qingchi.base.platform.weixin.login.AppLoginVO;
import com.qingchi.base.platform.weixin.login.LoginDataVO;
import com.qingchi.base.platform.weixin.login.LoginResult;
import com.qingchi.base.repository.user.AccountRepository;
import com.qingchi.base.repository.user.TokenRepository;
import com.qingchi.base.repository.log.UserLogRepository;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.service.UserService;
import com.qingchi.base.utils.JsonUtils;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.PhoneNumVO;
import com.qingchi.server.model.UserDetailVO;
import com.qingchi.server.service.AuthCodeService;
import com.qingchi.server.service.LoginService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author qinkaiyuan
 * @date 2019-02-17 14:14
 */
@RestController
@RequestMapping("user")
public class MiniAppLoginController {
    @Resource
    private TokenRepository tokenRepository;
    @Resource
    private AccountRepository accountRepository;

    @Resource
    private UserService userService;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private UserRepository userRepository;
    @Resource
    private UserLogRepository userLogRepository;
    @Resource
    private LoginService loginService;

    @Resource
    private AuthCodeService authCodeService;

    /**
     * 微信小程序界面点击绑定手机号触发
     * @param bindPhoneVO
     * @param user
     * @return
     */
    @PostMapping("bindPhoneNum")
    @ResponseBody
    public ResultVO<?> bindPhoneNum(@RequestBody LoginDataVO bindPhoneVO, UserDO user) {
        return wxBindPhoneNum(bindPhoneVO, user);
    }


    //微信绑定手机号方法
    private ResultVO<?> wxBindPhoneNum(LoginDataVO bindPhoneVO, UserDO user) {
        Boolean sessionEnable = bindPhoneVO.getSessionEnable();
        Optional<AccountDO> accountDOOptional = accountRepository.findOneByUserId(user.getId());
        if (!accountDOOptional.isPresent()) {
            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
        }
        AccountDO accountDO = accountDOOptional.get();
        Date curDate = new Date();
        String sessionKey = accountDO.getSessionKey();
        //如果过期不可用 或者 为空
        if (sessionEnable) {
            if (StringUtils.isEmpty(sessionKey)) {
                return new ResultVO<>(ErrorCode.CUSTOM_ERROR);
            }
        } else {
            //session未失效，从数据库中获取
            //session失效
            //校验各个参数
            String provider = bindPhoneVO.getProvider();
            if (StringUtils.isEmpty(provider)) {
                provider = bindPhoneVO.getLoginType();
            }

            if (StringUtils.isEmpty(bindPhoneVO.getCode())
                    || StringUtils.isEmpty(provider)
            ) {
                QingLogger.logger.error("触发了不应该出现的异常，入参为空");
                return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
            }
            //获取key
            ResultVO<LoginResult> resultVO = loginService.getMpPlatformInfo(provider, bindPhoneVO);
            if (resultVO.hasError()) {
                return resultVO;
            }
            LoginResult loginResult = resultVO.getData();
            sessionKey = loginResult.getSession_key();
            //更新数据库的key
            accountDO.setUserId(user.getId());
            accountDO.setUpdateTime(curDate);
            accountDO.setSessionKey(sessionKey);
            accountRepository.save(accountDO);
        }

        ResultVO<String> resultVO1 = new ResultVO<>();
        String phoneJson;
        try {
            phoneJson = WxDecode.decrypt(bindPhoneVO.getEncryptedData(), sessionKey, bindPhoneVO.getIv());
        } catch (Exception e) {
            resultVO1.setData("再点击一次绑定按钮即可完成绑定");
            return resultVO1;
        }
        if (StringUtils.isEmpty(phoneJson)) {
            QingLogger.logger.error("系统异常，不该走到这里");
            resultVO1.setData("再点击一次绑定按钮即可完成绑定");
            return resultVO1;
        }
        try {
            PhoneNumVO phoneNumVO = JsonUtils.objectMapper.readValue(phoneJson, PhoneNumVO.class);
            String phoneNum = phoneNumVO.getPurePhoneNumber();

            ResultVO<String> resultVO = authCodeService.verifyUserAndPhoneNumMatch(phoneNum, user);
            if (resultVO.hasError()) {
                return resultVO;
            }
            user.setPhoneCountryCode(phoneNumVO.getCountryCode());
            user.setPhoneNum(phoneNum);
            user.setUpdateTime(new Date());
            user = userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("user", new UserDetailVO(user, true));
        map.put("hint", "绑定手机号成功");
        return new ResultVO<>(map);
    }

    /**
     * 登录接口，登录后返回用户的token
     * toDO 还没有做百度和头条平台的适配
     *
     * @param loginVO
     * @return
     */
    @PostMapping("appLogin")
    public ResultVO<?> appLogin(@RequestBody @Valid AppLoginVO loginVO) {
        System.out.println(loginVO);
        return new ResultVO<>();
    }

    /**
     * 登录接口，登录后返回用户的token
     * toDO 还没有做百度和头条平台的适配
     *
     * @param loginVO
     * @return
     */
    @PostMapping("miniAppLogin")
    public ResultVO<?> miniAppLogin(@RequestBody LoginDataVO loginVO) {
        String provider = loginVO.getProvider();
        if (StringUtils.isEmpty(provider)) {
            provider = loginVO.getLoginType();
        }
        //需要手动赋值
        loginVO.setPlatform(PlatformType.mp);
        return loginService.mpPlatformLogin(provider, loginVO);
    }


    /**
     * 不在使用
     * 这个是手机号直接绑定的
     *
     * @param bindPhoneVO
     * @param user
     * @return
     */
    @PostMapping("bindPhoneNum2")
    @ResponseBody
    public ResultVO<?> bindPhoneNum2(@RequestBody LoginDataVO bindPhoneVO, UserDO user) {
        return wxBindPhoneNum(bindPhoneVO, user);
    }
}
