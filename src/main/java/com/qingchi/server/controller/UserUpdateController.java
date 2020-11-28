package com.qingchi.server.controller;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ErrorMsg;
import com.qingchi.base.constant.GenderType;
import com.qingchi.base.platform.tencent.TencentCloud;
import com.qingchi.base.platform.weixin.HttpResult;
import com.qingchi.base.platform.weixin.WxUtil;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.service.UserService;
import com.qingchi.base.model.user.UserDetailDO;
import com.qingchi.base.repository.user.UserDetailRepository;
import com.qingchi.base.repository.user.UserImgRepository;
import com.qingchi.base.utils.AgeUtils;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.UserDetailVO;
import com.qingchi.server.model.UserEditVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 查询用户详情
 *
 * @author qinkaiyuan
 * @since 1.0.0
 */
@RestController
@RequestMapping("user")
public class UserUpdateController {
    @Resource
    private UserRepository userRepository;
    @Resource
    private UserDetailRepository userDetailRepository;
    @Resource
    private EntityManager entityManager;
    @Resource
    private UserImgRepository userImgRepository;
    @Resource
    private UserService userService;

    @PostMapping("updateAvatar")
    public ResultVO<?> updateAvatar(@Valid @NotNull String avatar, UserDO user) {
        //被举报状态的用户不能修改
        user.setAvatar("https://" + avatar);
        user.setUpdateTime(new Date());
        userRepository.save(user);
        return new ResultVO<>();
    }

    @PostMapping("edit")
    public ResultVO<UserDetailVO> editUser(UserDO user, @RequestBody @Valid UserEditVO userEditVO) {
        //昵称
        String nickname = userEditVO.getNickname();
        if (StringUtils.isEmpty(nickname)) {
            return new ResultVO<>("昵称不能为空");
        }
        if (nickname.length() > 6) {
            return new ResultVO<>("昵称长度不能大于6");
        } else {
            if (checkHasIllegals(nickname)) return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
            if (TencentCloud.textIsViolation(nickname)) {
                return new ResultVO<>("昵称包含违规内容，禁止修改，请修改后重试");
            }
            HttpResult wxResult = WxUtil.checkContentWxSec(nickname);
            if (wxResult.hasError()) {
                return new ResultVO<>("昵称包含违规内容，禁止修改，请修改后重试");
            }
            user.setNickname(StringUtils.substring(nickname, 0, 6));
        }
        //性别
        if (GenderType.genders.contains(userEditVO.getGender())) {
            user.setGender(userEditVO.getGender());
        } else {
            QingLogger.logger.warn("错误的性别类型");
        }
        //生日，年龄
        String birthday = userEditVO.getBirthday();
        if (StringUtils.isNotEmpty(birthday)) {
            user.setBirthday(birthday);
            user.setAge(AgeUtils.getAgeByBirth(birthday));
        }
        //保存地区名
        if (StringUtils.isNotEmpty(userEditVO.getLocation())) {
            if (userEditVO.getLocation().length() > 10) {
                return new ResultVO<>("市县区名称长度不能大于10");
            } else {
                String userLocation = userEditVO.getLocation();
                if (checkHasIllegals(userLocation)) return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                if (TencentCloud.textIsViolation(userLocation)) {
                    return new ResultVO<>("地区名称违规");
                }
                HttpResult wxResult = WxUtil.checkContentWxSec(userLocation);
                if (wxResult.hasError()) {
                    return new ResultVO<>("地区名称违规");
                }
                user.setLocation(userEditVO.getLocation());
            }
        }
        //保存用户详情相关，联系方式
        Optional<UserDetailDO> optionalUserDetailDO = userDetailRepository.findFirstOneByUserId(user.getId());
        UserDetailDO userDetailDO;
        userDetailDO = optionalUserDetailDO.orElseGet(() -> new UserDetailDO(user));
        String contactAccount = userEditVO.getContactAccount();
        String wxAccount = userEditVO.getWxAccount();
        String qqAccount = userEditVO.getQqAccount();
        String wbAccount = userEditVO.getWbAccount();
        //代表老版本
        if (contactAccount == null) {
            if (wxAccount != null) {
                if (StringUtils.isNotEmpty(wxAccount)) {
                    if (wxAccount.length() > 23) {
                        return new ResultVO<>("微信账户名不得超过23个字符，例如：491369310");
                    } else if (wxAccount.length() < 6) {
                        return new ResultVO<>("微信账户名必须大于5个字符，例如：491369310");
                    } else {
                        if (checkHasIllegals(wxAccount)) {
                            return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                        }
                        if (TencentCloud.textIsViolation(wxAccount)) {
                            return new ResultVO<>("微信账户违规");
                        }
                        HttpResult wxResult = WxUtil.checkContentWxSec(wxAccount);
                        if (wxResult.hasError()) {
                            return new ResultVO<>("微信账户违规");
                        }
                    }
                }
                userDetailDO.setWxAccount(wxAccount);
            }
            if (qqAccount != null) {
                if (StringUtils.isNotEmpty(qqAccount)) {
                    if (qqAccount.length() > 15) {
                        return new ResultVO<>("qq号不得超过15个字符，例如：491369310");
                    } else if (qqAccount.length() < 5) {
                        return new ResultVO<>("qq号必须大于4个字符，例如：491369310");
                    } else {
                        if (checkHasIllegals(qqAccount)) {
                            return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                        }
                        if (TencentCloud.textIsViolation(qqAccount)) {
                            return new ResultVO<>("qq号违规");
                        }
                        HttpResult wxResult = WxUtil.checkContentWxSec(qqAccount);
                        if (wxResult.hasError()) {
                            return new ResultVO<>("qq号违规");
                        }
                    }
                    userDetailDO.setQqAccount(qqAccount);
                }
            }
            if (wbAccount != null) {
                if (StringUtils.isNotEmpty(wbAccount)) {
                    if (wbAccount.length() > 20) {
                        return new ResultVO<>("微博名称不得超过20个字符，例如：清池");
                    } else if (wbAccount.length() < 2) {
                        return new ResultVO<>("微博名称必须大于1个字符，例如：清池");
                    } else {
                        if (checkHasIllegals(wbAccount)) {
                            return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                        }
                        if (TencentCloud.textIsViolation(wbAccount)) {
                            return new ResultVO<>("微博名称违规");
                        }
                        HttpResult wxResult = WxUtil.checkContentWxSec(wbAccount);
                        if (wxResult.hasError()) {
                            return new ResultVO<>("微博名称违规");
                        }
                    }
                }
                userDetailDO.setWbAccount(wbAccount);
            }
            //老版本将其他的联系方式放置到联系方式上
            //如果微信不为空，且长度大于5
            if (StringUtils.isNotEmpty(wxAccount)) {
                contactAccount = "wx:" + wxAccount;
            } else if (StringUtils.isNotEmpty(qqAccount)) {
                contactAccount = "qq:" + qqAccount;
            } else if (StringUtils.isNotEmpty(wbAccount)) {
                contactAccount = "wb:" + wbAccount;
            } else {
                contactAccount = "";
            }
        } else {
            //新版本
            if (StringUtils.isNotEmpty(contactAccount)) {
                if (contactAccount.length() > 30) {
                    return new ResultVO<>("联系方式不能超过30个字符，例如：vx:491369310");
                } else if (contactAccount.length() < 5) {
                    return new ResultVO<>("联系方式必须大于4个字符，例如：vx:491369310");
                } else {
                    if (checkHasIllegals(contactAccount))
                        return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                    if (TencentCloud.textIsViolation(contactAccount)) {
                        return new ResultVO<>("联系方式违规");
                    }
                    HttpResult wxResult = WxUtil.checkContentWxSec(contactAccount);
                    if (wxResult.hasError()) {
                        return new ResultVO<>("联系方式违规");
                    }
                }
            }
        }
        userDetailDO.setContactAccount(contactAccount);
        userDetailRepository.save(userDetailDO);
        user.setUpdateTime(new Date());
        return new ResultVO<>(new UserDetailVO(userRepository.save(user), true));
    }

    public static boolean checkHasIllegals(String content) {
        List<String> illegals = AppConfigConst.illegals;
        for (String illegal : illegals) {
            if (StringUtils.isNotEmpty(illegal) && StringUtils.containsIgnoreCase(content, illegal)) {
                QingLogger.logger.info("发布了涉污动态，关键词：{}，内容：{}", illegal, content);
                return true;
            }
        }
        return false;
    }
}