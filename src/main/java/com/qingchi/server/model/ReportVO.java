package com.qingchi.server.model;


import com.qingchi.base.constant.ViolateType;
import com.qingchi.base.entity.ReportDetailUtils;
import com.qingchi.base.entity.UserImgUtils;
import com.qingchi.base.model.report.ReportDO;
import com.qingchi.base.model.report.ReportDetailDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.UserImgDO;
import com.qingchi.base.utils.UserUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报信息
 */
public class ReportVO {
    /**
     * 关联类型，关联的是说说，评论，匹配，用户信息
     */
    private String reportContentType;

    /**
     * 被举报的用户名称
     */
    private ReportUserVO user;

    /**
     * 举报的状态，待审核，已通过，未通过
     */
    private String status;

    private List<UploadImgVO> imgs;
    /**
     * 被举报的类型列表
     */
    private List<String> types;

    private Integer supportRatio;
    private Integer opposeRatio;

    /**
     * 参与人数
     */
    private Integer memberCount;

    public ReportVO() {
    }

    public ReportVO(ReportDO reportDO) {
        this.reportContentType = reportDO.getReportContentType();
        UserDO receiveUser = UserUtils.get(reportDO.getReceiveUserId());
        this.user = new ReportUserVO(receiveUser);
        this.status = reportDO.getStatus();

        List<UserImgDO> userImgDOS = UserImgUtils.getImgs(receiveUser.getId());
        this.imgs = userImgDOS.stream().map(UploadImgVO::new).collect(Collectors.toList());
        this.supportRatio = reportDO.getSupportRatio();
        this.opposeRatio = reportDO.getOpposeRatio();

        List<ReportDetailDO> reportDetailDOS = ReportDetailUtils.getAll(reportDO.getId());

        this.memberCount = reportDetailDOS.size();
        //不显示 没有违规 类型
        this.types = new ArrayList<>(new HashSet<>(reportDetailDOS.stream().filter(item -> !item.getReportType().equals(ViolateType.noViolation)).map(ReportDetailDO::getReportType).collect(Collectors.toList())));
    }

    public static List<ReportVO> reportDOToVOS(List<ReportDO> reportDOS) {
        return reportDOS.stream().map(ReportVO::new).collect(Collectors.toList());
    }

    public String getReportContentType() {
        return reportContentType;
    }

    public void setReportContentType(String reportContentType) {
        this.reportContentType = reportContentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<UploadImgVO> getImgs() {
        return imgs;
    }

    public void setImgs(List<UploadImgVO> imgs) {
        this.imgs = imgs;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public Integer getSupportRatio() {
        return supportRatio;
    }

    public void setSupportRatio(Integer supportRatio) {
        this.supportRatio = supportRatio;
    }

    public Integer getOpposeRatio() {
        return opposeRatio;
    }

    public void setOpposeRatio(Integer opposeRatio) {
        this.opposeRatio = opposeRatio;
    }

    public ReportUserVO getUser() {
        return user;
    }

    public void setUser(ReportUserVO user) {
        this.user = user;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}
