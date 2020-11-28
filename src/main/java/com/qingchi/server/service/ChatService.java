package com.qingchi.server.service;

import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.constant.ChatType;
import com.qingchi.base.constant.ChatUserStatus;
import com.qingchi.base.model.user.UserDO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2019-06-16 12:39
 */
@Service
public class ChatService {
    @Resource
    ChatRepository chatRepository;
    @Resource
    ChatUserRepository chatUserRepository;

    //登录情况下查询用户有权限的chatuser
    public ChatVO createChat(UserDO user, UserDO receiveUser) {
        ChatDO chat = new ChatDO(ChatType.single);
        //match属于私聊，需要保存对方的内容，方便展示头像昵称
        ChatUserDO mineChatUser = new ChatUserDO(chat.getId(), user.getId(), receiveUser.getId(), chat.getType());
        //自己的设置为待匹配状态，需要等对方回复后才能改为正常
        mineChatUser.setStatus(ChatUserStatus.enable);
        ChatUserDO receiveChatUser = new ChatUserDO(chat.getId(), receiveUser.getId(), user.getId(), chat.getType());
        List<ChatUserDO> chatUserDOS = Arrays.asList(mineChatUser, receiveChatUser);
        chatUserRepository.saveAll(chatUserDOS);

        //生成chat
        chat = chatRepository.save(chat);
        return new ChatVO(chat);
    }
}
