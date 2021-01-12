package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.status.BaseStatus;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.modelVO.FollowAddVO;
import com.qingchi.base.model.user.FollowDO;
import com.qingchi.base.repository.follow.FollowRepository;
import com.qingchi.base.service.FollowService;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.base.utils.UserUtils;
import com.qingchi.server.common.FollowConst;
import com.qingchi.server.model.FollowUserVO;
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
@RequestMapping("follow")
public class FollowController {
    @Resource
    private FollowRepository followRepository;
    @Resource
    private UserRepository userRepository;
    @Resource
    private FollowService followService;

    @PostMapping("queryUserFollows")
    public ResultVO<Map<String, List<FollowUserVO>>> queryUserFollows(UserDO user) {
        Map<String, List<FollowUserVO>> map = new HashMap<>();
        //查询他的关注
        List<FollowDO> followDOS = followRepository.findTop30ByUserIdAndStatusOrderByUpdateTimeDesc(user.getId(), BaseStatus.enable);
        List<UserDO> userDOS = followDOS.stream().map(followDO -> UserUtils.get(followDO.getBeUserId())).collect(Collectors.toList());
        List<FollowUserVO> followUserVOS = userDOS.stream().map(userDO -> {
            //查看对方是否也关注了自己
            Integer followCount = followRepository.countByUserIdAndBeUserIdAndStatus(userDO.getId(), user.getId(), BaseStatus.enable);
            //默认显示已关注
            String followStatus = FollowConst.followed;
            //默认对方没有关注自己
            boolean beFollow = false;
            if (followCount > 0) {
                followStatus = FollowConst.eachFollow;
                beFollow = true;
            }
            return new FollowUserVO(userDO, followStatus, beFollow);
        }).collect(Collectors.toList());

        //查询他的粉丝
        List<FollowDO> fans = followRepository.findTop30ByUserIdAndStatusOrderByUpdateTimeDesc(user.getId(), BaseStatus.enable);
        List<UserDO> fansUserDOS = fans.stream().map(followDO -> UserUtils.get(followDO.getUserId())).collect(Collectors.toList());
        List<FollowUserVO> fansUserVOS = fansUserDOS.stream().map(userDO -> {
            //查看自己是否关注了对方
            Integer followCount = followRepository.countByUserIdAndBeUserIdAndStatus(user.getId(), userDO.getId(), BaseStatus.enable);
            //默认显示关注
            String followStatus = FollowConst.follow;
            //如果自己关注了对方，就是互相关注
            if (followCount > 0) {
                followStatus = FollowConst.eachFollow;
            }
            //对方是自己的粉丝，一定关注了自己
            return new FollowUserVO(userDO, followStatus, true);
        }).collect(Collectors.toList());
        map.put("follows", followUserVOS);
        map.put("fans", fansUserVOS);
        return new ResultVO<>(map);
    }


    @PostMapping("addFollow")
    public ResultVO<?> addFollow(@RequestBody @Valid @NotNull FollowAddVO addVO, UserDO user) {
        Optional<UserDO> userDOOptional = userRepository.findById(addVO.getBeUserId());
        if (!userDOOptional.isPresent()) {
            return new ResultVO<>("无法关注不存在的用户");
        }
        UserDO beUserDO = userDOOptional.get();
        //自己关注自己
        if (beUserDO.getId().equals(user.getId())) {
            return new ResultVO<>("不能自己关注自己哦");
        }
        return followService.addFlow(user, beUserDO);
    }


    @PostMapping("cancelFollow")
    public ResultVO cancelFollow(@RequestBody @Valid @NotNull FollowAddVO addVO, UserDO user) {
        Optional<UserDO> userDOOptional = userRepository.findById(addVO.getBeUserId());
        if (!userDOOptional.isPresent()) {
            return new ResultVO<>("无法取消关注不存在的用户");
        }
        UserDO beUserDO = userDOOptional.get();
        //自己关注自己
        if (beUserDO.getId().equals(user.getId())) {
            return new ResultVO<>("不能自己取消关注自己哦");
        }
        Optional<FollowDO> followDOOptional = followRepository.findFirstByUserIdAndBeUserIdOrderByIdDesc(user.getId(), beUserDO.getId());
        FollowDO followDO;
        //如果已经关注了
        if (followDOOptional.isPresent()) {
            followDO = followDOOptional.get();
            //已经关注
            if (BaseStatus.enable.equals(followDO.getStatus())) {
                followDO.setStatus(BaseStatus.delete);
                user.setFollowNum(user.getFollowNum() - 1);
                beUserDO.setFansNum(beUserDO.getFansNum() - 1);
                followDO.setUpdateTime(new Date());
                followRepository.save(followDO);
                return new ResultVO<>();
            }
        }
        QingLogger.logger.warn("系统异常，用户{}取消关注了没有关注的用户{}", user.getId(), beUserDO.getId());
        return new ResultVO<>("并没有关注此用户");
    }
}
