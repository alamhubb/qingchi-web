package com.qingchi.server.service;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.TalkOperateType;
import com.qingchi.base.model.talk.TagDO;
import com.qingchi.base.model.talk.TagTypeDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.tag.TagRepository;
import com.qingchi.base.repository.tag.TagTypeRepository;
import com.qingchi.base.store.TagStoreUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author qinkaiyuan
 * @date 2019-11-16 23:09
 */
@Service
public class TagService {
    @Resource
    private TagRepository tagRepository;
    @Resource
    private TagStoreUtils tagQueryRepository;
    @Resource
    private TagTypeRepository tagTypeRepository;


    public ResultVO<Set<TagDO>> checkAndUpdateTagCount(UserDO user, List<Integer> tagIds, String talkOperateType) {
        Set<TagDO> tagDOList = new HashSet<>();
        /**
         * 考虑上一版本，可以为空，为空没有影响，toDO 下一版本删除记录用户最近tag的代码，只在用户本地记录
         */
        if (!ObjectUtils.isEmpty(tagIds)) {
            for (Integer tagId : tagIds) {
                if (!ObjectUtils.isEmpty(tagId)) {
                    //查询启用的话题
                    Optional<TagDO> optionalTagDO = tagRepository.findByIdAndStatus(tagId, CommonStatus.enable);
                    //如果话题存在且可用
                    if (optionalTagDO.isPresent()) {
                        TagDO tagDO = optionalTagDO.get();
                        if (!CommonStatus.enable.equals(tagDO.getStatus())) {
                            return new ResultVO<>("引用了不可使用的话题");
                        }
                        //次数加1
                        tagDO.setCount(tagDO.getCount() + 1);
                        if (TalkOperateType.talkAdd.equals(talkOperateType)) {
                            tagDO.setTalkCount(tagDO.getTalkCount() + 1);
                            TagTypeDO tagTypeDO = tagTypeRepository.findById(tagDO.getTagTypeId()).get();
                            tagTypeDO.setTalkCount(tagDO.getTalkCount() + 1);
                        }
                        tagDOList.add(tagDO);
                        //暂时不再在后台记录用户最近使用的标签
                    /*if (user != null) {
                        Date curDate = new Date();
                        Optional<UserTagDO> optionalUserTag = userTagRepository.findFirstByUserIdAndTag(user, tagDO);
                        UserTagDO userTagDO;
                        //如果用户存在这个记录
                        if (optionalUserTag.isPresent()) {
                            userTagDO = optionalUserTag.get();
                            if (userTagDO.getStatus().equals(CommonStatus.delete)) {
                                userTagDO.setStatus(CommonStatus.enable);
                            }
                            userTagDO.setCount(userTagDO.getCount() + 1);
                            userTagDO.setUpdateTime(curDate);
                        } else {
                            //给用户new一个
                            userTagDO = new UserTagDO(user, tagDO);
                        }
                        if (TalkOperateType.talkAdd.equals(talkOperateType)) {
                            userTagDO.setTalkCount(userTagDO.getTalkCount() + 1);
                        }
                        userTagRepository.save(userTagDO);
                    }*/
                    } else {
                        /**
                         * 带选择时为null，可以为null，但不能不是null，数据库还不存在
                         */
                        if (!ObjectUtils.isEmpty(tagId)) {
                            //tagId不为null，且数据库中还不存在
                            return new ResultVO<>("引用了不可使用的话题");
                        }
                    }
                }
            }
            if (tagDOList.size() > 0) {
                tagRepository.saveAll(tagDOList);
            }
        }
        return new ResultVO<>(tagDOList);
    }
}
