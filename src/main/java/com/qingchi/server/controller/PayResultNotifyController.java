package com.qingchi.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingchi.base.constant.CommonConst;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.PayType;
import com.qingchi.base.platform.qq.QQPayResult;
import com.qingchi.base.platform.weixin.WxUtil;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.ShellOrderDO;
import com.qingchi.base.repository.shell.ShellOrderRepository;
import com.qingchi.base.model.user.VipOrderDO;
import com.qingchi.base.repository.shell.VipOrderRepository;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.base.utils.UserUtils;
import com.qingchi.server.platform.QQPayNotifyResult;
import com.qingchi.server.platform.WXPayNotifyResult;
import com.thoughtworks.xstream.XStream;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 查询用户详情
 *
 * @author qinkaiyuan
 * @since 1.0.0
 */
@RestController
@RequestMapping("user")
public class PayResultNotifyController {
    @Resource
    private VipOrderRepository vipOrderRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ShellOrderRepository shellOrderRepository;

    @RequestMapping(value = "wxPayNotify", produces = MediaType.APPLICATION_XML_VALUE)
    public QQPayResult wxPayNotify(@RequestBody String notifyResult) throws IOException {
        QingLogger.logger.info("接收到支付成功通知：" + notifyResult);

        //根据结果字符串生成对象
        XStream xstream = new XStream();
        xstream.alias("xml", WXPayNotifyResult.class);
        Object qqPayResult1 = xstream.fromXML(notifyResult);
        String resultStr = objectMapper.writeValueAsString(qqPayResult1);
        HashMap resultMap = objectMapper.readValue(resultStr, HashMap.class);
        String sign = (String) resultMap.get("sign");

        resultMap.remove("sign");
        Map<String, String> stringMap = new HashMap<>();

        resultMap.forEach((k, v) -> stringMap.put(String.valueOf(k), ObjectUtils.isEmpty(v) ? null : String.valueOf(v)));

        String validateSign = WxUtil.getSignToken(stringMap);

        QingLogger.logger.info("校验签名为：" + validateSign);

        String out_trade_no = stringMap.get("out_trade_no");

        if (!validateSign.equals(sign)) {
            QQPayResult qqPayResult = new QQPayResult();
            QingLogger.logger.error("接收支付成功通知，订单号：" + out_trade_no + ",订单签名不同异常,:" + sign);
            qqPayResult.setReturn_code("ERROR");
            qqPayResult.setReturn_msg("订单异常");
            return qqPayResult;
        }
        Integer total_fee = Integer.parseInt(stringMap.get("total_fee"));
        String transaction_id = stringMap.get("transaction_id");

        /*xstream.alias("xml", WXPayNotifyResult.class);
        Object qqPayResult1 = xstream.fromXML(notifyResult);
        String resultStr = objectMapper.writeValueAsString(qqPayResult1);
        WXPayNotifyResult result = objectMapper.readValue(resultStr, WXPayNotifyResult.class);*/
        return getQqPayResult(sign, out_trade_no, total_fee, transaction_id);
    }

    @PostMapping(value = "qqPayNotify", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public QQPayResult qqPayNotify(@RequestBody QQPayNotifyResult result) {
        return getQqPayResult(result.getSign(), result.getOut_trade_no(), result.getTotal_fee(), result.getTransaction_id());
    }

    private QQPayResult getQqPayResult(String sign, String inside_order_no, Integer total_fee, String transaction_id) {
        QQPayResult qqPayResult = new QQPayResult();
        qqPayResult.setReturn_code("SUCCESS");

        Optional<VipOrderDO> vipOrderOptional = vipOrderRepository.findFirstByOrderNo(inside_order_no);
        VipOrderDO vipOrderDO;
        if (!vipOrderOptional.isPresent()) {
            QingLogger.logger.error("接收支付成功通知，订单号：" + inside_order_no + ",订单不存在");
            qqPayResult.setReturn_code("ERROR");
            qqPayResult.setReturn_msg("订单不存在");
            return qqPayResult;
        }
        vipOrderDO = vipOrderOptional.get();

        Integer verifyMoney = vipOrderDO.getAmount();
        if (!verifyMoney.equals(total_fee)) {
            QingLogger.logger.error("接收支付成功通知，订单号：" + inside_order_no + ",订单金额不相等异常,:" + total_fee);
            qqPayResult.setReturn_code("ERROR");
            qqPayResult.setReturn_msg("订单异常");
            return qqPayResult;
        }

        if (!CommonStatus.waitPay.equals(vipOrderDO.getStatus())) {
            QingLogger.logger.error("接收支付成功通知，订单号：" + inside_order_no + ",订单失效");
            qqPayResult.setReturn_code("ERROR");
            qqPayResult.setReturn_msg("订单已失效");
            return qqPayResult;
        }
        vipOrderDO.setStatus(CommonStatus.success);
        vipOrderDO.setStartDate(new Date());

        vipOrderDO.setMoney(total_fee);
        UserDO userDO = UserUtils.get(vipOrderDO.getUserId());

        if (PayType.shell.equals(vipOrderDO.getPayType())) {
            //用户现有shell
            //增加的数量
            Integer plusShell = total_fee / 10;
            userDO.setShell(userDO.getShell() + plusShell);

            ShellOrderDO shellOrderDO = new ShellOrderDO(userDO.getId(), plusShell, vipOrderDO.getId());
            shellOrderRepository.save(shellOrderDO);
            //用户现有经验值
            //userDO.setExperience(userDO.getExperience() + plusShell);
            //用户增加经验值和贝壳
        } else {
            vipOrderDO.setEndDate(new Date(vipOrderDO.getStartDate().getTime() + CommonConst.month));
            userDO.setVipFlag(true);
            userDO.setVipEndDate(vipOrderDO.getEndDate());
        }
        QingLogger.logger.info("接收" + vipOrderDO.getChannel() + "支付成功通知，订单号：" + inside_order_no + ",用户：" + userDO.getId());
        vipOrderDO.setOutOrderNo(transaction_id);
        vipOrderRepository.save(vipOrderDO);
        return qqPayResult;
    }
}