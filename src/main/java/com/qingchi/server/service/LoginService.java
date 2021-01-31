package com.qingchi.server.service;

import com.qingchi.base.config.ResultException;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.*;
import com.qingchi.base.constant.status.UserStatus;
import com.qingchi.base.model.account.AccountDO;
import com.qingchi.base.model.notify.NotifyDO;
import com.qingchi.base.model.user.TokenDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.modelVO.UnreadNotifyVO;
import com.qingchi.base.platform.baidu.BaiduConst;
import com.qingchi.base.platform.baidu.BaiduResult;
import com.qingchi.base.platform.baidu.BaiduUtil;
import com.qingchi.base.platform.qq.QQConst;
import com.qingchi.base.platform.toutiao.ToutiaoConst;
import com.qingchi.base.platform.weixin.WxConst;
import com.qingchi.base.platform.weixin.WxDecode;
import com.qingchi.base.platform.weixin.login.LoginDataVO;
import com.qingchi.base.platform.weixin.login.LoginResult;
import com.qingchi.base.repository.log.UserLogRepository;
import com.qingchi.base.repository.notify.NotifyRepository;
import com.qingchi.base.repository.user.AccountRepository;
import com.qingchi.base.repository.user.AuthenticationRepository;
import com.qingchi.base.repository.user.UserDistrictRepository;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.service.UserService;
import com.qingchi.base.store.TokenPlusRepository;
import com.qingchi.base.utils.*;
import com.qingchi.server.model.AppInitDataVO;
import com.qingchi.server.model.UserDetailVO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author qinkaiyuan
 * @date 2020-04-12 2:27
 */
@Service
public class LoginService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private AuthenticationRepository authRepository;
    @Resource
    private TokenPlusRepository tokenPlusRepository;
    @Resource
    private UserDistrictRepository userDistrictRepository;
    @Resource
    private ChatUserService chatUserService;
    @Resource
    private NotifyRepository notifyRepository;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private UserLogRepository userLogRepository;
    @Resource
    private LoginService loginService;
    @Resource
    private AccountRepository accountRepository;
    @Resource
    private UserService userService;
    @Resource
    private UserRepository userRepository;
    @Resource
    private AuthCodeService authCodeService;

    public AppInitDataVO getUserInitData(UserDO user, boolean isLogin) {
        AppInitDataVO appInitData = new AppInitDataVO();
        //是否为登陆接口，登陆接口返回token信息
        if (isLogin) {
            //生成userToken
            String userToken = TokenUtils.generateToken(user.getId());
            userToken = tokenPlusRepository.saveTokenDO(new TokenDO(userToken, user.getId()));
            appInitData.setTokenCode(userToken);
        }
        //user
        appInitData.setUser(new UserDetailVO(user, true));
        //notifies
        List<NotifyDO> notifyDOS = notifyRepository.findTop20ByReceiveUserIdAndTypeInOrderByIdDesc(user.getId(), NotifyType.comments);
        appInitData.setNotifies(UnreadNotifyVO.unreadNotifyDOToVOS(notifyDOS));
        //userDistricts
        /*List<DistrictVO> userDistrictVOS = DistrictVO.districtDOToVOS(districtDOS.stream()
                .map(userDistrictDO -> {
                    return DistrictUtils.get(userDistrictDO.getDistrictId());
                })
                .collect(Collectors.toList()));
        appInitData.setUserDistricts(userDistrictVOS);*/
        //chats
        List<ChatVO> chatVOs = chatUserService.getChats(user);
        appInitData.setChats(chatVOs);
        //微信订阅模板
        appInitData.setTalkTemplateIds(WxConst.talkTemplateIds);
        appInitData.setMessageTemplateIds(WxConst.messageTemplateIds);
        appInitData.setCommentTemplateIds(WxConst.commentTemplateIds);
        appInitData.setReportTemplateIds(WxConst.reportTemplateIds);

        //微信新版订阅模板
        appInitData.setWx_talkTemplateId(WxConst.talk_template_id);
        appInitData.setWx_commentTemplateId(WxConst.comment_template_id);
        appInitData.setWx_reportResultTemplateId(WxConst.report_result_template_id);
        appInitData.setWx_violationTemplateId(WxConst.violation_template_id);

        //qq订阅模板
        appInitData.setQq_talkTemplateId(QQConst.talk_template_id);
        appInitData.setQq_commentTemplateId(QQConst.comment_template_id);
        appInitData.setQq_reportResultTemplateId(QQConst.report_result_template_id);
        appInitData.setQq_violationTemplateId(QQConst.violation_template_id);
        return appInitData;
    }

    @Value("${config.qq.mp.qq_mp_id}")
    private String qq_mp_id;
    @Value("${config.qq.mp.qq_mp_secret}")
    private String qq_mp_secret;

    @Value("${config.wx.mp.wx_mp_id}")
    private String wx_mp_id;
    @Value("${config.wx.mp.wx_mp_secret}")
    private String wx_mp_secret;

    @Value("${config.tt.mp.tt_mp_id}")
    private String tt_mp_id;
    @Value("${config.tt.mp.tt_mp_secret}")
    private String tt_mp_secret;

    @Value("${config.bd.mp.bd_mp_client_id}")
    private String bd_mp_client_id;
    @Value("${config.bd.mp.bd_mp_client_secret}")
    private String bd_mp_client_secret;

    public ResultVO<LoginResult> getMpPlatformInfo(String provider, LoginDataVO loginVO) {
        //toDO 可以做一个http封装的库，让后台http调用和axios一样方便
        //登录的时候如果没有手机号，则手机号注册成功，自动注册一个user，用户名待填，自动生成一个昵称，密码待填，头像待上传
        //如果已经登录过，则返回那个已经注册的user，根据手机号获取user，返回登录成功
        //获取数据库的认证信息的验证码
        //获取openid
        String url;
        String jsCode = loginVO.getCode();
        //百度
        if (ProviderType.baidu.equals(provider)) {
            url = BaiduConst.baiduLoginUrl + "client_id=" + bd_mp_client_id + "&sk=" + bd_mp_client_secret + "&code=" + jsCode;
        } else if (ProviderType.qq.equals(provider)) {
            url = QQConst.qqLoginUrl + "appid=" + qq_mp_id + "&secret=" + qq_mp_secret + "&js_code=" + jsCode + "&grant_type=authorization_code";
        } else if (ProviderType.toutiao.equals(provider)) {
            url = ToutiaoConst.toutiaoLoginUrl + "appid=" + tt_mp_id + "&secret=" + tt_mp_secret + "&code=" + jsCode;
        } else {
            //微信
            url = WxConst.wxLoginUrl + "appid=" + wx_mp_id + "&secret=" + wx_mp_secret + "&js_code=" + jsCode + "&grant_type=authorization_code";
        }
        ResponseEntity<LoginResult> responseEntity = restTemplate.getForEntity(url, LoginResult.class);
        LoginResult wxLoginResult = responseEntity.getBody();
        assert wxLoginResult != null;
        if (wxLoginResult.hasError()) {
            return new ResultVO<>("登陆失败，" + ErrorMsg.CONTACT_SERVICE);
//            return new ResultVO<>(ErrorCode.BUSINESS_ERROR);
        }
        String openId = wxLoginResult.getOpenid();
        if (ProviderType.wx.equals(provider)) {
            //只有unionid为空才解析
            if (StringUtils.isEmpty(wxLoginResult.getUnionid())) {
                String enData = loginVO.getEncryptedData();
                String enIv = loginVO.getIv();
                //都不为空才解析unionid
                if (StringUtils.isNotEmpty(enData) && StringUtils.isNotEmpty(enIv)) {
                    String userInfoJson;
                    try {
                        userInfoJson = WxDecode.decrypt(enData, wxLoginResult.getSession_key(), enIv);
                    } catch (Exception e) {
                        logger.error("微信解析unionid出错1");
                        e.printStackTrace();
                        return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
                    }
                    if (StringUtils.isEmpty(userInfoJson)) {
                        logger.error("微信解析unionid出错2");
                        return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
                    }
                    try {
                        Map map = JsonUtils.objectMapper.readValue(userInfoJson, Map.class);
                        String enOpenId = (String) map.get("openId");
                        String enUnionId = (String) map.get("unionId");
                        if (StringUtils.isEmpty(enOpenId) || StringUtils.isEmpty(enUnionId)) {
                            logger.error("微信解析unionid出错3");
                            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
                        }
                        if (!enOpenId.equals(openId)) {
                            logger.error("微信解析unionid出错4");
                            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
                        }
                        wxLoginResult.setUnionid(enUnionId);
                    } catch (Exception e) {
                        logger.error("微信解析unionid出错3");
                        e.printStackTrace();
                        return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
                    }
                }
            }
        } else if (ProviderType.baidu.equals(provider)) {
            RestTemplate template = new RestTemplate();
            // 封装参数，千万不要替换为Map与HashMap，否则参数无法传递
            MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
            paramMap.add("openid", openId);
            // 2、使用postForEntity请求接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(paramMap, headers);
            String unionIdUrl = BaiduConst.baiduUnionIdUrl + BaiduUtil.getAccessToken();
            BaiduResult bdResult = template.postForEntity(unionIdUrl, httpEntity, BaiduResult.class).getBody();
            assert bdResult != null;
            if (bdResult.hasError()) {
                return new ResultVO<>(ErrorCode.BUSINESS_ERROR, bdResult.toString());
            }
            Map<String, String> map = bdResult.getData();
            openId = map.get("unionid");
            wxLoginResult.setOpenid(openId);
        }
        ResultVO<LoginResult> resultVO = new ResultVO<>();
        resultVO.setData(wxLoginResult);
        return resultVO;
    }


    public ResultVO<?> phoneLogin(LoginDataVO loginVO) {
        //所有平台，手机号登陆方式代码一致
        //登录的时候如果没有手机号，则手机号注册成功，自动注册一个user，用户名待填，自动生成一个昵称，密码待填，头像待上传
        //如果已经登录过，则返回那个已经注册的user，根据手机号获取user，返回登录成功
        //记录用户错误日志
        String phoneNum = loginVO.getPhoneNum();
        String authCode = loginVO.getAuthCode();
        //如果存在非数字
        //1.为空 2. 包含非数字 3.不为11位 ，返回
        if (StringUtils.isEmpty(phoneNum) || IntegerUtils.strHasNoNumber(phoneNum) || phoneNum.length() != 11) {
            logger.error("有人跳过前端，直接访问后台，错误手机号");
            return new ResultVO<>("请输入正确的手机号");
        }
        //校验验证码，传null用户记录日志
        ResultVO<String> resultVO = authCodeService.verifyAuthCode(phoneNum, authCode, null);
        if (resultVO.hasError()) {
            return resultVO;
        }
        //如果手机号已经存在账户，则直接使用，正序获取第一个用户
        Optional<UserDO> userDOOptional = userRepository.findFirstByPhoneNumOrderByIdAsc(phoneNum);
        UserDO dbUser;
        //有用户返回，没有创建
        String platform = loginVO.getPlatform();
        if (userDOOptional.isPresent()) {
            dbUser = userDOOptional.get();
        } else {
            dbUser = userService.createUser(phoneNum, platform);
            AccountDO accountDO = new AccountDO(platform, loginVO.getProvider(), dbUser.getId());
            accountRepository.save(accountDO);
        }
        //则更新用户手机号
        return new ResultVO<>(loginService.getUserInitData(dbUser, true));
    }


    public ResultVO<?> platformLogin(LoginDataVO loginVO, String provider, LoginResult loginResult, String mpOpenId, String unionId, Optional<AccountDO> optionalAccount) {
        String platform = loginVO.getPlatform();
        AccountDO accountDO;
        UserDO dbUser;
        Date curDate = new Date();
        //如果已经注册过
        if (optionalAccount.isPresent()) {
            accountDO = optionalAccount.get();
            dbUser = UserUtils.get(accountDO.getUserId());
            //如果账号被封禁则报错
            if (UserStatus.violation.equals(dbUser.getStatus())) {
                //账号被封禁，多少天后解封
                return new ResultVO<>(ErrorMsgUtil.getErrorCode605ContactServiceValue(dbUser.getViolationEndTime()));
            }
            //否则就会走最下面
        } else {
            //处理性别
            GenderTypeEnum genderTypeEnum = GenderTypeEnum.enumOf(loginVO.getGender());
            String genderName;
            if (genderTypeEnum != null) {
                genderName = genderTypeEnum.getName();
            } else {
                genderName = GenderTypeEnum.female.getName();
            }
            //收到验证码的手机号，和邀请码用户
            if (PlatformType.mp.equals(platform)) {
                dbUser = userService.createUser(loginVO.getNickName(), loginVO.getAvatarUrl(), genderName, loginVO.getCity(), platform);
            } else {
                if (provider.equals(ProviderType.qq)) {
                    String birthday = null;
                    if (StringUtils.isNotEmpty(loginVO.getBirthday())) {
                        birthday = DateUtils.yearStringToDate(loginVO.getBirthday());
                    }
                    dbUser = userService.createUser(loginVO.getNickName(), loginVO.getAvatarUrl(), genderName, birthday, loginVO.getCity(), platform);
                } else {
                    dbUser = userService.createUser(loginVO.getNickName(), loginVO.getAvatarUrl(), genderName, loginVO.getCity(), platform);
                }
            }
            accountDO = new AccountDO(platform, provider, dbUser.getId());
        }
        //更新account信息,登陆方式
        if (ProviderType.qq.equals(provider)) {
            if (PlatformType.mp.equals(platform)) {
                accountDO.setQqMpOpenId(mpOpenId);
            } else {
                accountDO.setQqAppOpenId(mpOpenId);
            }
            accountDO.setQqUnionId(unionId);
        } else {
            if (PlatformType.mp.equals(platform)) {
                accountDO.setWxMpOpenId(mpOpenId);
                //微信时支持绑手机号使用
                accountDO.setSessionKey(loginResult.getSession_key());
            } else {
                accountDO.setWxAppOpenId(mpOpenId);
            }
            accountDO.setWxUnionId(unionId);
        }
        //最后一次登陆的渠道，还有登陆方式,每次登陆都更新渠道信息
        accountDO.setPlatform(platform);
        accountDO.setProvider(provider);
        accountDO.setClientid(loginVO.getClientid());
        accountDO.setUpdateTime(curDate);
        dbUser.setPlatform(platform);
        //只有微信小程序才会赋值
        accountRepository.save(accountDO);
        return new ResultVO<>(loginService.getUserInitData(dbUser, true));
    }

    public ResultVO<?> mpPlatformLogin(String provider, LoginDataVO loginVO) {
        ResultVO<LoginResult> resultVO = loginService.getMpPlatformInfo(provider, loginVO);
        if (resultVO.hasError()) {
            return resultVO;
        }
        //用来查询是否已注册与平台相关
        LoginResult loginResult = resultVO.getData();
        String mpOpenId = loginResult.getOpenid();
        String unionId = loginResult.getUnionid();
        //然后判断此用户是否注册过
        //注册过，获取已注册的用户信息
        //未注册生成注册
        Optional<AccountDO> optionalAccount = Optional.empty();
        //qq平台
        if (ProviderType.qq.equals(provider)) {
            if (StringUtils.isNotEmpty(unionId)) {
                optionalAccount = accountRepository.findFirstOneByQqUnionIdOrderByIdAsc(unionId);
            }
            if (!optionalAccount.isPresent()) {
                optionalAccount = accountRepository.findFirstOneByQqMpOpenIdOrderByIdAsc(mpOpenId);
            }
            //wx平台
        } else if (ProviderType.wx.equals(provider)) {
            //unionId查询
            if (StringUtils.isNotEmpty(unionId)) {
                optionalAccount = accountRepository.findFirstOneByWxUnionIdOrderByIdAsc(unionId);
            }
            //没有的话openId查询
            if (!optionalAccount.isPresent()) {
                optionalAccount = accountRepository.findFirstOneByWxMpOpenIdOrderByIdAsc(mpOpenId);
            }
        } else {
            QingLogger.logger.error("错误的第三方登陆类型");
            throw new ResultException("错误的第三方登陆类型");
        }
        return loginService.platformLogin(loginVO, provider, loginResult, mpOpenId, unionId, optionalAccount);
    }
}
