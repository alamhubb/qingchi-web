package com.qingchi.server.domain;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.*;
import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.model.user.UserContactDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.repository.follow.FollowRepository;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.base.utils.UserUtils;
import com.qingchi.server.service.ShellOrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
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

    //会话已开启，则可直接发起对话。
    //会华为开启，判断对方是否关注你。
    //未关注，提示开启。

    //否则可以直接进入chat页面。

    //对方关注了你，进入chat页面。有chat返回，没有创建并返回

    //支付创建的时候，需要判断是否已存在（你关注的对方，进入过这个界面，这个时候chat就创建了，所以你进来，他是待开启），所以不需要再次创建了

    @Transactional
    public ResultVO<ChatVO> payShellOpenChat(UserDO user, UserDO receiveUser, ChatUserDO chatUserDO) {
        //肯定不能通过 可用状态查询是否显示，
        //要有一个状态判断是否在前台显示，因为有时候开启了，但是前台不显示。你被对方开启

        ChatDO chatDO = null;
        //如果为空，则走创建逻辑,付费开启，chat直接开启
        ChatUserDO receiveChatUserDO = null;
        if (chatUserDO == null) {
            chatDO = new ChatDO(ChatType.single, CommonStatus.normal);
            //生成chat
            chatUserDO = new ChatUserDO(chatDO, user.getId(), receiveUser.getId());

            receiveChatUserDO = new ChatUserDO(chatDO, receiveUser.getId(), user.getId());
            //还需要发送一条消息，系统的消息。判断是，自己的这边提示，您付费开启了会话，对方付费开启了和您的会话。
        } else {
            //需要查询出来判断状态，区分返回不同的错误消息
            Optional<ChatDO> chatDOOptional = chatRepository.findById(chatUserDO.getChatId());
            if (!chatDOOptional.isPresent()) {
                QingLogger.logger.error("不存在的chat或者chat状态不正确");
                return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
            }

            chatDO = chatDOOptional.get();

            if (!chatDO.getStatus().equals(CommonStatus.waitOpen)) {
                return new ResultVO<>("会话已开启，请刷新后重试");
            }

            Optional<ChatUserDO> receiveChatUserDOOptional = chatUserRepository.findFirstByChatIdAndUserId(chatDO.getId(), receiveUser.getId());
            if (!receiveChatUserDOOptional.isPresent()) {
                QingLogger.logger.error("chatUser不存在或者状态错误");
                return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
            }

            receiveChatUserDO = receiveChatUserDOOptional.get();

            if (!receiveChatUserDO.getStatus().equals(CommonStatus.waitOpen)) {
                return new ResultVO<>("会话已开启，请刷新后重试");
            }

            Date curDate = new Date();
            //chat改为开启
            //开启chat
            //你需要自己的chat为代开起
            chatDO.setStatus(CommonStatus.normal);
            chatDO.setUpdateTime(curDate);

            //更改状态返回
            //开启自己的chatUser
            chatUserDO.setStatus(CommonStatus.normal);
            chatUserDO.setUpdateTime(curDate);
            //自己的要在前台显示，需要有一个状态控制是否前台显示
            chatUserDO.setFrontShow(true);
            //开启对方的chatUser
            receiveChatUserDO.setStatus(CommonStatus.normal);
            receiveChatUserDO.setUpdateTime(curDate);
            receiveChatUserDO.setFrontShow(true);
        }

        //返回
        shellOrderService.createAndSaveContactAndShellOrders(user, receiveUser, ExpenseType.openChat);

        chatDO = chatRepository.save(chatDO);

        chatUserDO.setLastContent("您付费开启了会话");
        chatUserDO.setOpenChatType(OpenChatType.payOpen);

        receiveChatUserDO.setLastContent("对方付费开启了和您的会话");
        receiveChatUserDO.setOpenChatType(OpenChatType.receivePayOpen);
        List<ChatUserDO> chatUserDOS = Arrays.asList(chatUserDO, receiveChatUserDO);
        chatUserRepository.saveAll(chatUserDOS);

        ChatVO chatVO = new ChatVO(chatDO, chatUserDO);
        //需要对方的用户名，昵称。会话未开启
        return new ResultVO<>(chatVO);
    }
}
