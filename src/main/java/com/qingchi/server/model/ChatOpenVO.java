package com.qingchi.server.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author qinkaiyuan
 * @date 2018-11-18 20:48
 */
@Data
public class ChatOpenVO {
    @NotNull
    private Long id;

    @NotNull
    private Boolean needPayOpen;
}
