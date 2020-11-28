package com.qingchi.server.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author qinkaiyuan
 * @date 2019-05-26 21:19
 */
@Data
public class MessageAddVO {
    private Long chatUserId;
    private Long chatId;
    private Integer receiveUserId;
    @NotBlank
    private String content;
}
