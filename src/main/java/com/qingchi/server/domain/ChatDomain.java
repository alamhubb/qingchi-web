package com.qingchi.server.domain;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.NotifyType;
import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.model.notify.NotifyDO;
import com.qingchi.base.model.talk.CommentDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.repository.notify.NotifyRepository;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.service.NotifyService;
import com.qingchi.base.service.ReportService;
import com.qingchi.base.service.ViolationService;
import com.qingchi.server.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatDomain {

    @Resource
    private ChatUserRepository chatUserRepository;

    @Resource
    private ChatRepository chatRepository;

    @Resource
    private ChatService chatService;


}
