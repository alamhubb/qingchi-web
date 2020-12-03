package com.qingchi.server.service;

import com.qingchi.base.constant.ChatStatus;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.constant.ChatType;
import com.qingchi.base.constant.ChatUserStatus;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.follow.FollowRepository;
import com.qingchi.server.model.serviceResult.CreateSingleChatResult;
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
    @Resource
    FollowRepository followRepository;

    //登录情况下查询用户有权限的chatuser
    public CreateSingleChatResult createSingleChat(UserDO user, UserDO receiveUser) {
        ChatDO chat = new ChatDO(ChatType.single);

        //生成chat
        chat = chatRepository.save(chat);

        //match属于私聊，需要保存对方的内容，方便展示头像昵称
        ChatUserDO mineChatUser = new ChatUserDO(chat, user.getId(), receiveUser.getId());
        //自己的设置为待匹配状态，需要等对方回复后才能改为正常

        //查看对方是否也关注了自己
        Integer receiveFollowCount = followRepository.countByUserIdAndBeUserIdAndStatus(receiveUser.getId(), user.getId(), CommonStatus.normal);
        //如果对方未关注自己，则不允许直接向对方发送消息，则改为待开启
        mineChatUser.setUnreadNum(0);
        if (receiveFollowCount < 1) {
            //设置为待开启，需要一方发送消息后改为开启
            mineChatUser.setStatus(CommonStatus.waitOpen);
            mineChatUser.setLastContent("会话未开启");
        }

        ChatUserDO receiveChatUser = new ChatUserDO(chat.getId(), receiveUser.getId(), user.getId(), chat.getType());
        receiveChatUser.setUnreadNum(0);
        //查看自己是否也关注了对方
        Integer requestFollowCount = followRepository.countByUserIdAndBeUserIdAndStatus(user.getId(), receiveUser.getId(), CommonStatus.normal);
        //如果自己未关注对方，则对方不允许直接发送消息，改为待开启
        if (requestFollowCount < 1) {
            //设置为待开启，需要一方发送消息后改为开启
            receiveChatUser.setStatus(CommonStatus.waitOpen);
            receiveChatUser.setLastContent("会话未开启");
        }
        System.out.println(123);

        List<ChatUserDO> chatUserDOS = Arrays.asList(mineChatUser, receiveChatUser);
        chatUserRepository.saveAll(chatUserDOS);

        //你需要自己的chat为代开起
        //需要对方的用户名，昵称。会话未开启
        return new CreateSingleChatResult(chat, mineChatUser, receiveChatUser);
    }


}
