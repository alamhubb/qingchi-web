package com.qingchi.server.model;

import com.qingchi.base.model.system.DistrictDO;
import lombok.Data;

/**
 * @author qinkaiyuan
 * @date 2019-10-31 21:31
 */
@Data
public class TalkDistrictVO {
    //省
    private String provinceName;
    //市
    private String cityName;
    //区县
    private String districtName;
    //统一标识
    private String adCode;

    public TalkDistrictVO(DistrictDO district) {
        this.provinceName = district.getProvinceName();
        this.cityName = district.getCityName();
        this.districtName = district.getDistrictName();
        this.adCode = district.getAdCode();
    }
}
