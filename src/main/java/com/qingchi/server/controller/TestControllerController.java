package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.GenderType;
import com.qingchi.base.constant.status.ContentStatus;
import com.qingchi.base.constant.status.ReportStatus;
import com.qingchi.base.repository.talk.TalkRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2019-09-28 11:09
 * 前端初始化内容
 */
@RestController
@RequestMapping("test")
public class TestControllerController {
    @Resource
    private TalkRepository talkRepository;

    @RequestMapping("")
    public ResultVO<Object> testUrl(String content) {
        List<Integer> ids = talkRepository.queryTalkIdsTop10ByGender(
                Collections.singletonList(0), null, ContentStatus.preAudit, ContentStatus.otherCanSeeContentStatus, GenderType.genders, PageRequest.of(0, 10));
        return new ResultVO<>(ids);
    }

}
