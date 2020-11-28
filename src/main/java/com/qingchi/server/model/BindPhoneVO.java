package com.qingchi.server.model;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * @author qinkaiyuan
 * @date 2019-02-14 22:03
 */
@Data
public class BindPhoneVO {
    @NotBlank
    @Length(min = 11, max = 11)
    private String phoneNum;
    @NotBlank
    @Length(min = 4, max = 4)
    private String authCode;
    private String inviteCode;
    private String platform;
}
