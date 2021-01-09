package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ErrorLevel;
import com.qingchi.base.constant.status.ContentStatus;
import com.qingchi.base.entity.ErrorLogUtils;
import com.qingchi.base.model.monitoring.ErrorLogDO;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.modelVO.MessageUserVO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.notify.NotifyRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.service.NotifyService;
import com.qingchi.base.service.ReportService;
import com.qingchi.base.service.ViolationService;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.CommentAddVO;
import com.qingchi.server.model.CommentDeleteVO;
import com.qingchi.server.model.TalkCommentVO;
import com.qingchi.server.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("comment")
public class CommentController {
    public static final Logger logger = LoggerFactory.getLogger(MessageUserVO.class);

    @Resource
    private EntityManager entityManager;

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private TalkRepository talkRepository;

    @Resource
    private CommentService commentService;

    @Resource
    private NotifyService notifyService;
    @Resource
    private NotifyRepository notifyRepository;
    @Resource
    private ViolationService violationService;
    @Resource
    private ReportService reportService;

    /**
     * @param commentVO
     * @param user
     * @return
     */

    @PostMapping("deleteComment")
    public ResultVO deleteComment(@RequestBody @Valid CommentDeleteVO commentVO, UserDO user) {
/**
 * 删除动态操作，
 * 如果是系统管理员删除动态，则必须填写原因，删除后发表动态的用户将被封禁
 * 如果是自己删的自己的动态，则不需要填写原因，默认原因是用户自己删除
 */
        Optional<CommentDO> optionalCommentDO = commentRepository.findOneByIdAndStatusIn(commentVO.getCommentId(), CommonStatus.otherCanSeeContentStatus);
        if (!optionalCommentDO.isPresent()) {
            return new ResultVO<>("评论已经删除");
        }
        CommentDO commentDO = optionalCommentDO.get();
        Optional<TalkDO> talkDOOptional = talkRepository.findById(commentDO.getTalkId());
        TalkDO talkDO;
        if (talkDOOptional.isPresent()) {
            talkDO = talkDOOptional.get();
        } else {
            ErrorLogUtils.save(new ErrorLogDO(user.getId(), "comment的talk不存在错误", commentDO, ErrorLevel.urgency));
            logger.error("comment的talk不存在错误:{}", commentDO);
            return new ResultVO<>("系统异常，无法删除不属于自己的动态");
        }
        //是否是自己删除自己的动态
        if (commentDO.getUserId().equals(user.getId())) {
            commentDO.setStatus(ContentStatus.delete);
            commentDO.setDeleteReason("评论用户自行删除");
        } else if (talkDO.getUserId().equals(user.getId())) {
            commentDO.setStatus(ContentStatus.delete);
            commentDO.setDeleteReason("动态用户删除评论");
        } else {
            QingLogger.logger.warn("有人尝试删除不属于自己的评论,用户名:{},id:{},尝试删除commentId：{}", user.getNickname(), user.getId(), commentDO.getId());
            return new ResultVO<>("系统异常，无法删除不属于自己的动态");
        }
        commentDO.setUpdateTime(new Date());
        commentRepository.save(commentDO);
        return new ResultVO<>();
    }

    @PostMapping("addComment")
    public ResultVO<TalkCommentVO> commentAdd(@RequestBody @Valid @NotNull CommentAddVO addVO, UserDO user) throws IOException {

        //推送消息
//        notifyService.sendNotifies(comment.getNotifies());
        //因前台问题，无论如何都返回父级comment
        /*if (comment.getParentComment() != null) {
            comment = commentRepository.findById(comment.getParentComment().getId()).get();
        }*/
        return commentService.addComment(addVO, user);
    }

    /*private Integer getCommentNo(CommentAddVO addVO) {
        Talk talk = new Talk(addVO.getTalkId());
        Comment commentNo = commentRepository.findFirstByTalkOrderByNoDesc(talk);
        ChildComment childCommentNo = childRep.findFirstByTalkOrderByNoDesc(talk);
        Integer no = 0;
        if (commentNo != null && childCommentNo != null) {
            if (commentNo.getNo() > childCommentNo.getNo()) {
                no = commentNo.getNo();
            } else {
                no = childCommentNo.getNo();
            }
        } else if (commentNo != null) {
            no = commentNo.getNo();
        } else if (childCommentNo != null) {
            no = childCommentNo.getNo();
        }
        return no;
    }*/

/*else {
        ChildComment comment = childRep.save(addVO.toChildComment(user, no));
        entityManager.clear();
        comment = childRep.findById(comment.getId()).get();
        //        messagingTemplate.convertAndSendToUser(user.getName().equals("q") ? "k" : "q", "/queue/notifications", comment);
        String username = user.getName();
        String replyUsername = username.equals("q") ? "k" : "q";
        SocketMsg socketMsg = new SocketMsg(username, replyUsername, comment.getContent());
        messagingTemplate.convertAndSendToUser(replyUsername, "/queue/notifications", socketMsg);
        //获取所有说说
        return comment;
    }*/
}
