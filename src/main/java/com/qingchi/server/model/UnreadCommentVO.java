package com.qingchi.server.model;

import com.qingchi.base.entity.CommentUtils;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.repository.talk.CommentRepository;
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
public class UnreadCommentVO {
    @Resource
    private CommentRepository commentRepository;
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

    private TalkVO talk;


    private CommentUserVO user;

    private List<UnreadCommentVO> childComments;

    private UnreadCommentVO replyComment;

    public UnreadCommentVO() {
    }

    public UnreadCommentVO(CommentDO commentDO) {
        this.id = commentDO.getId();
        this.no = commentDO.getNo();
        this.content = commentDO.getContent();
        this.createTime = commentDO.getCreateTime();
        this.user = new CommentUserVO(UserUtils.get(commentDO.getUserId()));
        //不明白下面这行的意义，未读消息不需要显示子评论吧
        //        this.childComments = UnreadCommentVO.commentDOToVOS(commentRepository.findTop3ByParentCommentOrderByUpdateTimeDescIdDesc(commentDO));
        if (!ObjectUtils.isEmpty(commentDO.getReplyCommentId())) {
            this.replyComment = new UnreadCommentVO(CommentUtils.get(commentDO.getReplyCommentId()));
        }
    }

    public static List<UnreadCommentVO> commentDOToVOS(List<CommentDO> commentDOS) {
        return commentDOS.stream().map(UnreadCommentVO::new).collect(Collectors.toList());
    }
}
