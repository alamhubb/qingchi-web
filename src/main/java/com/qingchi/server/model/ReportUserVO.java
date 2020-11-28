package com.qingchi.server.model;


import com.qingchi.base.model.user.UserDO;
import lombok.Data;

/**
 * 举报信息
 */
@Data
public class ReportUserVO {
    private Integer id;
    private String nickname;

    public ReportUserVO() {
    }

    public ReportUserVO(UserDO user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
    }
}
