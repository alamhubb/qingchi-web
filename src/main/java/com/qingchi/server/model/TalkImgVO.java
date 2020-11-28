package com.qingchi.server.model;

import com.qingchi.base.model.talk.TalkImgDO;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class TalkImgVO {
    private Integer id;
    private String src;
    private Double aspectRatio;
    private Integer size;
    //压缩率
    private Double quality;

    public TalkImgVO() {
    }

    public TalkImgVO(TalkImgDO talkImgDO) {
        this.id = talkImgDO.getId();
        this.src = talkImgDO.getSrc();
        this.aspectRatio = talkImgDO.getAspectRatio();
    }

    public TalkImgDO toDO() {
        TalkImgDO talkImgDO = new TalkImgDO();
        talkImgDO.setSrc(this.src);
        talkImgDO.setAspectRatio(this.aspectRatio);
        talkImgDO.setSize(this.size);
        talkImgDO.setCreateTime(new Date());
        talkImgDO.setQuality(this.quality);
        return talkImgDO;
    }

    public static List<TalkImgVO> talkImgDOToVOS(List<TalkImgDO> imgDOS) {
        return imgDOS.stream().map(TalkImgVO::new).collect(Collectors.toList());
    }

    public static List<TalkImgDO> talkImgVOToDOS(List<TalkImgVO> imgVOS) {
        return imgVOS.stream().map(TalkImgVO::toDO).collect(Collectors.toList());
    }
}
