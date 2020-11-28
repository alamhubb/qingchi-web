package com.qingchi.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingchi.base.domain.ReportDomain;
import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.config.redis.RedisSubListenerConfig;
import com.qingchi.base.modelVO.MessageVO;
import com.qingchi.base.modelVO.NotifyVO;
import com.qingchi.base.constant.*;
import com.qingchi.base.model.chat.MessageDO;
import com.qingchi.base.model.chat.MessageReceiveDO;
import com.qingchi.base.repository.chat.MessageReceiveRepository;
import com.qingchi.base.repository.chat.MessageRepository;
import com.qingchi.base.model.notify.NotifyDO;
import com.qingchi.base.repository.notify.NotifyRepository;
import com.qingchi.base.service.NotifyService;
import com.qingchi.base.platform.qq.QQUtil;
import com.qingchi.base.platform.weixin.HttpResult;
import com.qingchi.base.platform.weixin.WxUtil;
import com.qingchi.base.service.ReportService;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.utils.JsonUtils;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.MessageAddVO;
import com.qingchi.server.model.MessageQueryVO;
import com.qingchi.server.model.MsgDeleteVO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author qinkaiyuan
 * @date 2018-11-18 20:45
 */

@RestController
@RequestMapping("message")
public class MessageController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Resource
    private UserRepository userRepository;
    @Resource
    private ChatRepository chatRepository;
    @Resource
    private MessageRepository messageRepository;
    @Resource
    private NotifyService notifyService;
    @Resource
    private NotifyRepository notifyRepository;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ChatUserRepository chatUserRepository;
    @Resource
    private MessageReceiveRepository messageReceiveDORepository;
    @Resource
    private ReportService reportService;
    @Resource
    private ReportDomain reportDomain;

    /**
     * toDO 这里有问题，都统一用的 msgid
     *
     * @param user
     * @param queryVO
     * @return
     */
    @PostMapping("queryMessages")
    public ResultVO<List<MessageVO>> queryMessages(UserDO user, @RequestBody @Valid MessageQueryVO queryVO) {
        List<MessageVO> messageVOS = new ArrayList<>();
        Long chatId = queryVO.getChatId();
        //msg id也要统一，举报的时候不知道是哪个
        List<Long> msgIds = queryVO.getMsgIds();
        ChatDO chatDO = null;
        //前台没传这个值
        if (chatId == null) {
            if (msgIds.size() > 0) {
                Optional<MessageDO> messageDOOptional = messageRepository.findById(msgIds.get(0));
                if (messageDOOptional.isPresent()) {
                    Optional<ChatDO> chatDOOptional = chatRepository.findById(messageDOOptional.get().getChatId());
                    if (chatDOOptional.isPresent()) {
                        chatDO = chatDOOptional.get();
                    }

                }
            }
        } else {
            Optional<ChatDO> chatDOOptional = chatRepository.findById(chatId);
            if (chatDOOptional.isPresent()) {
                chatDO = chatDOOptional.get();
            }
        }
        if (chatDO != null) {
            List<MessageDO> messageDOS = messageRepository.findTop30ByChatIdAndStatusInAndIdNotInOrderByCreateTimeDescIdDesc(chatDO.getId(), CommonStatus.otherCanSeeContentStatus, msgIds);
            messageVOS = MessageVO.messageDOToVOS(messageDOS);
        }
        return new ResultVO<>(messageVOS);
    }


    @PostMapping("sendMsg")
    public ResultVO<MessageVO> sendMsg(UserDO user, @RequestBody MessageAddVO msgAddVO) throws IOException {
        Long chatId = msgAddVO.getChatId() != null ? msgAddVO.getChatId() : msgAddVO.getChatUserId();
        String talkContent = msgAddVO.getContent();
        if (StringUtils.isEmpty(talkContent)) {
            return new ResultVO<>("不能发布空内容");
        }
        if (StringUtils.isEmpty(user.getPhoneNum())) {
            QingLogger.logger.error("用户未绑定手机号还能调用后台发布功能，用户Id：{}", user.getId());
            return new ResultVO<>(ErrorMsg.bindPhoneNumCan);
        }

        if (!CommonStatus.canPublishContentStatus.contains(user.getStatus())) {
            return new ResultVO<>(ErrorMsg.userMaybeViolation);
        }


        Optional<ChatDO> chatDOOptional = chatRepository.findFirstByIdAndStatus(chatId, CommonStatus.normal);
        if (!chatDOOptional.isPresent()) {
            log.error("被攻击了，出现了不存在的消息:{}", chatId);
            return new ResultVO<>("该聊天不存在");
        }

        ChatDO chat = chatDOOptional.get();
        //生成消息，先不管群聊只管私聊，私聊会有receiveUser，用来判断已读未读
        //只管群聊
        //查看chat的类型
        if (chat.getType().equals(ChatType.system_group)) {
            //不为空才进行校验
            if (StringUtils.isNotEmpty(talkContent)) {
                if (UserUpdateController.checkHasIllegals(talkContent)) {
                    return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                }
                HttpResult wxResult = WxUtil.checkContentWxSec(talkContent);
                if (wxResult.hasError()) {
                    return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                }
                HttpResult qqResult = QQUtil.checkContentQQSec(talkContent);
                if (qqResult.hasError()) {
                    return new ResultVO<>(ErrorMsg.CHECK_VIOLATION_ERR_MSG);
                }
            }

            //不校验权限，只要是系统用户就可以发送
            MessageDO message = messageRepository.save(new MessageDO(chat.getId(), msgAddVO.getContent(), user.getId()));
            //校验是否触发关键词，如果触发生成举报，修改动态为预审查，只能用户自己可见
            reportDomain.checkKeywordsCreateReport(message);

            NotifyVO notifyVO = new NotifyVO(chat, user, message);
            //如果官方群聊，则给所有人发送信息
            try {
                stringRedisTemplate.convertAndSend(RedisSubListenerConfig.allUserKey, JsonUtils.objectMapper.writeValueAsString(notifyVO));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return new ResultVO<>(new MessageVO(message, 1));
            //不为官方群聊
        } else {
            //如果是官方通知和
            //如果为官方群聊，则所有人都可以发送内容
            //查询用户是否有权限往chat中发送内容
            Optional<ChatUserDO> chatUserDOOptional = chatUserRepository.findFirstByChatIdAndUserIdAndStatus(chatId, user.getId(), CommonStatus.normal);
            if (!chatUserDOOptional.isPresent()) {
                log.error("用户已经被踢出来了，不具备给这个chat发送消息的权限");
                //用户给自己被踢出来，或者自己删除的内容发消息。提示异常
                return new ResultVO<>("聊天已关闭，请刷新后重试");
            }
            String content = msgAddVO.getContent();
            //构建消息
            MessageDO message = messageRepository.save(new MessageDO(chat.getId(), content, user.getId()));

            Date curDate = new Date();
            chat.setUpdateTime(curDate);
            chat.setLastContent(content);
            chatRepository.save(chat);

            List<NotifyDO> notifies = new ArrayList<>();
            //有权限，则给chat中的所有用户发送内容

            List<ChatUserDO> chatUserDOS = chatUserRepository.findByChatIdAndStatus(chatId, CommonStatus.normal);
            //包含自己，起码要有两个人
            if (chatUserDOS.size() <= 1) {
                //用户给自己被踢出来，或者自己删除的内容发消息。提示异常
                return new ResultVO<>("对方已退出聊天");
            }
            MessageReceiveDO mineMessageUser = new MessageReceiveDO();
            //发送消息
            for (ChatUserDO chatUserDO : chatUserDOS) {
                chatUserDO.setLastContent(message.getContent());
                chatUserDO.setUpdateTime(new Date());
                //如果为匹配chat，且为待匹配状态
                if (ChatType.match.equals(chat.getType()) && CommonStatus.waitMatch.equals(chat.getMatchStatus()) && chatUserDO.getStatus().equals(CommonStatus.waitMatch)) {
                    //则将用户的chat改为匹配成功
                    chatUserDO.setStatus(ChatUserStatus.enable);
                }
                MessageReceiveDO messageReceiveDO = new MessageReceiveDO(chatUserDO.getId(), user.getId(), chatUserDO.getReceiveUserId(), message.getId());
                Integer chatUserId = chatUserDO.getUserId();
                //自己的话不发送通知，自己的话也要构建消息，要不看不见，因为读是读这个表
                if (chatUserId.equals(user.getId())) {
                    messageReceiveDO.setIsMine(true);
                    messageReceiveDO.setIsRead(true);
                    mineMessageUser = messageReceiveDORepository.save(messageReceiveDO);
                } else {
                    //别人的chatUser，要增加未读，自己刚发的消息，别人肯定还没看
                    chatUserDO.setUnreadNum(chatUserDO.getUnreadNum() + 1);
                    NotifyDO notifyDO = notifyRepository.save(new NotifyDO(messageReceiveDORepository.save(messageReceiveDO)));
                    notifies.add(notifyDO);
                }
            }
            notifyRepository.saveAll(notifies);
            //保存message
            notifyService.sendNotifies(notifies, user);
            //只需要返回自己的
            return new ResultVO<>(new MessageVO(mineMessageUser, message));
        }
    }

    @PostMapping("deleteMsg")
    @ResponseBody
    public ResultVO<Object> deleteMsg(UserDO user, @RequestBody @Valid MsgDeleteVO msgVO) {
        /**
         * 删除动态操作，
         * 如果是系统管理员删除动态，则必须填写原因，删除后发表动态的用户将被封禁
         * 如果是自己删的自己的动态，则不需要填写原因，默认原因是用户自己删除
         */
        Optional<MessageDO> optionalMsgDO = messageRepository.findFirstOneByIdAndStatusIn(msgVO.getMsgId(), CommonStatus.otherCanSeeContentStatus);
        if (!optionalMsgDO.isPresent()) {
            return new ResultVO<>("无法删除不存在的消息");
        }
        MessageDO msgDO = optionalMsgDO.get();
        //如果是系统管理员操作,则把发表动态的用户封禁，如果用户本身也是管理员则不封禁，存在管理员自己删自己的情况
        if (UserType.system.equals(user.getType())) {
            //管理员不能删除内容了
            /*if (StringUtils.isEmpty(msgVO.getDeleteReason())) {
                return new ResultVO<>("必须填写删除原因");
            }
            //改为删除状态，和写上删除原因
            msgDO.setStatus(CommonStatus.delete);
            msgDO.setDeleteReason(msgVO.getDeleteReason());
            Date curDate = new Date();
            msgDO.setUpdateTime(curDate);
            UserDO violationUser = msgDO.getUser();
            //不为管理员自己删自己
            //且封禁撞他不为空，且为封禁，才执行封禁用户操作
            if (!ObjectUtils.isEmpty(msgVO.getViolation()) && msgVO.getViolation()) {
                violationUser.setStatus(CommonStatus.violation);
                msgDO.setStatus(CommonStatus.violation);
                //如果封禁的话，要改一下删除原因
                String deleteReason = "账号违规被封禁，请联系客服处理，删除原因：" + msgVO.getDeleteReason();
                //修改 D O的删除原因
                msgDO.setDeleteReason(deleteReason);
                violationUser.setViolationReason(msgVO.getDeleteReason());
                violationUser.setViolationCount(violationUser.getViolationCount() + 1);
                violationUser.setUpdateTime(curDate);
            }
            //给用户发送被封通知
            NotifyDO notifyDO = new NotifyDO(user, violationUser, NotifyType.delete_msg);
//            toDO notifyDO.setMessage(msgDO);
            notifyDO = notifyRepository.save(notifyDO);
            //推送消息
            notifyService.sendNotifies(Collections.singletonList(notifyDO));*/
            //不是管理员的话就必须是自己删除自己
        } else {
            //是否是自己删除自己的动态
            if (!msgDO.getUserId().equals(user.getId())) {
                QingLogger.logger.warn("有人尝试删除不属于自己的消息,用户名:{},id:{},尝试删除msgId：{}", user.getNickname(), user.getId(), msgDO.getId());
                return new ResultVO<>("系统异常，无法删除不属于自己的动态");
            }
            msgDO.setStatus(CommonStatus.delete);
            msgDO.setDeleteReason("用户自行删除");
        }
        msgDO.setUpdateTime(new Date());
        messageRepository.save(msgDO);
        return new ResultVO<>();
    }
}
