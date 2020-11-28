package com.qingchi.server.domain;

import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.service.ReportService;
import com.qingchi.server.check.CommentCheckService;
import com.qingchi.server.check.CommentAddLineTransfer;
import com.qingchi.server.factory.CommentFactory;
import com.qingchi.server.model.CommentAddVO;
import com.qingchi.server.store.CommentStore;
import com.qingchi.server.manage.TalkStore;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CommentDomain {
    @Resource
    private CommentRepository commentRepository;
    @Resource
    private CommentStore commentStore;

    @Resource
    private TalkStore talkStore;

    @Resource
    private ReportService reportService;

    @Resource
    private CommentCheckService commentCheckService;
    @Resource
    private CommentDomain commentDomain;
    @Resource
    private NotifyDomain notifyDomain;

    @Resource
    private CommentFactory commentFactory;

    @Resource
    private TalkRepository talkRepository;

    public CommentAddLineTransfer saveComment(CommentAddVO addVO, CommentAddLineTransfer commentAddLineTransfer, Integer requestUserId) {
        //创建和保存comment到db
        CommentDO commentDO = commentStore.saveAddComment(addVO, requestUserId);

        //关联更新talk到db，时间、评论次数等
        TalkDO talkDO = talkStore.updateTalkByAddComment(commentAddLineTransfer.getTalk());
        //设置为更新后的talk
        commentAddLineTransfer.setTalk(talkDO);
        //关联更新comment到db，时间、评论次数等
        commentAddLineTransfer = commentStore.updateCommentByAddComment(commentAddLineTransfer);

        commentAddLineTransfer.setCommentDO(commentDO);

        return commentAddLineTransfer;
    }
}
