package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.model.talk.HugDO;
import com.qingchi.base.repository.hug.HugRepository;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.server.model.HugAddVO;
import com.qingchi.server.model.HugVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("hug")
public class HugController {
    @Resource
    private TalkRepository talkRepository;

    @Resource
    private HugRepository hugRepository;

    @Resource
    private CommentRepository commentRepository;


    @PostMapping("addHug")
    public ResultVO<HugVO> addHug(@RequestBody @Valid @NotNull HugAddVO addVO, UserDO user) {
        if (addVO.getTalkId() != null) {
            Optional<TalkDO> talkOptional = talkRepository.findById(addVO.getTalkId());
            if (!talkOptional.isPresent()) {
                return new ResultVO<>("无法抱抱不存在的动态");
            }
            TalkDO talkDO = talkOptional.get();
            HugDO hugDO = addVO.toDO(talkDO);
            hugRepository.save(hugDO);
            Integer hugNum = talkDO.getHugNum();
            if (hugNum == null) {
                talkDO.setHugNum(1);
            } else {
                talkDO.setHugNum(++hugNum);
            }
            talkDO.setUpdateTime(new Date());
            talkRepository.save(talkDO);
        } else if (addVO.getCommentId() != null) {
            Optional<CommentDO> commentOptional = commentRepository.findById(addVO.getCommentId());
            if (!commentOptional.isPresent()) {
                return new ResultVO<>("无法抱抱不存在的评论");
            }
            CommentDO comment = commentOptional.get();
            HugDO hugDO = addVO.toDO(comment);
            hugRepository.save(hugDO);
            Integer hugNum = comment.getHugNum();
            if (hugNum == null) {
                comment.setHugNum(1);
            } else {
                comment.setHugNum(++hugNum);
            }
            comment.setUpdateTime(new Date());
            commentRepository.save(comment);
        } else {
            return new ResultVO<>("入参为空异常");
        }
        return new ResultVO<>();
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
