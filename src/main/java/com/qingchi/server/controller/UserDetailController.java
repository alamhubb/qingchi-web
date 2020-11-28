package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonConst;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.constant.ErrorMsg;
import com.qingchi.base.entity.UserImgUtils;
import com.qingchi.base.model.user.*;
import com.qingchi.base.platform.tencent.TencentCloud;
import com.qingchi.base.repository.shell.UserContactRepository;
import com.qingchi.base.repository.user.*;
import com.qingchi.base.service.UserService;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.*;
import com.qingchi.server.service.ShellOrderService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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
public class UserDetailController {

    @Resource
    private UserRepository userRepository;
    @Resource
    private UserDetailRepository userDetailRepository;
    @Resource
    private EntityManager entityManager;
    @Resource
    private UserImgRepository userImgRepository;
    @Resource
    private IdentityImgRepository identityImgRepository;
    @Resource
    private UserService userService;
    @Value("${config.qq.cos.imgUrl}")
    private String imgUrl;

    /**
     * 前台判断，用户是否安装成功过一次，没有的话判断用户现在是不是桌面启动，
     * 是桌面启动则给予一次赠送会员
     * <p>
     * <p>
     * <p>
     * 如果用户触发了 安装送会员功能
     *
     * @param user
     * @return
     */

    @PostMapping("mine")
    public ResultVO<UserDetailVO> getUserMine(UserDO user) {
        return new ResultVO<>(new UserDetailVO(user, true));
    }

    @PostMapping("queryUserDetail")
    public ResultVO<UserDetailVO> queryUserDetail(UserDO user, @RequestBody @Valid UserQueryVO queryVO) {
        if (user != null && user.getId().equals(queryVO.getUserId())) {
            return new ResultVO<>(new UserDetailVO(user, true));
        } else {
            Optional<UserDO> userOptional = userRepository.findById(queryVO.getUserId());
            if (userOptional.isPresent()) {
                UserDO userDO = userOptional.get();
                return new ResultVO<>(new UserDetailVO(userDO, user));
            }
        }
        return new ResultVO<>("无法查看不存在的用户");
    }

    /*@PostMapping("setFilter")
    public ResultVO<?> setFilter(@RequestBody SetUserFilterVO setVO, UserDO user) {
        Optional<UserDetailDO> userDetailDOOptional = userDetailRepository.findFirstOneByUser(user);
        if (userDetailDOOptional.isPresent()) {
            UserDetailDO userDetailDO = userDetailDOOptional.get();
            String gender = setVO.getGender();
            if (GenderType.allGenders.contains(gender)) {
                userDetailDO.setGender(gender);
            } else {
                Logger.logger.error("有人传入了错误的参数");
                return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
            }
            userDetailDO.setMinAge(setVO.getMinAge());
            userDetailDO.setMaxAge(setVO.getMaxAge());
            userDetailRepository.save(userDetailDO);
            return new ResultVO<>();
        }
        return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
    }*/

    @PostMapping("addImg")
    public ResultVO<UserDetailVO> addImg(UserDO user, @RequestBody @Valid UserImgVO img) {
        if (!CommonStatus.normal.equals(user.getStatus())) {
            return new ResultVO<>(ErrorMsg.userMaybeViolation);
        }
        List<UserImgDO> userImgDOS = UserImgUtils.getImgs(user.getId());
        if (userImgDOS.size() > 2) {
            return new ResultVO<>("最多上传3张照片，请删除后继续！");
        }
        UserImgDO userImgDO = img.toUserImgDO(user, imgUrl);
        //如果用户已认证，则上传的照片必须为已认证的
        if (user.getIsSelfAuth()) {
            Optional<IdentityImgDO> optionalIdentityImgDO = identityImgRepository.findFirstByUserIdAndStatusOrderByIdDesc(user.getId(), CommonStatus.normal);
            if (optionalIdentityImgDO.isPresent()) {
                IdentityImgDO identityImgDO = optionalIdentityImgDO.get();
                boolean isAuth = TencentCloud.imgAuth(userImgDO.getSrc(), identityImgDO.getSrc());
                //不为本人，则提示，为本人则继续下面的逻辑
                if (!isAuth) {
                    return new ResultVO<>("用户认证后，只能上传与认证信息相符的本人清晰露脸的真实照片");
                }
            } else {
                QingLogger.logger.error("系统异常，不应该出现已认证却没有记录的情况");
                return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
            }
        }
        userRepository.save(user);
        userImgRepository.save(userImgDO);
        entityManager.clear();
        UserDO userDO = userRepository.getOne(user.getId());
        return new ResultVO<>(new UserDetailVO(userDO, true));
    }

    @PostMapping("idCard/add")
    public ResultVO<UserDetailVO> addIdCard(UserDO user, @RequestBody IdCardVO idCard) {
        if (StringUtils.isEmpty(user.getIdCardStatus()) || user.getIdCardStatus().equals(CommonStatus.init)) {
            UserDO userDO = userService.addIdCard(user, idCard.toDO());
            return new ResultVO<>(new UserDetailVO(userDO, true));
        }
        return new ResultVO<>("已经提交过认证申请，暂无法再次提交!");
    }

    @PostMapping("deleteImg")
    public ResultVO<UserDetailVO> deleteImg(UserDO user, @RequestBody UserImgDeleteVO img) {
        if (!CommonStatus.normal.equals(user.getStatus())) {
            return new ResultVO<>(ErrorMsg.userMaybeViolation);
        }
        List<UserImgDO> userImgDOS = UserImgUtils.getImgs(user.getId());
        if (userImgDOS.size() > 1) {
            Optional<UserImgDO> userImgDOOptional = userImgRepository.getUserImgByUserIdAndId(user.getId(), img.getId());
            if (userImgDOOptional.isPresent()) {
                UserImgDO userImg = userImgDOOptional.get();
                userImg.setStatus(CommonStatus.delete);
                userImg.setUpdateTime(new Date());
                userImgRepository.save(userImg);
            }
            entityManager.clear();
            UserDO userDO = userRepository.getOne(user.getId());
            return new ResultVO<>(new UserDetailVO(userDO, true));
        } else {
            return new ResultVO<>("请至少保留一张照片");
        }
    }

    @Resource
    private ShellOrderService shellOrderService;
    @Resource
    private UserContactRepository userContactRepository;

    @PostMapping("getUserContact")
    public ResultVO<String> getUserContact(UserDO user, @RequestBody @Valid @NotNull UserQueryVO queryVO) {
        Integer userShell = user.getShell();
        if (userShell < 10) {
            QingLogger.logger.error("系统被攻击，不该触发这里，用户不够10贝壳");
            return new ResultVO<>(ErrorCode.CUSTOM_ERROR);
        }
        Integer userId = queryVO.getUserId();
        Optional<UserDO> userDOOptional = userRepository.findById(userId);
        if (userDOOptional.isPresent()) {
            UserDO beUser = userDOOptional.get();
            Optional<UserContactDO> userContactDOOptional = userContactRepository.findFirstByUserIdAndBeUserIdAndStatus(user.getId(), beUser.getId(), CommonStatus.normal);
            //已经获取过了，不应该还能获取
            if (userContactDOOptional.isPresent()) {
                QingLogger.logger.error("已经获取过用户联系方式了，不应该还能获取");
                return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
            }
            return shellOrderService.saveShellOrders(user, beUser, userShell);
        }
        return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
    }

    @PostMapping("switchUserContact")
    public ResultVO<String> switchUserContact(UserDO user, Boolean openContact) {
        if (openContact == null) {
            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
        }
        Optional<UserDetailDO> userDetailDOOptional = userDetailRepository.findFirstOneByUserId(user.getId());
        if (userDetailDOOptional.isPresent()) {
            UserDetailDO userDetail = userDetailDOOptional.get();
            userDetail.setOpenContact(openContact);
            userDetailRepository.save(userDetail);
            return new ResultVO<>();
        }
        return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
    }

    @Resource
    private DestroyAccountRepository destroyAccountRepository;

    @PostMapping("destroyAccount")
    public ResultVO<String> destroyAccount(UserDO user) {
        if (user == null) {
            QingLogger.logger.error("用户为空，不该调用注销账户功能");
            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
        }
        DestroyAccountDO destroyAccount = new DestroyAccountDO();
        Date cur = new Date();
        destroyAccount.setCreateTime(cur);
        //七天后的时间
        destroyAccount.setEndTime(new Date(cur.getTime() + CommonConst.day * 7));
        destroyAccount.setStatus(CommonStatus.normal);
        destroyAccount.setUserId(user.getId());
        destroyAccountRepository.save(destroyAccount);
        return new ResultVO<>();
    }
}