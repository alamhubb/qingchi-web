package com.qingchi.server.model;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * @author qinkaiyuan
 * @date 2019-02-14 22:03
 */
@Data
public class SendAuthCodeVO {
    @NotBlank
    @Length(min = 11, max = 11)
    private String phoneNum;

    public SendAuthCodeVO(@NotBlank @Length(min = 11, max = 11) String phoneNum) {
        this.phoneNum = phoneNum;
    }
}
