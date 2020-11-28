package com.qingchi.server.model;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TalkDeleteVO {
    @NotNull
    private Integer talkId;
}