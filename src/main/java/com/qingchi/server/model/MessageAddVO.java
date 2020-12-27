package com.qingchi.server.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author qinkaiyuan
 * @date 2019-05-26 21:19
 */
@Data
public class MessageAddVO {
    @NotNull
    private Long chatId;

    @NotBlank
    private String content;
    //暂时未使用
    private Integer receiveUserId;
}
