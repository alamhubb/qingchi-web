package com.qingchi.server.service;

import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.constant.ChatType;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.server.model.serviceResult.CreateSingleChatResult;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        /*List<ChatUserDO> chats2 = chatUserRepository.
                findByChatTypeNotInAndChatStatusAndUserIdAndStatusOrderByTopFlagDescChatTopLevelAscUpdateTimeDesc(
                        ChatType.systemChats, CommonStatus.normal, user.getId(), CommonStatus.normal);*/
        List<ChatUserDO> chats2 = new ArrayList<>();
        List<ChatVO> chatVOS2 = ChatVO.chatUserDOToVOS(chats2);
        chatVOS.addAll(chatVOS2);
        return chatVOS;
    }

    //未登录的情况下查询官方chat，官方群聊
    public List<ChatVO> getChats() {
        //未登录的情况只插叙你官方的chats
        List<ChatDO> chats1 = chatRepository.findByStatusAndTypeInOrderByTopFlagDescTopLevelAscUpdateTimeDesc(CommonStatus.normal, ChatType.systemChats);
        return ChatVO.chatDOToVOS(chats1);
    }
}
