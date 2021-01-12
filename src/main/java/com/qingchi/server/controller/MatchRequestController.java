package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.constant.ErrorMsg;
import com.qingchi.base.constant.MatchType;
import com.qingchi.base.constant.status.BaseStatus;
import com.qingchi.base.constant.status.UserStatus;
import com.qingchi.base.entity.UserImgUtils;
import com.qingchi.base.model.match.MatchRequestDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.UserImgDO;
import com.qingchi.base.platform.tencent.TencentCloud;
import com.qingchi.base.repository.chat.ChatRepository;
import com.qingchi.base.repository.match.MatchRequestRepository;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.service.FollowService;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.check.ModelContentCheck;
import com.qingchi.server.service.MatchRequestService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.faceid.v20180301.FaceidClient;
import com.tencentcloudapi.faceid.v20180301.models.LivenessCompareRequest;
import com.tencentcloudapi.faceid.v20180301.models.LivenessCompareResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("match")
public class MatchRequestController {
    @Resource
    private MatchRequestRepository requestRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private ChatRepository chatRepository;
    @Resource
    private FollowService followService;
    @Resource
    private MatchRequestService matchRequestService;
    @Value("${config.qq.cos.region}")
    private String region;

    @Resource
    private ModelContentCheck modelContentCheck;

    @PostMapping("likeMatchUser")
    public ResultVO<?> likeMatch(UserDO user, @Valid @NotNull Integer userId) throws Exception {
        ResultVO resultVO = modelContentCheck.checkUser(user);
        //校验内容是否违规
        if (resultVO.hasError()) {
            return new ResultVO<>(resultVO);
        }

        List<UserImgDO> userImgDOS = UserImgUtils.getImgs(user.getId());
        //查询出来就是看过，喜欢，不喜欢
        if (userImgDOS != null && userImgDOS.size() > 0) {
            if (user.getId().equals(userId)) {
                return new ResultVO<>("不许耍赖哦，不可以自己喜欢自己");
            }
            //判断用户是否开着匹配功能
            //只要开着匹配功能就可以匹配，如果不满足匹配条件，肯定会关闭匹配功能，所以只需要判断这个
            //打开了匹配开关，才可以显示喜欢按钮？本来想是喜欢按钮，然后提示
            //未打开的情况点击喜欢，提示未打开匹配开关，是否打开匹配开关并喜欢对方，确认取消，
            //然后先打开匹配开关，打开成功后点击喜欢。
            //然后点击喜欢，提示未过允许匹配时间，禁止匹配，可以给匹配按钮换个颜色
//        if (user.isOpenMatch()) {
            Optional<UserDO> receiveUserOpt = userRepository.findById(userId);
            //被喜欢的用户不为空
            if (receiveUserOpt.isPresent()) {
                //如果当前用户，或者对方用户，当前不是匹配打开状态
                //没打开的时候，点了匹配按钮，隐藏匹配按钮的，如果当前用户没打开的话，就是虚假请求
                //如果对方用户没打开的话，就是当前展示的已经过期了，当前用户已经被别人抢走了，你下手晚了
                //查询这个人之前是否已经喜欢了他
                UserDO receiveUser = receiveUserOpt.get();
                //如果被喜欢的用户开着匹配则继续执行
//                if (receiveUser.isOpenMatch()) {
                //查询被喜欢的用户是否喜欢了自己
                MatchRequestDO matchRequest = requestRepository.queryMatchRequestByUserIdAndReceiveUserIdAndStatus(receiveUser.getId(), user.getId(), MatchType.like);
                //因为查询时候会改为不喜欢，所以记录已经存在，所以需要先查询出来
                MatchRequestDO likeRequest = requestRepository.queryMatchRequestByUserIdAndReceiveUserId(user.getId(), receiveUser.getId());
                //新建喜欢请求
                //没有的话新建属性
                if (likeRequest == null) {
                    likeRequest = new MatchRequestDO();
                    likeRequest.setUserId(user.getId());
                    likeRequest.setReceiveUserId(receiveUser.getId());
                    likeRequest.setCreateTime(new Date());
                } else {
                    if (likeRequest.getStatus().equals(MatchType.like)) {
                        return new ResultVO<>("已经喜欢过了，请等待对方响应");
                    }
                }
                //已经存在的话，修改部分属性
                likeRequest.setUpdateTime(new Date());
                likeRequest.setStatus(MatchType.like);
                //将状态改为喜欢，保存，但是返回值没用到
                likeRequest = requestRepository.save(likeRequest);
                //用户被喜欢次数加1，
                receiveUser.setLikeCount(receiveUser.getLikeCount() + 1);
                receiveUser = userRepository.save(receiveUser);
                //关注此用户
                followService.addFlow(user, receiveUser);
                //如果这个人也已经喜欢了他,则两个人改为待匹配成功状态
                if (matchRequest != null) {
                    //匹配成功，给两个人创建chat、发送匹配成功消息
                    matchRequestService.sendMatchSuccessMsgToUser(user, receiveUser);
                }
            } else {
                // 对方已经关闭了匹配，可能和别人匹配成功了，或者关闭了匹配功能
                throw new Exception("喜欢了不存在的用户");
            }
            /*} else {
                //没打开匹配，还可以点击肯定是虚假请求
                throw new Exception("喜欢了不存在的用户");
            }*/
            /*} else {
                //没打开匹配，还可以点击肯定是虚假请求
                throw new Exception("不打开匹配无法喜欢，有人模拟请求");
            }*/
            return new ResultVO<>();
        } else {
            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("unlikeMatchUser")
    public ResultVO<?> unlikeMatchUser(UserDO user, @Valid @NotNull Integer userId) throws Exception {
        ResultVO resultVO = modelContentCheck.checkUser(user);
        //校验内容是否违规
        if (resultVO.hasError()) {
            return new ResultVO<>(resultVO);
        }

        List<UserImgDO> userImgDOS = UserImgUtils.getImgs(user.getId());
        //查询出来就是看过，喜欢，不喜欢
        if (userImgDOS != null && userImgDOS.size() > 0) {
            if (user.getId().equals(userId)) {
                return new ResultVO<>("不许耍赖哦，不可以自己喜欢自己");
            }
            //判断用户是否开着匹配功能
            //只要开着匹配功能就可以匹配，如果不满足匹配条件，肯定会关闭匹配功能，所以只需要判断这个
            //打开了匹配开关，才可以显示喜欢按钮？本来想是喜欢按钮，然后提示
            //未打开的情况点击喜欢，提示未打开匹配开关，是否打开匹配开关并喜欢对方，确认取消，
            //然后先打开匹配开关，打开成功后点击喜欢。
            //然后点击喜欢，提示未过允许匹配时间，禁止匹配，可以给匹配按钮换个颜色
//        if (user.isOpenMatch()) {
            Optional<UserDO> receiveUserOpt = userRepository.findById(userId);
            //被喜欢的用户不为空
            if (receiveUserOpt.isPresent()) {
                //如果当前用户，或者对方用户，当前不是匹配打开状态
                //没打开的时候，点了匹配按钮，隐藏匹配按钮的，如果当前用户没打开的话，就是虚假请求
                //如果对方用户没打开的话，就是当前展示的已经过期了，当前用户已经被别人抢走了，你下手晚了
                //查询这个人之前是否已经喜欢了他
                UserDO receiveUser = receiveUserOpt.get();
                //如果被喜欢的用户开着匹配则继续执行
//                if (receiveUser.isOpenMatch()) {
                //因为查询时候会改为不喜欢，所以记录已经存在，所以需要先查询出来
                MatchRequestDO likeRequest = requestRepository.queryMatchRequestByUserIdAndReceiveUserId(user.getId(), receiveUser.getId());
                //新建喜欢请求
                //没有的话新建属性
                if (likeRequest == null) {
                    likeRequest = new MatchRequestDO();
                    likeRequest.setUserId(user.getId());
                    likeRequest.setReceiveUserId(receiveUser.getId());
                    likeRequest.setCreateTime(new Date());
                } else {
                    if (likeRequest.getStatus().equals(MatchType.ilike)) {
                        QingLogger.logger.error("异常分支不应该走到这里不喜欢用户：{}", user.getId());
                        return new ResultVO<>("已经操作过了");
                    }
                }
                //已经存在的话，修改部分属性
                likeRequest.setUpdateTime(new Date());
                likeRequest.setStatus(MatchType.unlick);
                //将状态改为喜欢，保存，但是返回值没用到
                likeRequest = requestRepository.save(likeRequest);
            } else {
                // 对方已经关闭了匹配，可能和别人匹配成功了，或者关闭了匹配功能
                throw new Exception("不喜欢了不存在的用户");
            }
            /*} else {
                //没打开匹配，还可以点击肯定是虚假请求
                throw new Exception("喜欢了不存在的用户");
            }*/
            /*} else {
                //没打开匹配，还可以点击肯定是虚假请求
                throw new Exception("不打开匹配无法喜欢，有人模拟请求");
            }*/
            return new ResultVO<>();
        } else {
            return new ResultVO<>(ErrorCode.SYSTEM_ERROR);
        }
    }


    @PostMapping("userRealAuth")
    public ResultVO<?> userRealAuth(UserDO user, @Valid @NotNull Integer userId) throws Exception {
        //查询出来就是看过，喜欢，不喜欢
        try {
            Credential cred = TencentCloud.getCredential();
            ClientProfile clientProfile = TencentCloud.getClientProfile("faceid.tencentcloudapi.com");

            FaceidClient client = new FaceidClient(cred, region, clientProfile);

            String params = "{}";
            LivenessCompareRequest req = new LivenessCompareRequest();
            req.setImageBase64("");
            req.setVideoBase64("");
            req.setLivenessType("SILENT");

            LivenessCompareResponse resp = client.LivenessCompare(req);

        } catch (TencentCloudSDKException e) {
        }
        return null;
    }


}
