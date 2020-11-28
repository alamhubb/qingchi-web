package com.qingchi.server.model;

import com.qingchi.base.model.user.UserDO;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author qinkaiyuan 查询结果可以没有set和空构造，前台传值可以没有get
 * @date 2019-08-13 23:34
 */
@Data
public class CommentUserVO {
    private Integer id;
    private String nickname;
    private String avatar;
    private String gender;
    private Boolean vipFlag;

    public CommentUserVO() {
    }

    public CommentUserVO(UserDO user) {
        if (user != null) {
            this.id = user.getId();
            this.nickname = user.getNickname();
            this.gender = user.getGender();
            this.avatar = user.getAvatar();
            this.vipFlag = user.getVipFlag();
        }
    }

    public static List<CommentUserVO> userDOToVOS(List<UserDO> userDOs) {
        return userDOs.stream().map(CommentUserVO::new).collect(Collectors.toList());
    }
}
