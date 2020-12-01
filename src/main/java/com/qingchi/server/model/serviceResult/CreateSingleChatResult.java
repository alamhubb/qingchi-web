package com.qingchi.server.model.serviceResult;

import com.qingchi.base.model.chat.ChatDO;
import com.qingchi.base.model.chat.ChatUserDO;
import lombok.Data;

@Data
public class CreateSingleChatResult {
    private ChatDO chat;
    private ChatUserDO minChatUser;
    private ChatUserDO receiveChatUser;

    public CreateSingleChatResult(ChatDO chat, ChatUserDO minChatUser, ChatUserDO receiveChatUser) {
        this.chat = chat;
        this.minChatUser = minChatUser;
        this.receiveChatUser = receiveChatUser;
    }
}
