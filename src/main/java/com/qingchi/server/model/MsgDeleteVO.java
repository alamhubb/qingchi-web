package com.qingchi.server.model;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class MsgDeleteVO {
    @NotNull
    private Long msgId;
    private String deleteReason;
    private Boolean violation;
}