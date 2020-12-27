package com.qingchi.server.verify;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.status.ChatStatus;
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

    public ResultVO<ChatUserDO> checkChatHasUserId(Long chatId, Integer userId) {
        Optional<ChatUserDO> chatUserDOOptional = chatUserRepository.findFirstByChatIdAndChatStatusAndUserId(chatId, ChatStatus.enable, userId);
        return chatUserDOOptional.map(ResultVO::new).orElseGet(() -> new ResultVO<>("不存在的会话"));
        /*Optional<ChatDO> chatDOOptional = chatRepository.findById(chatId);
        if (chatDOOptional.isPresent()) {
            ChatDO chatDO = chatDOOptional.get();

        }*/
    }
}
