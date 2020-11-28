package com.qingchi.server.service;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.domain.ReportDomain;
import com.qingchi.base.model.notify.NotifyDO;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.service.KeywordsTriggerService;
import com.qingchi.base.service.NotifyService;
import com.qingchi.base.service.ReportService;
import com.qingchi.server.check.CommentAddLineTransfer;
import com.qingchi.server.check.CommentCheckService;
import com.qingchi.server.domain.CommentDomain;
import com.qingchi.server.domain.NotifyDomain;
import com.qingchi.server.factory.CommentFactory;
import com.qingchi.server.model.CommentAddVO;
import com.qingchi.server.model.TalkCommentVO;
import com.qingchi.server.store.CommentStore;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2018-09-16 18:35
 */
@Service
public class CommentService {
    @Resource
    private CommentCheckService commentCheckService;
    @Resource
    private KeywordsTriggerService keywordsTriggerService;
    @Resource
    private CommentDomain commentDomain;
    @Resource
    private NotifyDomain notifyDomain;
    @Resource
    private ReportService reportService;
    @Resource
    private ReportDomain reportDomain;
    @Resource
    private CommentStore commentStore;
    @Resource
    private CommentFactory commentFactory;

    @Resource
    private CommentRepository commentRepository;
    @Resource
    private TalkRepository talkRepository;
    @Resource
    private NotifyService notifyService;
  /*  @Resource
    CommentRepository comRep;

    public List<Comment> queryCommentList() {
        List<Comment> comments = comRep.findAllByTalk(new Talk(15));
        return comments;
    }*/

    public ResultVO<TalkCommentVO> addComment(CommentAddVO addVO, UserDO requestUser) throws IOException {
        //校验comment
        ResultVO<CommentAddLineTransfer> resultVO = commentCheckService.checkCommentAddVO(
                addVO,
                requestUser
        );

        //校验结果
        if (resultVO.hasError()) {
            return new ResultVO<>(resultVO);
        }
        //校验时候，访问了数据库，存储了talk、parent、reply这些值，方便以后使用，传输使用
        CommentAddLineTransfer commentAddLineTransfer = resultVO.getData();

        //保存comment，内部关联保存了talk、parentComment、replyComment
        commentAddLineTransfer = commentDomain.saveComment(addVO, commentAddLineTransfer, requestUser.getId());

        CommentDO commentDO = commentAddLineTransfer.getCommentDO();

        // 校验是否触发关键词
        reportDomain.checkKeywordsCreateReport(commentDO);


        List<NotifyDO> notifyDOS = notifyDomain.saveCreateCommentNotifies(commentDO, commentAddLineTransfer.getTalk(), commentAddLineTransfer.getParentComment(), commentAddLineTransfer.getReplyComment(), requestUser);

        //推送消息
        notifyService.sendNotifies(notifyDOS, requestUser);
        return new ResultVO<>(new TalkCommentVO(requestUser, commentDO, false));
    }


}


