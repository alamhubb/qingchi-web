package com.qingchi.server.model;

import com.qingchi.base.model.user.UserDO;
import lombok.Data;

/**
 * 不需要像帖子一样，每次有回复都刷新，因为不愁看，且你评论后的，有动静你会有通知
 */
@Data
public class FollowUserVO {
    private Integer id;
    private String nickname;
    private String avatar;
    private Integer fansNum;
    private Integer followNum;
    //是否为vip，
    private Boolean vipFlag;
    private String gender;

    private String followStatus;

    //对方是否有关注自己
    private Boolean beFollow;

    public FollowUserVO() {
    }

    public FollowUserVO(UserDO user, String followStatus, Boolean beFollow) {
        this.id = user.getId();
        this.avatar = user.getAvatar();
        this.nickname = user.getNickname();
        this.gender = user.getGender();
        this.fansNum = user.getFansNum();
        this.followNum = user.getFollowNum();
        this.vipFlag = user.getVipFlag();
        this.followStatus = followStatus;
        this.beFollow = beFollow;
    }
}