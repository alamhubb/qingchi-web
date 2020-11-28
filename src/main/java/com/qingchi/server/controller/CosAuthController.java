package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.utils.ImgUtil;
import com.tencent.cloud.CosStsClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.TreeMap;

@RestController
@RequestMapping("cos")
public class CosAuthController {
    @Value("${config.qq.cos.secretId}")
    private String secretId;
    @Value("${config.qq.cos.secretKey}")
    private String secretKey;
    @Value("${config.qq.cos.bucketName}")
    private String bucketName;
    @Value("${config.qq.cos.region}")
    private String region;

    @PostMapping("getCosAuthorization")
    public ResultVO<String> getCosAuthorization(UserDO user) {
        TreeMap<String, Object> config = new TreeMap<>();

        try {
            // 替换为您的 SecretId
            config.put("SecretId", secretId);
            // 替换为您的 SecretKey
            config.put("SecretKey", secretKey);

            // 临时密钥有效时长，单位是秒，默认1800秒，最长可设定有效期为7200秒
            config.put("durationSeconds", 1800);

            // 换成您的 bucket
            config.put("bucket", bucketName);
            // 换成 bucket 所在地区
            config.put("region", region);

            // 这里改成允许的路径前缀，可以根据自己网站的用户登录态判断允许上传的具体路径，例子：a.jpg 或者 a/* 或者 * 。
            // 如果填写了“*”，将允许用户访问所有资源；除非业务需要，否则请按照最小权限原则授予用户相应的访问权限范围。
            config.put("allowPrefix", ImgUtil.getUserImgUrl(user.getId()));

            // 密钥的权限列表。简单上传、表单上传和分片上传需要以下的权限，其他权限列表请看 https://cloud.tencent.com/DOcument/product/436/31923
            String[] allowActions = new String[]{
                    // 表单上传、小程序上传
                    "name/cos:PostObject"
            };
            config.put("allowActions", allowActions);

            JSONObject credential = CosStsClient.getCredential(config);
            //成功返回临时密钥信息，如下打印密钥信息
            ResultVO<String> resultVO = new ResultVO<>();
            resultVO.setData(credential.toString());
            return resultVO;
        } catch (Exception e) {
            //失败抛出异常
            throw new IllegalArgumentException("no valid secret !");
        }
    }
}
