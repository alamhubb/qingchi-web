package com.qingchi.server.controller;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.domain.ReportDomain;
import com.qingchi.base.model.BaseModelDO;
import com.qingchi.base.constant.*;
import com.qingchi.base.repository.chat.MessageReceiveRepository;
import com.qingchi.base.repository.chat.MessageRepository;
import com.qingchi.base.repository.report.ReportDetailRepository;
import com.qingchi.base.repository.report.ReportRepository;
import com.qingchi.base.service.BaseModelService;
import com.qingchi.base.service.ReportService;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.user.UserRepository;
import com.qingchi.base.repository.user.UserImgRepository;
import com.qingchi.base.utils.DateUtils;
import com.qingchi.base.utils.UserUtils;
import com.qingchi.base.model.report.ReportAddVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("report")
public class ReportController {
    @Resource
    private ReportRepository reportRepository;
    @Resource
    private ReportDetailRepository reportDetailRepository;
    @Resource
    private TalkRepository talkRepository;
    @Resource
    private CommentRepository commentRepository;
    @Resource
    private UserImgRepository userImgRepository;
    @Resource
    private MessageRepository messageRepository;
    @Resource
    private UserRepository userRepository;
    @Resource
    private MessageReceiveRepository messageReceiveRepository;

    @Resource
    private BaseModelService baseModelService;

    @Resource
    private ReportService reportService;

    @Resource
    private ReportDomain reportDomain;

    /*@PostMapping("queryReports")
    public ResultVO<List<ReportVO>> queryReports() {
//        reportRepository.findDistinctTalkByStatus(CommonStatus.audit);
        List<ReportDO> reportDOS = reportRepository.findAll();
        return new ResultVO<>(ReportVO.reportDOToVOS(reportDOS));
    }*/


    @PostMapping("addReport")
    public ResultVO<?> addReport(@RequestBody @Valid @NotNull ReportAddVO reportAddVO, UserDO user) {
        //校验举报类型
        String reportType = reportAddVO.getReportType();
        if (!ViolateType.violateTypes.contains(reportType)) {
            return new ResultVO<>("错误的举报类型");
        }
        String reportContentType = reportAddVO.getReportContentType();
        if (!ReportContentType.reportContentTypeTypes.contains(reportContentType)) {
            return new ResultVO<>("错误的举报内容类型");
        }
        if (ViolateType.other.equals(reportType) && StringUtils.isEmpty(reportAddVO.getContent())) {
            return new ResultVO<>("选择其他违规时，请您补充观点");
        }
        //这点应该在新增举报那个页面能控制，下面这俩
        //正义值小于0，则每天只能举报2个，举报提示
        //正义值小于300不能再举报
        //给用户通知，您举报成功失败，奖励或扣除积分，每天满多少，低于0 2练个，低于200不能再举报
        if (!user.getType().equals(UserType.system)) {
            if (!CommonStatus.canPublishContentStatus.contains(user.getStatus())) {
                return new ResultVO<>(ErrorMsg.userMaybeViolation);
            }
            Date todayZero = DateUtils.getTodayZeroDate();
            Date curDate = new Date();
            Integer userJusticeValue = user.getJusticeValue();
            //小于-100不允许再举报
            if (userJusticeValue <= AppConfigConst.cantReportValue) {
                return new ResultVO<>("您涉嫌胡乱举报，被禁止使用举报功能");
                //小于-10一天只能举报两次
            }
            Integer reportCount = reportDetailRepository.countByUserIdAndCreateTimeBetween(user.getId(), todayZero, curDate);
            if (userJusticeValue < AppConfigConst.limitReportValue) {
                if (reportCount >= AppConfigConst.lowLimitReportCount) {
                    return new ResultVO<>("因您的正义值低于：" + AppConfigConst.limitReportValue + "，所以您每天只能举报：" + AppConfigConst.lowLimitReportCount + "次");
                }
            }
            Integer highLimitReportCount = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.highLimitReportCountKey);
            if (reportCount >= highLimitReportCount) {
                return new ResultVO<>("每人每天只能举报" + highLimitReportCount + "次");
            }
        }
        //现在这个意思是，只能举报，正常显示的动态，待审核，不违规，预审核，删除的都举报不了
        //理论上来说这些状态应该都是不能被举报的，因为看不见，所以这个逻辑现在也没啥问题，没了不违规状态了
        //查询动态状态是否为正常
        Optional<BaseModelDO> modelOptional;
        // 举报了动态
        // 举报了动态
        if (ReportContentType.talk.equals(reportContentType)) {
            //查询出 评论信息
            //只查询正常能看到的，违规，审核，删除的都提示
            modelOptional = talkRepository.findTop1ById(reportAddVO.getTalkId());
        } else if (ReportContentType.comment.equals(reportContentType)) {
            modelOptional = commentRepository.findOneByIdAndStatus(reportAddVO.getCommentId(), CommonStatus.normal);
        } else if (ReportContentType.message.equals(reportContentType)) {
            modelOptional = messageRepository.findOneByIdAndStatus(reportAddVO.getMessageId(), CommonStatus.normal);
        } else if (ReportContentType.userImg.equals(reportContentType)) {
            modelOptional = userImgRepository.findOneByIdAndStatus(reportAddVO.getUserImgId(), CommonStatus.normal);
        } else {
            return new ResultVO<>("错误的内容类型");
        }
        //这里限制了内容只能被举报一次，只有第一次被举报时，修改用户状态和动态状态
        //之后，只有审核时，才修改动态，
        if (!modelOptional.isPresent()) {
            return new ResultVO<>("内容已被举报，审核中");
        }
        BaseModelDO modelDO = modelOptional.get();
        //不为正常则不该看到，提示已被举报，有点问题不影响业务的小问题，提示信息不对
        if (!CommonStatus.normal.equals(modelDO.getStatus())) {
            return new ResultVO<>("内容已被举报，审核中");
        }
        //这里之后才能校验
        UserDO receiveUser = UserUtils.get(modelDO.getUserId());
        //举报人不为系统管理员才校验
        if (!user.getType().equals(UserType.system)) {
            if (UserType.system.equals(receiveUser.getType())) {
                return new ResultVO<>("不能举报官方内容");
            } else if (user.getId().equals(receiveUser.getId())) {
                return new ResultVO<>("不能举报自己的评论");
            } /*else if (modelDO.getStatus().equals(CommonStatus.noViolation)) {
                return new ResultVO<>("内容已审核，不违规");
            }*/
        }
        return reportDomain.userReportContent(
                reportAddVO,
                modelDO,
                user.getId()
        );
    }

}
