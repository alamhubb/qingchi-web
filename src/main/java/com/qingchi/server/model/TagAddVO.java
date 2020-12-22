package com.qingchi.server.model;

import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.model.talk.TagDO;
import com.qingchi.base.model.user.UserDO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * @author qinkaiyuan
 * @date 2019-11-07 15:20
 */

@Data
public class TagAddVO {
    @NotBlank
    public String tagName;
    public String description;

    public TagDO toDO(UserDO user) {
        TagDO tagDO = new TagDO();
        tagDO.setApplyUserId(user.getId());
        //先默认为1，以后设置可以更改选择类型
        tagDO.setTagTypeId(1);
        tagDO.setName(tagName);
        tagDO.setAvatar("https://cdxapp-1257733245.cos.ap-beijing.myqcloud.com/qingchi/static/qclogo.jpg!avatar");
        tagDO.setDescription(description);
        tagDO.setStatus(CommonStatus.enable);
        tagDO.setCount(0);
        tagDO.setTalkCount(0);
        tagDO.setCreateTime(new Date());
        tagDO.setUpdateTime(new Date());
        return tagDO;
    }
}
