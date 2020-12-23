package com.qingchi.server.verify;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.repository.chat.ChatUserRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

@Service
public class ChatVerify {
    @Resource
    private ChatRepository chatRepository;
    @Resource
    private ChatUserRepository chatUserRepository;

    public ResultVO<ChatUserDO> checkChatIdAndUserIdExist(Long chatId, Integer userId) {
        Optional<ChatUserDO> chatUserDOOptional = chatUserRepository.findFirstByChatIdAndUserId(chatId, userId);
        return chatUserDOOptional.map(ResultVO::new).orElseGet(() -> new ResultVO<>("您无法访问此会话"));
        /*Optional<ChatDO> chatDOOptional = chatRepository.findById(chatId);
        if (chatDOOptional.isPresent()) {
            ChatDO chatDO = chatDOOptional.get();

        }*/
    }
}
