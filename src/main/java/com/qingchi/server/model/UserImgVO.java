package com.qingchi.server.model;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ReportContentType;
import com.qingchi.base.constant.status.ContentStatus;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.IdentityImgDO;
import com.qingchi.base.model.user.UserImgDO;
import lombok.Data;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Component
public class UserImgVO {
    private Integer id;
    @NotBlank
    private String src;
    @NotNull
    private Double aspectRatio;
    private Double height;
    private Double width;
    //压缩率
    private Double quality;
    private Integer size;
    private Integer reportNum;

    public UserImgVO() {
    }

    public UserImgVO(UserImgDO img) {
        this.id = img.getId();
        this.src = img.getSrc();
        this.aspectRatio = img.getAspectRatio();
        this.setWidth((double) 360);
        this.setHeight(this.getWidth() / this.getAspectRatio());
        this.reportNum = img.getReportNum();
    }

    public UserImgDO toUserImgDO(UserDO user, String imgUrl) {
        //这里需要记录，变更历史，通过照片有效无效记录，
        UserImgDO userImgDO = new UserImgDO();
        userImgDO.setSrc(imgUrl + this.getSrc());
        userImgDO.setAspectRatio(this.getAspectRatio());
        userImgDO.setQuality(this.getQuality());
        userImgDO.setSize(this.size);
        userImgDO.setUserId(user.getId());
        userImgDO.setStatus(ContentStatus.enable);
        userImgDO.setCreateTime(new Date());
        userImgDO.setContent(AppConfigConst.img_content);
        userImgDO.setReportContentType(ReportContentType.userImg);
        userImgDO.setReportNum(0);
        userImgDO.setIsSelfAuth(false);
        user.setAvatar(userImgDO.getSrc() + "!avatar");
        return userImgDO;
    }

    public IdentityImgDO toIdentityImgDO(UserDO user, String imgUrl) {
        //这里需要记录，变更历史，通过照片有效无效记录，
        IdentityImgDO identityImgDO = new IdentityImgDO();
        identityImgDO.setSrc(imgUrl + this.getSrc());
        identityImgDO.setAspectRatio(this.getAspectRatio());
        identityImgDO.setUserId(user.getId());
        identityImgDO.setCreateTime(new Date());
        identityImgDO.setStatus(CommonStatus.authFail);
        return identityImgDO;
    }

    public static List<UserImgVO> userImgDOToVOS(List<UserImgDO> imgDOs) {
        return imgDOs.stream().map(UserImgVO::new).collect(Collectors.toList());
    }
}
