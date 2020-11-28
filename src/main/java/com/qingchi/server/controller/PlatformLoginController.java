package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.constant.ProviderType;
import com.qingchi.base.constant.PlatformType;
import com.qingchi.base.platform.qq.QQConst;
import com.qingchi.base.platform.weixin.login.LoginDataVO;
import com.qingchi.base.platform.weixin.login.LoginResult;
import com.qingchi.base.model.account.AccountDO;
import com.qingchi.base.repository.user.AccountRepository;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.text.MessageFormat;
import java.util.Optional;

@RestController
@RequestMapping("user")
public class PlatformLoginController {
    @Resource
    private LoginService loginService;
    @Resource
    private AccountRepository accountRepository;
    @Resource
    private RestTemplate restTemplate;

    @PostMapping("platformLogin")
    public ResultVO<?> platformLogin(@RequestBody @Valid LoginDataVO loginVO) {
        String provider = loginVO.getProvider();
        if (org.apache.commons.lang.StringUtils.isEmpty(provider)) {
            provider = loginVO.getLoginType();
        }

        if (ProviderType.phone.equals(provider)) {
            return loginService.phoneLogin(loginVO);
        } else {
            String platform = loginVO.getPlatform();
            //非手机号登录
            // 区分
            //如果是app
            if (PlatformType.mp.equals(platform)) {
                //根据相关信息获取open，uni等id
                return loginService.mpPlatformLogin(provider, loginVO);
            } else {
                String openId;
                String unionId;
                if (ProviderType.qq.equals(provider)) {
                    String accessToken = loginVO.getAccessToken();
                    ResponseEntity<LoginResult> responseEntity = restTemplate.getForEntity(MessageFormat.format(QQConst.qq_app_unionId_url, accessToken), LoginResult.class);
                    LoginResult wxLoginResult = responseEntity.getBody();
                    if (ObjectUtils.isEmpty(wxLoginResult) || wxLoginResult.hasError()) {
                        return new ResultVO<>(ErrorCode.BUSINESS_ERROR);
                    }
                    //如果是app平台
                    openId = wxLoginResult.getOpenid();
                    unionId = wxLoginResult.getUnionid();
                } else if (ProviderType.wx.equals(provider)) {
                    openId = loginVO.getOpenId();
                    unionId = loginVO.getUnionId();
                } else {
                    QingLogger.logger.error("有人异常访问登陆，错误的登录类型");
                    return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
                }
                if (StringUtils.isEmpty(openId) || StringUtils.isEmpty(unionId)) {
                    QingLogger.logger.error("有人异常访问登陆，错误的登录类型");
                    return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
                }
                Optional<AccountDO> optionalAccount;
                //app 平台必有 unionid，没有就是没登录
                if (ProviderType.qq.equals(provider)) {
                    optionalAccount = accountRepository.findFirstOneByQqUnionIdOrderByIdAsc(unionId);
                    //wx平台
                } else {
                    //unionId查询
                    optionalAccount = accountRepository.findFirstOneByWxUnionIdOrderByIdAsc(unionId);
                }
                return loginService.platformLogin(loginVO, provider, null, openId, unionId, optionalAccount);
                //如果是小程序
            }
        }
    }
}
