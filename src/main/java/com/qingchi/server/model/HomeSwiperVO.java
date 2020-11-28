package com.qingchi.server.model;

import com.qingchi.base.model.system.HomeSwiperDO;
import lombok.Data;

/**
 * @author qinkaiyuan
 * @date 2020-05-23 17:21
 */
@Data
public class HomeSwiperVO {
    private String name;
    private String skipUrl;
    private String imgUrl;
    private Boolean skip;
    private String skipType;
    private String standUrl;
    private String standType;

    public HomeSwiperVO(HomeSwiperDO homeSwiper) {
        this.name = homeSwiper.getName();
        this.skipUrl = homeSwiper.getSkipUrl();
        this.imgUrl = homeSwiper.getImgUrl();
        this.skip = homeSwiper.getSkip();
        this.skipType = homeSwiper.getSkipType();
        this.standUrl = homeSwiper.getStandUrl();
        this.standType = homeSwiper.getStandType();
    }
}
