package com.qingchi.server.model;

import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.model.talk.HugDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.utils.RequestUtils;
import lombok.Data;

import java.util.Date;

/**
 * TODO〈一句话功能简述〉
 * TODO〈功能详细描述〉
 *
 * @author qinkaiyuan
 * @since TODO[起始版本号]
 */
@Data
public class HugAddVO {
    private Integer talkId;
    private Integer commentId;

    public HugDO toDO(TalkDO talkDO) {
        HugDO hugDO = new HugDO();
        hugDO.setCreateTime(new Date());
        hugDO.setTalkId(talkDO.getId());
        hugDO.setUserId(RequestUtils.getUser().getId());
        return hugDO;
    }

    public HugDO toDO(CommentDO commentDO) {
        HugDO hugDO = new HugDO();
        hugDO.setCreateTime(new Date());
        hugDO.setCommentId(commentDO.getId());
        hugDO.setUserId(RequestUtils.getUser().getId());
        return hugDO;
    }

}
