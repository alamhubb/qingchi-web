package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.*;
import com.qingchi.base.constant.status.ContentStatus;
import com.qingchi.base.domain.ReportDomain;
import com.qingchi.base.model.system.DistrictDO;
import com.qingchi.base.model.talk.TagDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.talk.TalkImgDO;
import com.qingchi.base.model.talk.TalkTagDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.talk.TalkImgRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.base.repository.talk.TalkTagRepository;
import com.qingchi.base.store.DistrictStoreUtils;
import com.qingchi.base.utils.DateUtils;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.check.ModelContentCheck;
import com.qingchi.server.model.TalkAddVO;
import com.qingchi.server.model.TalkDeleteVO;
import com.qingchi.server.model.TalkImgVO;
import com.qingchi.server.model.TalkVO;
import com.qingchi.base.service.IllegalWordService;
import com.qingchi.server.service.TagService;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("talk")
public class TalkController {
    @Resource
    private TalkRepository talkRepository;
    @Resource
    private TagService tagService;

    @Resource
    private TalkImgRepository talkImgRepository;

    @Resource
    private TalkTagRepository talkTagRepository;

    @Resource
    private ReportDomain reportDomain;
    @Resource
    private IllegalWordService illegalWordService;

    @Resource
    private ModelContentCheck modelContentCheck;

    @PostMapping("deleteTalk")
    @ResponseBody
    public ResultVO<Object> deleteTalk(UserDO user, @RequestBody @Valid TalkDeleteVO talkVO) {
        /**
         * 删除动态操作，
         * 如果是系统管理员删除动态，则必须填写原因，删除后发表动态的用户将被封禁
         * 如果是自己删的自己的动态，则不需要填写原因，默认原因是用户自己删除
         */
        Optional<TalkDO> optionalTalkDO = talkRepository.findOneByIdAndStatusIn(talkVO.getTalkId(), CommonStatus.otherCanSeeContentStatus);
        if (!optionalTalkDO.isPresent()) {
            return new ResultVO<>("无法删除不存在的动态");
        }
        TalkDO talkDO = optionalTalkDO.get();
        //不是管理员的话就必须是自己删除自己
        //是否是自己删除自己的动态
        if (!talkDO.getUserId().equals(user.getId())) {
            QingLogger.logger.warn("有人尝试删除不属于自己的动态,用户名:{},id:{},尝试删除talkId：{}", user.getNickname(), user.getId(), talkDO.getId());
            return new ResultVO<>("系统异常，无法删除不属于自己的动态");
        }
        talkDO.setUpdateTime(new Date());
        talkDO.setStatus(ContentStatus.delete);
        talkDO.setDeleteReason("用户自行删除");
        talkRepository.save(talkDO);
        return new ResultVO<>();
    }


    @PostMapping("addTalk")
    @ResponseBody
    public ResultVO addTalk(UserDO user, @RequestBody @Valid @NotNull TalkAddVO talkVO) throws IOException {
        ResultVO<Object> checkResult = checkTalkAddVO(user, talkVO);
        if (checkResult.hasError()) {
            return checkResult;
        }
        //校验地理位置
        String adCode = talkVO.getAdCode();
        //如果为未定位或者老版本，adcode为空，则设置为中国地区
        if (adCode == null) {

            adCode = CommonConst.chinaDistrictCode;
        }
        //根据adcode获取地区名
        DistrictDO districtDO = DistrictStoreUtils.findFirstOneByAdCode(adCode);
        if (districtDO == null) {
            return new ResultVO<>("选择了不存在的地区");
        }
        //获取经过后台认证的 行政区DO
        //话题校验
        List<Integer> tagIds = talkVO.getTagIds();
        ResultVO<Set<TagDO>> resultVO = tagService.checkAndUpdateTagCount(user, tagIds, TalkOperateType.talkAdd);
        if (resultVO.hasError()) {
            return resultVO;
        }

        TalkDO talkDO = talkVO.toDO(districtDO, user);

        //保存说说
        talkDO = talkRepository.save(talkDO);

        List<TalkTagDO> list = new ArrayList<>();

        Set<TagDO> tagDOS = resultVO.getData();
        for (TagDO tagDO : tagDOS) {
            TalkTagDO talkTagDO = new TalkTagDO();
            talkTagDO.setTagId(tagDO.getId());
            talkTagDO.setTalkId(talkDO.getId());
            list.add(talkTagDO);
        }
        talkTagRepository.saveAll(list);


        List<TalkImgDO> imgDOS = TalkImgVO.talkImgVOToDOS(talkVO.getImgs());

        for (TalkImgDO imgDO : imgDOS) {
            imgDO.setTalkId(talkDO.getId());
        }

        talkImgRepository.saveAll(imgDOS);


        //校验是否触发关键词，如果触发生成举报，修改动态为预审查，只能用户自己可见
        reportDomain.checkKeywordsCreateReport(talkDO);


        TalkVO talkVO1 = new TalkVO();
        talkVO1.setId(talkDO.getId());
        //返回新增的talkId方便前台高亮
        return new ResultVO<>(talkVO1);
    }

    private ResultVO<Object> checkTalkAddVO(UserDO user, TalkAddVO talkVO) {
        String talkContent = talkVO.getContent();

        if (StringUtils.isEmpty(talkContent) && CollectionUtils.isEmpty(talkVO.getImgs())) {
            return new ResultVO<>("不能发布文字和图片均为空的动态");
        }
        if (talkContent.length() > 200) {
            return new ResultVO<>("动态最多支持200个字，请精简动态内容");
        }

        ResultVO resultVO = modelContentCheck.checkUserAndContent(talkVO.getContent(), user);
        //校验内容是否违规
        if (resultVO.hasError()) {
            return new ResultVO<>(resultVO);
        }

        //系统管理员则不校验规则
        if (!UserType.system.equals(user.getType())) {
            Date curDate = new Date();
            Date oneMinuteBefore = new Date(curDate.getTime() - CommonConst.minute);
            //1分钟内不能发超过1条
            Integer minuteCount = talkRepository.countByUserIdAndCreateTimeBetween(user.getId(), oneMinuteBefore, curDate);
            if (minuteCount > 0) {
                QingLogger.logger.info("1分钟最多发布1条动态，请稍后再试:+" + talkContent);
                return new ResultVO<>("1分钟最多发布1条动态，请稍后再试");
            }
            Date tenMinuteBefore = new Date(curDate.getTime() - 10 * CommonConst.minute);
            Integer tenMinuteBeforeCount = talkRepository.countByUserIdAndCreateTimeBetween(user.getId(), tenMinuteBefore, curDate);
            if (tenMinuteBeforeCount > 2) {
                QingLogger.logger.info("10分钟最多发布3条动态，请稍后再试:+" + talkContent);
                return new ResultVO<>("10分钟最多发布3条动态，请稍后再试");
            }
            //每天0点到现在不能发布超过10条
            //获取当天0点
            Date zero = DateUtils.getTodayZeroDate();
            //10分钟内不能发超过5条
            //1天内不能发超过10条
            Integer oneDayBeforeCount = talkRepository.countByUserIdAndCreateTimeBetween(user.getId(), zero, curDate);
            if (oneDayBeforeCount > 9) {
                QingLogger.logger.info("1天最多发布10条动态，请稍后再试:+" + talkContent);
                return new ResultVO<>("1天最多发布10条动态，请稍后再试");
            }
        }
        return new ResultVO<>();
    }
}