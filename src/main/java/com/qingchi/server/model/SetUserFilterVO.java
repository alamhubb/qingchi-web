package com.qingchi.server.model;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author qinkaiyuan 查询结果可以没有set和空构造，前台传值可以没有get
 * @date 2019-08-13 23:34
 */
@Data
public class SetUserFilterVO {
    @NotBlank
    private String gender;
    @NotNull
    @Range(min = 0, max = 40, message = "参数异常")
    private Integer minAge;
    @NotNull
    @Range(min = 0, max = 40, message = "参数异常")
    private Integer maxAge;
}
