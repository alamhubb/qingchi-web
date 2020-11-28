package com.qingchi.server.model;

import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.utils.UserUtils;
import lombok.Data;
import java.util.Date;

@Data
public class ReplyCommentVO {
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

    public ReplyCommentVO() {
    }

    public ReplyCommentVO(CommentDO comment) {
        this.id = comment.getId();
        this.no = comment.getNo();
        this.content = comment.getContent();
        this.createTime = comment.getCreateTime();
        this.user = new CommentUserVO(UserUtils.get(comment.getUserId()));
    }
}
