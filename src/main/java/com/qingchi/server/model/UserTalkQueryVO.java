package com.qingchi.server.model;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2019-10-28 16:11
 */
@Data
public class UserTalkQueryVO {
    @NotNull(message = "入参为空异常")
    private Integer userId;
    @NotEmpty(message = "参数异常")
    private List<Integer> talkIds;
}
