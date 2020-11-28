package com.qingchi.server.model;

import com.qingchi.base.model.user.UserDO;
import lombok.Data;

/**
 * @author qinkaiyuan 查询结果可以没有set和空构造，前台传值可以没有get
 * @date 2019-08-13 23:34
 */
@Data
public class TalkUserVO {
    private Integer id;
    private String nickname;
    private String gender;
    private String avatar;
    private Integer age;
    //是否为vip，
    private Boolean vipFlag;
    //已开vip多少个月
    private Integer loveValue;
    private Integer justiceValue;
    private Integer gradeLevel;
    private Integer wealthLevel;

    public TalkUserVO() {
    }

    public TalkUserVO(UserDO user) {
        if (user != null) {
            this.id = user.getId();
            this.nickname = user.getNickname();
            this.gender = user.getGender();
            this.avatar = user.getAvatar();
            this.age = user.getAge();
            //满分10W /1千，得到百分之颜值分
            this.vipFlag = user.getVipFlag();
            this.loveValue = user.getLoveValue();
            this.justiceValue = user.getJusticeValue();
        }
    }
}
