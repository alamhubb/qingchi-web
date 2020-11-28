package com.qingchi.server.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2020-05-23 17:21
 */
@Data
public class TalkTabVO {
    //显示的名称，只用来显示，不做判断。
    private String name;
    //用来判断，首页、同城、关注
    private String type;
    private List<TalkVO> talks;
    private String loadMore = "more";

    public TalkTabVO(String name, String type) {
        this.name = name;
        this.type = type;
        this.talks = new ArrayList<>();
    }
}
