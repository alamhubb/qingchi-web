package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.HomeType;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.store.TalkQueryRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.server.service.TalkQueryService;
import com.qingchi.server.model.TalkQueryVO;
import com.qingchi.server.model.TalkVO;
import com.qingchi.server.model.UserTalkQueryVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("talk")
public class QueryTalkController {
    @Resource
    private TalkRepository talkRepository;
    @Resource
    private TalkQueryRepository talkQueryRepository;
    @Resource
    private UserRepository userRepository;
    @Resource
    private TalkQueryService talkQueryService;

    /**
     * 有个初始查询全部的，还有查单个的
     *
     * @param user
     * @param queryVO
     * @return
     */
    @PostMapping("queryOtherHomeTypeTalks")
    public ResultVO<?> queryOtherHomeTypeTalks(UserDO user, @RequestBody @Valid @NotNull(message = "参数异常") TalkQueryVO queryVO) {
        String curHomeType = queryVO.getHomeType();
        List<String> homeTypes = HomeType.homeTypes;
        List<Map<String, List<TalkVO>>> talkMaps = new ArrayList<>();
        for (String homeType : homeTypes) {
            //user不为空或者查询的不为关注，当前home不为已选home
            if (!curHomeType.equals(homeType) && (user != null || !homeType.equals(HomeType.follow_name))) {
                Map<String, List<TalkVO>> map = new HashMap<>();
                queryVO.setHomeType(homeType);
                ResultVO<List<TalkVO>> resultVO = talkQueryService.queryTalkListParamHandle(user, queryVO);
                if (resultVO.hasError()) {
                    return resultVO;
                }
                map.put(homeType, resultVO.getData());
                talkMaps.add(map);
            }
        }
        //查询其他的全部前10条内容
        //传入当前的hometype
        //还有其他条件
        return new ResultVO<>(talkMaps);
    }

    //@RequestBody(required = false) TalkFilter query
    @PostMapping("queryTalks")
    public ResultVO<?> queryTalkList(UserDO user, @RequestBody @Valid @NotNull(message = "参数异常") TalkQueryVO queryVO) {
        return talkQueryService.queryTalkListParamHandle(user, queryVO);
    }

    @PostMapping("queryUserTalks")
    public ResultVO<List<TalkVO>> queryUserTalks(@RequestBody @Valid @NotNull(message = "参数异常") UserTalkQueryVO queryVO, UserDO user) {
        Optional<UserDO> userOptional = userRepository.findById(queryVO.getUserId());
        if (!userOptional.isPresent()) {
            return new ResultVO<>("不存在的用户");
        }
        UserDO detailUser = userOptional.get();
        List<TalkDO> talks;
        if (user != null && detailUser.getId().equals(user.getId())) {
            talks = talkQueryRepository.queryTalksTop10ByMine(queryVO.getTalkIds(), detailUser.getId());
        } else {
            talks = talkQueryRepository.queryTalksTop10ByUser(queryVO.getTalkIds(), detailUser.getId());
        }
        return new ResultVO<>(talks.stream().map(talkDO -> new TalkVO(user, talkDO)).collect(Collectors.toList()));
//        return queryService.queryTalkListByFilter(query);
        /*if (homeType == HomeTypeEnum.FOLLOW.getValue()) {
            //为关注页
            return queryService.queryFollowTalkList();
        } else if (homeType == HomeTypeEnum.MATCH.getValue()) {
            //为匹配页
            return queryService.queryMatchTalkList();
        } else {
            //为全部页
        }*/
    }

    @PostMapping("queryTalkDetail")
    public ResultVO<TalkVO> queryTalkList(@Valid @NotNull Integer talkId, UserDO user) {
        Optional<TalkDO> optionalTalkDO = talkRepository.findOneByIdAndStatusIn(talkId, CommonStatus.selfCanSeeContentStatus);
        if (optionalTalkDO.isPresent()) {
            TalkDO talkDO = optionalTalkDO.get();
            return new ResultVO<>(new TalkVO(user, talkDO, true));
        }
        return new ResultVO<>("无法查看不存在的动态");
//        return queryService.queryTalkListByFilter(query);
        /*if (homeType == HomeTypeEnum.FOLLOW.getValue()) {
            //为关注页
            return queryService.queryFollowTalkList();
        } else if (homeType == HomeTypeEnum.MATCH.getValue()) {
            //为匹配页
            return queryService.queryMatchTalkList();
        } else {
            //为全部页
        }*/
    }
}
