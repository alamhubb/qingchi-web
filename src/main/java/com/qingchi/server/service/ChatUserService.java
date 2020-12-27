package com.qingchi.server.service;

import com.qingchi.base.constant.status.ChatStatus;
import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.constant.ChatType;
import com.qingchi.base.model.user.UserDO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2019-06-16 12:39
 */
@Service
public class ChatUserService {
    @Resource
    ChatRepository chatRepository;
    @Resource
    ChatUserRepository chatUserRepository;

    //登录情况下查询用户有权限的chatuser
    public List<ChatVO> getChats(UserDO user) {
        //未登录的情况只插叙你官方的chats
        List<ChatVO> chatVOS = getChats();
        List<ChatUserDO> chats2 = chatUserRepository.findByChatStatusAndUserIdAndFrontShowTrueOrderByChatTopLevelDescTopFlagDescUpdateTimeDesc(ChatStatus.enable, user.getId());
        List<ChatVO> chatVOS2 = ChatVO.chatUserDOToVOS(chats2);
        chatVOS.addAll(chatVOS2);
        return chatVOS;
    }

    //未登录的情况下查询官方chat，官方群聊
    public List<ChatVO> getChats() {
        //未登录的情况只插叙你官方的chats
        List<ChatDO> chats1 = chatRepository.findByStatusAndTypeInOrderByTopLevelAscUpdateTimeDesc(ChatStatus.enable, ChatType.systemChats);
        return ChatVO.chatDOToVOS(chats1);
    }
}
