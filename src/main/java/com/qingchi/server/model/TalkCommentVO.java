package com.qingchi.server.model;

import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.entity.CommentUtils;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.utils.UserUtils;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Component
public class TalkCommentVO {
    private static CommentRepository commentRepository;

    @Resource
    public void setCommentRepository(CommentRepository commentRepository) {
        TalkCommentVO.commentRepository = commentRepository;
    }

    //如果这个评论 有parent，就代表已经是一个子评论，就不用把他设置为parent而是用它的parentId，他是否有parent
    private Integer id;


    private Integer no;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论时间
     */
    private Date createTime;

    private CommentUserVO user;

    private List<TalkCommentVO> childComments;

    private ReplyCommentVO replyComment;

    /**
     * 评论数量，子评论数量
     */
    private Integer childCommentNum;
    /**
     * 抱抱次数
     */
    private Integer hugNum;
    private Integer reportNum;

    public TalkCommentVO() {
    }

    public TalkCommentVO(UserDO user, CommentDO comment, boolean showAll) {
        this.id = comment.getId();
        this.no = comment.getNo();
        this.content = comment.getContent();
        this.createTime = comment.getCreateTime();
        this.hugNum = comment.getHugNum();
        this.reportNum = comment.getReportNum();
        this.childCommentNum = comment.getChildCommentNum();
        this.user = new CommentUserVO(UserUtils.get(comment.getUserId()));
        if (showAll) {
            this.childComments = TalkCommentVO.commentDOToVOS(user, commentRepository.findTop50ByParentCommentIdAndStatusInOrderByUpdateTimeDesc(comment.getId(), CommonStatus.selfCanSeeContentStatus), true);
        } else {
            this.childComments = TalkCommentVO.commentDOToVOS(user, commentRepository.findTop3ByParentCommentIdAndStatusInOrderByUpdateTimeDesc(comment.getId(), CommonStatus.selfCanSeeContentStatus), false);
        }
        if (!ObjectUtils.isEmpty(comment.getReplyCommentId())) {
            this.replyComment = new ReplyCommentVO(CommentUtils.get(comment.getReplyCommentId()));
        }
    }

    public static List<TalkCommentVO> commentDOToVOS(UserDO user, List<CommentDO> commentDOS, boolean showAll) {
        return commentDOS.stream()
                //过滤掉非自己的预审核状态的评论
                .filter(talkCommentDO -> {
                    // 用户不为 null && 自己的评论才显示
                    return (user != null && talkCommentDO.getUserId().equals(user.getId()))
                            //或者评论的状态不为预审核
                            || !CommonStatus.preAudit.equals(talkCommentDO.getStatus());
                })
                .map(talkCommentDO -> new TalkCommentVO(user, talkCommentDO, showAll)).collect(Collectors.toList());
    }
}
