package com.qingchi.server.model;

import com.qingchi.base.model.user.UserImgDO;
import lombok.Data;

@Data
public class UploadImgVO {
    private String src;
    private Double aspectRatio;

    public UploadImgVO(UserImgDO img) {
        this.src = img.getSrc();
        this.aspectRatio = img.getAspectRatio();
    }

    public UploadImgVO(String img, Integer width, Integer height) {
        this.src = img;
        this.aspectRatio = (double) width / height;
    }
}
