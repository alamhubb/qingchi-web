package com.qingchi.server.check;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.UserType;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.notify.NotifyRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.service.NotifyService;
import com.qingchi.base.service.ViolationService;
import com.qingchi.server.model.CommentAddVO;
import com.qingchi.base.service.IllegalWordService;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.util.Optional;

/**
 * @author qinkaiyuan
 * @date 2018-09-16 18:35
 */
@Service
public class CommentCheckService {

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
    private IllegalWordService illegalWordService;
    @Resource
    private ModelContentCheck modelContentCheck;

  /*  @Resource
    CommentRepository comRep;

    public List<Comment> queryCommentList() {
        List<Comment> comments = comRep.findAllByTalk(new Talk(15));
        return comments;
    }*/

    //校验添加新增comment的评论是否正确
    public ResultVO<CommentAddLineTransfer> checkCommentAddVO(CommentAddVO addVO, UserDO requestUser) {
        ResultVO resultVO = modelContentCheck.checkUserAndContent(addVO.getContent(),requestUser);
        //校验内容是否违规
        if (resultVO.hasError()) {
            return new ResultVO<>(resultVO);
        }

        //因为与下面有关联所以拿到了上面
        Optional<TalkDO> talkOptional = talkRepository.findById(addVO.getTalkId());
        if (!talkOptional.isPresent()) {
            return new ResultVO<>("无法评论不存在的动态");
        }
        TalkDO talkDO = talkOptional.get();
        //不为系统用户才校验
        if (!UserType.system.equals(requestUser.getType())) {
            if (!ObjectUtils.isEmpty(talkDO.getGlobalTop()) && talkDO.getGlobalTop() > 0) {
                return new ResultVO<>("禁止评论官方置顶");
            }
        }

        CommentDO parentCommentDO = null;
        if (addVO.getCommentId() != null) {
            Optional<CommentDO> commentOptional = commentRepository.findById(addVO.getCommentId());
            if (!commentOptional.isPresent()) {
                return new ResultVO<>("无法回复不存在的评论");
            }
            parentCommentDO = commentOptional.get();
        }
        //得到回复的评论
        CommentDO replyCommentDO = null;
        if (addVO.getReplyCommentId() != null) {
            Optional<CommentDO> replyCommentOptional = commentRepository.findById(addVO.getReplyCommentId());
            if (!replyCommentOptional.isPresent()) {
                return new ResultVO<>("无法回复不存在的子评论");
            }
            replyCommentDO = replyCommentOptional.get();
        }

        return new ResultVO<>(new CommentAddLineTransfer(talkDO, parentCommentDO, replyCommentDO));
    }
}
