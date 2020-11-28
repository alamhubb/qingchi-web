package com.qingchi.server.model;


import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CommentDeleteVO {
    @NotNull
    private Integer commentId;
}
