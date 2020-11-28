package com.qingchi.server.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户支付vo
 *
 * @author qinkaiyuan
 * @since 1.0.0
 */
@Data
public class UserPayVO {
    @NotBlank
    private String provider;
    private String platform;
    private Integer amount;
    private String payType;
}