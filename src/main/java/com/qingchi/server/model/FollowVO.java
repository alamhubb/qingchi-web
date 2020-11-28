package com.qingchi.server.model;

import com.qingchi.base.model.talk.HugDO;
import lombok.Data;

import java.util.Date;

@Data
public class FollowVO {
    private Date createTime;

    public FollowVO() {
    }

    public FollowVO(HugDO hugDO) {
        this.createTime = hugDO.getCreateTime();
    }
}
