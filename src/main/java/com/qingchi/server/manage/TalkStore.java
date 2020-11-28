package com.qingchi.server.manage;

import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.repository.talk.TalkRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Date;

@Repository
public class TalkStore {
    @Resource
    private TalkRepository talkRepository;

    /**
     * 需要对talk进行操作，必传talk不可精简
     *
     * @param talk
     * @return
     */
    public TalkDO updateTalkByAddComment(TalkDO talk) {
        Integer commentNum = talk.getCommentNum();
        if (commentNum == null) {
            talk.setCommentNum(1);
        } else {
            talk.setCommentNum(++commentNum);
        }
        //更新talk更新时间
        talk.setUpdateTime(new Date());

        talk = talkRepository.save(talk);
        return talk;
    }
}
