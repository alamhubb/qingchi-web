package com.qingchi.server.store;

import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.server.check.CommentAddLineTransfer;
import com.qingchi.server.factory.CommentFactory;
import com.qingchi.server.model.CommentAddVO;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Date;

@Repository
public class CommentStore {
    @Resource
    private CommentRepository commentRepository;
    @Resource
    private CommentFactory commentFactory;

    //保存新增的comment
    public CommentDO saveAddComment(CommentAddVO addVO, Integer requestUserId) {
        CommentDO commentDO = commentFactory.createCommentDO(
                addVO,
                requestUserId
        );

        //保存为预审核状态，关键词校验通过，改为正常
        commentDO = commentRepository.save(commentDO);
        return commentDO;
    }


    /**
     * 修改comment时，关联更新回复评论和父评论，返回更新后的内容到comment中了
     *
     * @param commentAddLineTransfer
     * @return
     */
    public CommentAddLineTransfer updateCommentByAddComment(CommentAddLineTransfer commentAddLineTransfer) {
        Date curDate = new Date();
        CommentDO parentComment = commentAddLineTransfer.getParentComment();
        //执行comment关联操作
        //为父评论添加子评论数
        if (parentComment != null) {
            Integer ChildCommentNum = parentComment.getChildCommentNum();
            //这里属于业务
            if (ChildCommentNum == null) {
                parentComment.setChildCommentNum(1);
            } else {
                parentComment.setChildCommentNum(++ChildCommentNum);
            }
            CommentDO replyComment = commentAddLineTransfer.getReplyComment();
            if (replyComment == null) {
                //只有直接回复父评论，才更新时间
                parentComment.setUpdateTime(curDate);
            } else {
                //测试所有自己评论自己，刚才处空指针了
                replyComment.setUpdateTime(curDate);
                //更新后保存到数据库
                replyComment = commentRepository.save(replyComment);
                commentAddLineTransfer.setReplyComment(replyComment);
            }
            //更新后保存到数据库
            parentComment = commentRepository.save(parentComment);
            commentAddLineTransfer.setParentComment(parentComment);
        }
        return commentAddLineTransfer;
    }
}
