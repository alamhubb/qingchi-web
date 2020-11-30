package com.qingchi.server.domain;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ChatType;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.model.user.UserContactDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.repository.follow.FollowRepository;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.service.ShellOrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PayShellOpenChatDomain {
    @Resource
    ChatRepository chatRepository;
    @Resource
    ChatUserRepository chatUserRepository;
    @Resource
    FollowRepository followRepository;
    @Resource
    private ShellOrderService shellOrderService;

    public ResultVO<ChatVO> payShellOpenChat(UserDO user, Integer receiveUserId, ChatDO chatDO, ChatUserDO chatUserDO) {

        Optional<ChatUserDO> receiveChatUserDOOptional = chatUserRepository.findFirstByChatIdAndUserId(chatDO.getId(), receiveUserId);
        if (!receiveChatUserDOOptional.isPresent()) {
            QingLogger.logger.error("chat：{}下不存在该用户：{}", chatDO.getId(), user.getId());
            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
        }
        ChatUserDO receiveChatUserDO = receiveChatUserDOOptional.get();

        chatDO.setUpdateTime(new Date());
//        chatDO.set

        //开启chat


        //你需要自己的chat为代开起
        //需要对方的用户名，昵称。会话未开启
        return null;
    }
}
