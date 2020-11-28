package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ProviderType;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.platform.weixin.login.LoginDataVO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.server.model.BindPhoneVO;
import com.qingchi.server.model.UserDetailVO;
import com.qingchi.server.service.AuthCodeService;
import com.qingchi.server.service.LoginService;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author qinkaiyuan
 * @date 2019-02-17 14:14
 */
@RestController
@RequestMapping("phone")
public class AppLoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private UserRepository userRepository;
    @Resource
    private LoginService loginService;
    @Resource
    private AuthCodeService authCodeService;

    @PostMapping("appLogin")
    @ResponseBody
    public ResultVO<?> appLogin(@RequestBody @Valid @NotNull LoginDataVO loginVO) {
        //登录的时候如果没有手机号，则手机号注册成功，自动注册一个user，用户名待填，自动生成一个昵称，密码待填，头像待上传
        //如果已经登录过，则返回那个已经注册的user，根据手机号获取user，返回登录成功
        //记录用户错误日志
        loginVO.setProvider(ProviderType.phone);
        return loginService.phoneLogin(loginVO);
    }

    /**
     * 如果填写了验证码
     * 1.如果用户已经注册，非新用户，//登录成功，不注册，提示603错误
     * 2.如果未注册，
     * 判断邀请码是否正确，错误，提示邀请码错误，请重新填写注册
     * 3.注册，赠送一个月会员 同4
     * 是否有邀请码，有邀请码额外赠送正确，
     * 4新用户新增一个会员订单，开通会员，设置是否会员，是否年会员，设置所有属性
     * 5.邀请人用户，判断是否已是年会员，是
     * 赠送清池币
     * 不是
     * 赠送会员
     *
     * @param bindPhoneVO
     * @return
     */
    @PostMapping("bindPhoneNum")
    @ResponseBody
    public ResultVO<?> bindPhoneNum(@RequestBody @Valid @NotNull BindPhoneVO bindPhoneVO, UserDO user) {
        //登录的时候如果没有手机号，则手机号注册成功，自动注册一个user，用户名待填，自动生成一个昵称，密码待填，头像待上传
        //如果已经登录过，则返回那个已经注册的user，根据手机号获取user，返回登录成功
        //记录用户错误日志
        String phoneNum = bindPhoneVO.getPhoneNum();
        ResultVO<String> resultVO = authCodeService.verifyPhoneNum(phoneNum, user);
        if (resultVO.hasError()) {
            return resultVO;
        }
        String authCode = bindPhoneVO.getAuthCode();
        resultVO = authCodeService.verifyAuthCode(phoneNum, authCode, user);
        if (resultVO.hasError()) {
            return resultVO;
        }
        //则更新用户手机号
        user.setPhoneCountryCode("86");
        user.setPhoneNum(phoneNum);
        user.setUpdateTime(new Date());
        user = userRepository.save(user);
        return new ResultVO<>(new UserDetailVO(user, true));
    }

    /**
     * 腾讯云手机验证码相关
     *
     * @param phoneNum
     * @return
     * @throws Exception
     */
    @PostMapping("sendAuthCode")
    @ResponseBody
    public ResultVO<String> sendAuthCode(@Valid @NotBlank @Length(min = 11, max = 11) String phoneNum, UserDO user, HttpServletRequest request) {
        return authCodeService.sendAuthCodeHandle(phoneNum, user, request);
    }

}
