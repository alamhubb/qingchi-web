package com.qingchi.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.constant.PayAmountSet;
import com.qingchi.base.constant.PayType;
import com.qingchi.base.constant.PlatformType;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.service.UserService;
import com.qingchi.base.repository.user.UserDetailRepository;
import com.qingchi.base.repository.user.UserImgRepository;
import com.qingchi.base.repository.shell.VipOrderRepository;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.UserPayResultVO;
import com.qingchi.server.model.UserPayVO;
import com.qingchi.server.service.UserPayService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * 查询用户详情
 *
 * @author qinkaiyuan
 * @since 1.0.0
 */
@RestController
@RequestMapping("user")
public class UserPayController {
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
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private VipOrderRepository vipOrderRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private UserPayService userPayService;

    /**
     * 前台判断，用户是否安装成功过一次，没有的话判断用户现在是不是桌面启动，
     * 是桌面启动则给予一次赠送会员
     * <p>
     * <p>
     * <p>
     * 如果用户触发了 安装送会员功能
     */
    @PostMapping("pay")
    public ResultVO<UserPayResultVO> pay(@RequestBody @Valid @NotNull UserPayVO payVO, HttpServletRequest request, UserDO user) throws IOException {
        String provider = payVO.getProvider();
        String platform = payVO.getPlatform();
        if (StringUtils.isEmpty(platform)) {
            platform = PlatformType.mp;
        }
        //前台传过来的为元
        Integer payAmount = payVO.getAmount();
        String payType = payVO.getPayType();
        if (PayType.shell.equals(payType)) {
            if (PayAmountSet.amountList.contains(payAmount)) {
                //前台为元，到这里转换为分
                return userPayService.pay(platform, provider, payType, payAmount * 100, user, request);
            }
        } else if (PayType.vip.equals(payType)) {
            return userPayService.payVip(platform, provider, user, request);
        }
        QingLogger.logger.error("系统异常，充值功能遭到攻击");
        return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
    }

    @PostMapping("payVipNew")
    public ResultVO<UserPayResultVO> payVipNew(String platform, HttpServletRequest request, UserDO user) throws IOException {
        return userPayService.payVip(PlatformType.mp, platform, user, request);
    }

    /**
     * 前台判断，用户是否安装成功过一次，没有的话判断用户现在是不是桌面启动，
     * 是桌面启动则给予一次赠送会员
     * <p>
     * <p>
     * <p>
     * 如果用户触发了 安装送会员功能
     * <p>
     * 已作废，下版本删除2020.7.12
     */
    @PostMapping("payVip")
    @Deprecated
    public ResultVO<UserPayResultVO> payVip(String platform, HttpServletRequest request, UserDO user) throws IOException {
        return userPayService.payVip(PlatformType.mp, platform, user, request);
    }
}