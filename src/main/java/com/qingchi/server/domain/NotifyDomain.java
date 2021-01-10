package com.qingchi.server.domain;

import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.NotifyType;
import com.qingchi.base.constant.status.ContentStatus;
import com.qingchi.base.model.notify.NotifyDO;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.notify.NotifyRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.service.NotifyService;
import com.qingchi.base.service.ReportService;
import com.qingchi.base.service.ViolationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotifyDomain {

    @Resource
    private EntityManager entityManager;

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private TalkRepository talkRepository;

    @Resource
    private NotifyService notifyService;
    @Resource
    private NotifyRepository notifyRepository;
    @Resource
    private ViolationService violationService;
    @Resource
    private ReportService reportService;

    public List<NotifyDO> saveCreateCommentNotifies(CommentDO commentDO, TalkDO talkDO, CommentDO parentCommentDO, CommentDO replyCommentDO, UserDO requestUser) {
        List<NotifyDO> notifies = new ArrayList<>();
        Integer talkUserId = talkDO.getUserId();
        Integer commentId = commentDO.getId();
        Integer talkId = talkDO.getId();
        Integer commentUserId = commentDO.getUserId();
        //如果评论的自己的动态，则给所有二级评论人发通知
        if (commentUserId.equals(talkUserId)) {
            //自己评论了自己的talk则要通知所有 其他评论了这个talk的人
            //判断不为子评论，本人回复了别人就是子评论，不给其他评论了这个talk的人发送通知
            if (parentCommentDO == null) {
                List<CommentDO> commentDOS = commentRepository.findTop50ByTalkIdAndStatusInAndParentCommentIdIsNullOrderByUpdateTimeDesc(talkUserId, ContentStatus.selfCanSeeContentStatus);
                for (CommentDO childComment : commentDOS) {
                    //不给自己发送通知
                    if (!childComment.getUserId().equals(commentUserId)) {
                        NotifyDO notify = new NotifyDO(commentUserId, childComment.getUserId(), commentId, talkId, NotifyType.talk_comment);
                        notifies.add(notify);
                    }
                }
            }
        } else {
            //如果不是评论自己的talk，给talk的主人发消息
            NotifyDO talkNotify = new NotifyDO(commentUserId, talkDO.getUserId(), commentId, talkId, NotifyType.talk_comment);
            notifies.add(talkNotify);
        }


        //如果回复的评论
        if (parentCommentDO != null) {
            Integer parentCommentUserId = parentCommentDO.getUserId();
            //这里查个逻辑，如果自己评论了自己的评论，则给所有子评论发送通知


            //判断不会重复通知,talk和comment 用户不相同，才发送通知，如果自己评论了自己的评论
            //父评论用户不为talk的用户，不是talk用户自己评论自己的
            //是talk用户怎么样，是talk用户不用发送通知
            //因为上面给talk发过，所以做了个判断您，不为talk
            //评论自己的话就不给自己发通知了
            if (!parentCommentUserId.equals(commentDO.getUserId())) {
                NotifyDO commentNotify = new NotifyDO(commentUserId, parentCommentUserId, commentId, talkId, NotifyType.comment_comment);
                notifies.add(commentNotify);
            } else {
                //但是要给所有这条评论的子评论用户发通知
                List<CommentDO> childComments = commentRepository.findByParentCommentId(parentCommentDO.getId());
                for (CommentDO childCommentDO : childComments) {
                    //但是不能给自己发
                    if (!childCommentDO.getUserId().equals(commentUserId)) {
                        NotifyDO notify = new NotifyDO(commentUserId, childCommentDO.getUserId(), commentId, talkId, NotifyType.comment_comment);
                        notifies.add(notify);
                    }
                }
            }

            Date curDate = new Date();
            //只有子评论需要replyUser
            if (replyCommentDO != null) {
                //判断不重复发送通知，如果子回复和talk或者和评论相同，则不需要再发送通知了

                //如果自己回复自己就不需要发通知了
                NotifyDO replyCommentNotify = new NotifyDO(commentUserId, replyCommentDO.getUserId(), commentId, talkId, NotifyType.reply_comment);
                notifies.add(replyCommentNotify);
            }
        }
        Collections.reverse(notifies);
        notifies = notifies.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() ->
                new TreeSet<>(Comparator.comparing(NotifyDO::getReceiveUserId))), ArrayList::new));
        notifies = notifyRepository.saveAll(notifies);
        return notifies;
    }
}
