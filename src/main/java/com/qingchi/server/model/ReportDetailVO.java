package com.qingchi.server.model;


import com.qingchi.base.entity.UserImgUtils;
import com.qingchi.base.model.report.ReportDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.UserImgDO;
import com.qingchi.base.utils.UserUtils;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报信息
 */
@Data
public class ReportDetailVO {
    /**
     * 举报类型，政治敏感，暴露，暴力等
     */
    private String type;


    /**
     * 举报时的备注信息
     */
    private String content;

    /**
     * 举报的状态，待审核，已通过，未通过
     */
    private String status;

    /**
     * 关联类型，关联的是说说，评论，匹配，用户信息
     */
    private String reportContentType;


    /**
     * 被举报的用户名称
     */
    private String nickname;

    private List<UploadImgVO> imgs;

    public ReportDetailVO() {
    }

    public ReportDetailVO(ReportDO reportDO) {
        this.reportContentType = reportDO.getReportContentType();
//        this.type = reportDO.getChildReports().get(0).getReportType();
        UserDO receiveUser = UserUtils.get(reportDO.getReceiveUserId());
        this.nickname = receiveUser.getNickname();
        this.status = reportDO.getStatus();
        List<UserImgDO> userImgDOS = UserImgUtils.getImgs(receiveUser.getId());
        this.imgs = userImgDOS.stream().map(UploadImgVO::new).collect(Collectors.toList());
    }

    public static List<ReportDetailVO> reportDOToVOS(List<ReportDO> reportDOS) {
        return reportDOS.stream().map(ReportDetailVO::new).collect(Collectors.toList());
    }
}
