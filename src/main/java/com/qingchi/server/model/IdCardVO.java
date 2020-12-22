package com.qingchi.server.model;

import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.model.user.IdCardDO;

import java.util.Date;

/**
 * @author qinkaiyuan
 * @date 2019-08-28 0:04
 */
public class IdCardVO {
    //正面身份证地址
    private String frontIdCardImg;
    //反面身份证地址
    private String backIdCardImg;

    public void setFrontIdCardImg(String frontIdCardImg) {
        this.frontIdCardImg = frontIdCardImg;
    }

    public void setBackIdCardImg(String backIdCardImg) {
        this.backIdCardImg = backIdCardImg;
    }

    public IdCardDO toDO() {
        IdCardDO idCardDO = new IdCardDO();
        idCardDO.setFrontIdCardImg(frontIdCardImg);
//        目前不需要身份证背面
//        idCardDO.setBackIdCardImg(backIdCardImg);
        idCardDO.setCreateDate(new Date());
        idCardDO.setStatus(CommonStatus.enable);
        return idCardDO;
    }
}