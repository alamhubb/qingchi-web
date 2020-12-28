package com.qingchi.server.verify;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.constant.status.ChatStatus;
import com.qingchi.base.constant.status.ChatUserStatus;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.repository.chat.ChatUserRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

@Service
public class ChatUserVerify {
    @Resource
    private ChatRepository chatRepository;
    @Resource
    private ChatUserRepository chatUserRepository;

    //基础，不存在就是系统异常
    public ResultVO<ChatUserDO> checkChatHasUserId(Long chatId, Integer userId) {
        Optional<ChatUserDO> chatUserDOOptional = chatUserRepository.findFirstByChatIdAndChatStatusAndUserId(chatId, ChatStatus.enable, userId);
        return chatUserDOOptional.map(ResultVO::new).orElseGet(() -> new ResultVO<>(ErrorCode.SYSTEM_ERROR));
        /*Optional<ChatDO> chatDOOptional = chatRepository.findById(chatId);
        if (chatDOOptional.isPresent()) {
            ChatDO chatDO = chatDOOptional.get();

        }*/
    }

    public ResultVO<ChatUserDO> checkChatHasUserIdAndEnable(Long chatId, Integer userId) {
        ResultVO<ChatUserDO> resultVO = this.checkChatHasUserId(chatId, userId);
        if (resultVO.hasError()) {
            return resultVO;
        }
        ChatUserDO chatUserDO = resultVO.getData();
        if (!chatUserDO.getStatus().equals(ChatUserStatus.enable)) {
            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
        }
        return new ResultVO<>(chatUserDO);
    }
}
