package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.NotifyType;
import com.qingchi.base.model.notify.NotifyDO;
import com.qingchi.base.repository.notify.NotifyRepository;
import com.qingchi.base.modelVO.UnreadNotifyVO;
import com.qingchi.base.model.user.UserDO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("notify")
public class NotifyController {
    @Resource
    private NotifyRepository notifyRepository;

    @PostMapping("queryNotifies")
    public ResultVO<List<UnreadNotifyVO>> queryNotifies(UserDO user) {
        List<NotifyDO> notifyDOS = notifyRepository.findTop20ByReceiveUserIdAndTypeInOrderByIdDesc(user.getId(), NotifyType.comments);
        return new ResultVO<>(UnreadNotifyVO.unreadNotifyDOToVOS(notifyDOS));
    }

    @PostMapping("queryUnreadNotifies")
    public ResultVO<List<UnreadNotifyVO>> queryUnreadComments(UserDO user) {
        List<NotifyDO> notifyDOS = notifyRepository.findAllByReceiveUserIdAndTypeInAndHasReadFalseOrderByIdDesc(user.getId(), NotifyType.comments);
        return new ResultVO<>(UnreadNotifyVO.unreadNotifyDOToVOS(notifyDOS));
    }

    @PostMapping("queryUnreadNotifiesAndUpdateHasRead")
    public ResultVO<List<UnreadNotifyVO>> queryUnreadNotifiesAndUpdateHasRead(UserDO user) {
        List<NotifyDO> notifyDOS = notifyRepository.findAllByReceiveUserIdAndTypeInAndHasReadFalseOrderByIdDesc(user.getId(), NotifyType.comments);
        for (NotifyDO notifyDO : notifyDOS) {
            notifyDO.setHasRead(true);
        }
        notifyRepository.saveAll(notifyDOS);
        notifyDOS = notifyRepository.findTop20ByReceiveUserIdAndTypeInOrderByIdDesc(user.getId(), NotifyType.comments);
        return new ResultVO<>(UnreadNotifyVO.unreadNotifyDOToVOS(notifyDOS));
    }
}
