package com.qingchi.server.controller;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.ErrorCode;
import com.qingchi.base.constant.ErrorMsg;
import com.qingchi.base.model.talk.TagDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.platform.tencent.TencentCloud;
import com.qingchi.base.platform.weixin.HttpResult;
import com.qingchi.base.platform.weixin.WxUtil;
import com.qingchi.base.redis.TagRedis;
import com.qingchi.base.redis.TagTypeVO;
import com.qingchi.base.redis.TagVO;
import com.qingchi.base.repository.tag.TagRepository;
import com.qingchi.base.repository.tag.TagTypeRepository;
import com.qingchi.base.service.IllegalWordService;
import com.qingchi.base.store.TagStoreUtils;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.TagAddVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * @author qinkaiyuan
 * @date 2018-11-18 20:45
 */

@RestController
@RequestMapping("tag")
public class TagController {
    @Resource
    private TagRepository tagRepository;
    @Resource
    private TagStoreUtils tagQueryRepository;
    @Resource
    private IllegalWordService illegalWordService;

    @PostMapping("addTag")
    public ResultVO<TagVO> addTag(UserDO user, @RequestBody @Valid TagAddVO tagAddVO) {
        String tagName = tagAddVO.getTagName();
        if (tagName.length() > 4) {
            return new ResultVO<>("话题最多支持四个字");
        }
        ResultVO resultVO = illegalWordService.checkHasIllegals(tagName);
        //校验内容是否违规
        if (resultVO.hasError()) {
            return new ResultVO<>(resultVO);
        }

        Optional<TagDO> optionalTagDO = tagRepository.findOneByName(tagName);
        //toDO 这里有坑，就是没有查询标签状态，如果标签已经禁用，这里也可以直接用了
        if (optionalTagDO.isPresent()) {
            return new ResultVO<>(ErrorCode.CUSTOM_ERROR, "标签已经存在，请直接使用", new TagVO(optionalTagDO.get()));
        }
        if (TencentCloud.textIsViolation(tagName)) {
            return new ResultVO<>("标签名称违规");
        }

        String description = tagAddVO.getDescription();
        if (StringUtils.isNotEmpty(description)) {
            HttpResult wxDesResult = WxUtil.checkContentWxSec(description);
            if (wxDesResult.hasError()) {
                return new ResultVO<>("标签描述包含违规内容，禁止发布，请修改后重试");
            }
        }
        TagDO tagDO = tagAddVO.toDO(user);
        tagDO = tagRepository.save(tagDO);
        TagVO tagVO = new TagVO(tagDO);
        return new ResultVO<>(tagVO);
    }



    @Resource
    private TagRedis tagRedis;

    /**
     * 前端新增talk时，需要选择tag，前端搜索时使用
     *
     * @return
     */
    @PostMapping("queryTags")
    public ResultVO<List<TagVO>> queryTags() {
        return new ResultVO<>(tagRedis.getAllTags());
    }
    @PostMapping("queryTagTypes")
    public ResultVO<List<TagTypeVO>> queryTagTypes() {
        return new ResultVO<>(tagRedis.getAllTageTypes());
    }

    @PostMapping("queryHotTags")
    public ResultVO<List<TagVO>> queryHotTags() {
        return new ResultVO<>(tagRedis.getHotTags());
    }

    /**
     * 添加一条用户选择的记录
     *暂时作废，已在查询和新增时调用了这个方法
     * @return
     */
    /*@PostMapping("addDistrictRecord")
    public ResultVO addDistrictRecord(UserDO user, @NotBlank(message = "入参为空异常") String adCode) {
        return districtService.addDistrictRecord(user, adCode);
    }*/
}
