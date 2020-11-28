package com.qingchi.server.model;

import com.qingchi.base.model.talk.TagDO;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author qinkaiyuan
 * @date 2019-11-07 15:20
 */

@Data
public class TalkTagVO {
    public Integer id;
    public String name;

    public TalkTagVO() {
    }

    public TalkTagVO(TagDO tagDO) {
        this.id = tagDO.getId();
        this.name = StringUtils.substring(tagDO.getName(), 0, 4);
    }

    public static List<TalkTagVO> tagDOToVOS(List<TagDO> DOs) {
        return DOs
                .stream()
                .map(
                        TalkTagVO::new)
                .collect(Collectors.toList());
    }
}
