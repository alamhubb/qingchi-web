package com.qingchi.server.service;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.PayType;
import com.qingchi.base.constant.status.UserStatus;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.server.model.UserPayResultVO;
import com.qingchi.server.platform.PlatformUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 查询用户详情
 *
 * @author qinkaiyuan
 * @since 1.0.0
 */
@Service
public class UserPayService {
    public ResultVO<UserPayResultVO> payVip(String platform, String provider, UserDO user, HttpServletRequest request) throws IOException {
        if (user.getVipFlag()) {
            return new ResultVO<>("您已开通会员，无需重复开通");
        }
        Integer payAmount = ((Integer) AppConfigConst.appConfigMap.get(AppConfigConst.vipPriceKey));
        return this.pay(platform, provider, PayType.vip, payAmount, user, request);
    }

    public ResultVO<UserPayResultVO> pay(String platform, String provider, String payType, Integer amount, UserDO user, HttpServletRequest request) throws IOException {
        if (user == null) {
            return new ResultVO<>("请登陆后再进行支付操作");
        }
        if (UserStatus.violation.equals(user.getStatus())) {
            return new ResultVO<>("用户已被封禁，无法进行支付");
        }
        return new ResultVO<>(PlatformUtils.pay(platform, provider, payType, amount, user, request));
    }
}