package com.qingchi.server.model;

import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ReportContentType;
import com.qingchi.base.model.system.DistrictDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.server.platform.AliAPI;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TalkAddVO {
    private String content;
    private List<TalkImgVO> imgs;
    private String adCode;
    private List<Integer> tagIds;
    /*
     *  经度 Longitude 简写Lng
     * 纬度范围-90~90，经度范围-180~180
     */
    private Double lon;
    /*
     * 纬度 Latitude 简写Lat
     */
    private Double lat;

    public TalkDO toDO(DistrictDO district, UserDO user) {
        TalkDO talkDO = new TalkDO();
        Date curdate = new Date();
        talkDO.setCreateTime(curdate);
        talkDO.setUpdateTime(curdate);
        talkDO.setContent(this.content);
        talkDO.setHugNum(0);
        talkDO.setCommentNum(0);
        talkDO.setReportNum(0);
        talkDO.setGlobalTop(0);
        talkDO.setStatus(CommonStatus.normal);
        //已在外层经过校验

        talkDO.setUserId(user.getId());

        talkDO.setReportContentType(ReportContentType.talk);

        //如果经纬度为空
        if (this.lon == null || this.lat == null) {
            //如果经纬度为空
            RectangleVO rectangleVO = AliAPI.getRectangle();
            if (rectangleVO != null) {
                this.lon = rectangleVO.getLon();
                this.lat = rectangleVO.getLat();
            }
        }
//        PositionDO positionDO = new PositionDO(user.getId(), districtDO, this.lon, this.lat);
//        positionDO = PositionUtils.save(positionDO);
//        talkDO.setPositionId(positionDO.getId());

        //使用talk本身存储,position 和 district
        talkDO.setAdCode(district.getAdCode());
        talkDO.setAdName(district.getAdName());
        talkDO.setProvinceName(district.getProvinceName());
        talkDO.setCityName(district.getCityName());
        talkDO.setDistrictName(district.getDistrictName());
        talkDO.setLon(this.lon);
        talkDO.setLat(this.lat);

        return talkDO;
    }
}