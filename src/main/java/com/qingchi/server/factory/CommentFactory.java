package com.qingchi.server.factory;

import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ReportContentType;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.server.model.CommentAddVO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

@Component
public class CommentFactory {
    @Resource
    private CommentRepository commentRepository;

    /***
     *
     * @param addVO
     * @param requestUserId
     * @return
     */
    public CommentDO createCommentDO(CommentAddVO addVO, Integer requestUserId) {
        //创建 do一步,获取序号
        CommentDO commentNoDO = commentRepository.findFirstByTalkIdOrderByIdDesc(addVO.getTalkId());
        Integer commentNo = 0;
        if (commentNoDO != null) {
            commentNo = commentNoDO.getNo();
        }
        //构建DO，comment基础内容
        CommentDO comment = new CommentDO();
        comment.setNo(++commentNo);
        comment.setContent(addVO.getContent());
        comment.setStatus(CommonStatus.normal);
        comment.setReportContentType(ReportContentType.comment);
        comment.setHugNum(0);
        comment.setChildCommentNum(0);
        comment.setReportNum(0);
        Date curDate = new Date();
        comment.setCreateTime(curDate);
        comment.setUpdateTime(curDate);
        //关联内容
        comment.setReplyCommentId(addVO.getReplyCommentId());
        comment.setParentCommentId(addVO.getCommentId());
        comment.setTalkId(addVO.getTalkId());
        comment.setUserId(requestUserId);
        return comment;
    }
}