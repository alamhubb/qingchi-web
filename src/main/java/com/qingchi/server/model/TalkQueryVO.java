package com.qingchi.server.model;

import lombok.Data;

import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2019-08-21 20:47
 */
@Data
public class TalkQueryVO {
    private List<Integer> talkIds;
    private String homeType;
    private List<Integer> tagIds;

    private String adCode;
    private Double lon;
    private Double lat;

    private Integer minAge;
    private Integer maxAge;
    private String gender;
    private Boolean openPosition;
    private String platform;
    private String standby;
}
