package com.qingchi.server.common;

import com.qingchi.server.model.TalkTabVO;

import java.util.Arrays;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2019-09-20 21:47
 */
public class TalkTabType {
    public static final String follow_name = "关注";
    public static final String follow_type = "follow";

    public static final String home_name = "首页";
    public static final String home_type = "home";

    public static final String city_name = "同城";
    public static final String city_type = "city";

    public static final List<TalkTabVO> talkTabs = Arrays.asList(new TalkTabVO(follow_name, follow_type), new TalkTabVO(home_name, home_type), new TalkTabVO(city_name, city_type));
}
