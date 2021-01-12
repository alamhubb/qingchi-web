package com.qingchi.server.service;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.*;
import com.qingchi.base.constant.status.ContentStatus;
import com.qingchi.base.model.talk.TagDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.store.TalkQueryRepository;
import com.qingchi.base.repository.talk.TalkRepository;
import com.qingchi.server.common.TalkTabType;
import com.qingchi.server.model.RectangleVO;
import com.qingchi.server.model.TalkQueryVO;
import com.qingchi.server.model.TalkVO;
import com.qingchi.server.platform.AliAPI;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qinkaiyuan
 * @date 2018-09-16 18:35
 */
@Service
public class TalkQueryService {
    @Resource
    private TalkRepository talkRepository;
    @Resource
    private TalkQueryRepository talkQueryRepository;
    @Resource
    private TagService tagService;

    public static final Logger logger = LoggerFactory.getLogger(TalkQueryService.class);

    public ResultVO<List<TalkVO>> queryTalkListParamHandle(UserDO user, TalkQueryVO queryVO) {
        //如果传进来的时是空的，则默认赋值[0]，因为sql in 需要有值
        List<Integer> talkIds = queryVO.getTalkIds();
        if (talkIds == null || talkIds.size() == 0) {
            talkIds = new ArrayList<>(Collections.singletonList(0));
        }
        String homeType = queryVO.getHomeType();
        List<TalkDO> talkDOS;
        //首页类型不为空且为关注,查询关注, 兼容老版本和新版本
        if (StringUtils.isNotEmpty(homeType) && (HomeType.follow_name.equals(homeType) || TalkTabType.follow_type.equals(homeType))) {
            if (user != null) {
                talkDOS = talkQueryRepository.queryTalksTop10ByUserFollow(talkIds, user.getId());
            } else {
                return new ResultVO<>(new ArrayList<>());
            }
        } else {
            String adCode = queryVO.getAdCode();
            //添加用户使用地区的记录，记录用户最近访问的地区
            //下一版本不再记录用户使用位置，本地记录
//            districtService.addDistrictRecord(user, adCode, TalkOperateType.talkQuery);
            String adCodeStr;
            //必有adCode，如果首页类型为首页，则为中国
            //如果为首页不筛选地区和tag
            //只有为新版本全国才更改为空
            if (StringUtils.isNotEmpty(homeType) && (HomeType.home_name.equals(homeType)) || TalkTabType.home_type.equals(homeType)) {
                adCodeStr = "";
                //如果为空，为0或者为中国，则查询全部
                //话题校验
            } else {
                //老版本走着里没啥问题，去判断到底多少，也能为空
                if (adCode == null || CommonConst.chinaDistrictCode.equals(adCode)) {
                    adCodeStr = "";
                    //否则去重后面100的整除。like查询
                } else {
                    int adCodeInt = Integer.parseInt(adCode);
                    if (adCodeInt % 100 == 0) {
                        adCodeInt = adCodeInt / 100;
                        if (adCodeInt % 100 == 0) {
                            adCodeInt = adCodeInt / 100;
                        }
                    }
                    adCodeStr = String.valueOf(adCodeInt);
                }
            }
            //话题校验
            List<Integer> tagIds = queryVO.getTagIds();
            if (tagIds != null && tagIds.size() > 3) {
                return new ResultVO<>("最多同时筛选3个话题");
            }
            ResultVO<Set<TagDO>> resultVO = tagService.checkAndUpdateTagCount(user, tagIds, TalkOperateType.talkQuery);
            if (resultVO.hasError()) {
                return new ResultVO<>(resultVO.getErrorMsg());
            }
            talkDOS = this.queryTalkList(user, talkIds, adCodeStr, queryVO);
        }
        if ((queryVO.getLon() == null || queryVO.getLat() == null)) {
            //调用网络接口40毫秒
            RectangleVO rectangleVO = AliAPI.getRectangle();
            if (rectangleVO != null) {
                queryVO.setLon(rectangleVO.getLon());
                queryVO.setLat(rectangleVO.getLat());
            }
        }
        List<TalkVO> talkVOS = talkDOS.stream().map(talkDO -> new TalkVO(user, talkDO, queryVO)).collect(Collectors.toList());
        return new ResultVO<>(talkVOS);
    }

    public List<TalkDO> queryTalkList(UserDO user, List<Integer> talkIds, String adCode, TalkQueryVO talkQueryVO) {
        List<TalkDO> talkDOS = new ArrayList<>();
        //查询最多两条置顶内容
        //为首次查询
        //只有首页才显示置顶内容？(TalkTabType.home_type.equals(talkType) || (HomeType.home_name.equals(talkType))) &&
        if ((talkIds.size() == 1 && talkIds.get(0).equals(0))) {
            talkDOS = talkRepository.findTop2ByStatusInAndIdNotInAndGlobalTopGreaterThanOrderByGlobalTopDesc(ContentStatus.otherCanSeeContentStatus, talkIds, CommonStatus.initNum);
            talkIds.addAll(talkDOS.stream().map(TalkDO::getId).collect(Collectors.toList()));
        }
        String gender = talkQueryVO.getGender();
        List<String> genderList = GenderType.genders;
        if (GenderType.male.equals(gender)) {
            genderList = GenderType.maleAry;
        } else if (GenderType.female.equals(gender)) {
            genderList = GenderType.femaleAry;
        }
        //设置极限值
        Integer minAge = -500;
        //如果前台传过来的不为空则使用前台的
        if (!ObjectUtils.isEmpty(talkQueryVO.getMinAge())) {
            minAge = talkQueryVO.getMinAge();
        }
        //设置极限值
        Integer maxAge = 500;
        Integer frontMaxAge = talkQueryVO.getMaxAge();
        //如果前台传过来的不为空则使用前台的，40为前台最大值，如果选择40则等于没设置上线
        if (!ObjectUtils.isEmpty(frontMaxAge) && frontMaxAge < 40) {
            maxAge = frontMaxAge;
        }
        //tags为0的情况
        List<TalkDO> talkDOSTemp;
        Integer userId = null;
        if (user != null) {
            userId = user.getId();
        }
        //没选择tag的情况
        if (CollectionUtils.isEmpty(talkQueryVO.getTagIds())) {
            if (minAge <= 8 && maxAge >= 40 && StringUtils.isEmpty(adCode)) {
                //新增自己可见预审核状态动态，弃用这个
//                talkDOSTemp = talkRepository.findTop10ByStatusInAndUserGenderInAndAndIdNotInOrderByUpdateTimeDesc(CommonStatus.otherCanSeeContentStatus, genderList, talkIds);
                talkDOSTemp = talkQueryRepository.queryTalksTop10ByGender(talkIds, userId, genderList);
            } else {
                talkDOSTemp = talkQueryRepository.queryTalksTop10ByGenderAgeAndLikeAdCode(talkIds, userId, genderList, minAge, maxAge, adCode + "%");
                //同上弃用
//                talkDOSTemp = talkRepository.findTop10ByStatusInAndUserGenderInAndUserAgeBetweenAndIdNotInAndDistrictAdCodeLikeOrderByUpdateTimeDescIdDesc(CommonStatus.otherCanSeeContentStatus, genderList, minAge, maxAge, talkIds, adCode + "%");
            }
        } else {
            talkDOSTemp = talkQueryRepository.queryTalksTop10ByGenderAgeAndLikeAdCodeAndTagIds(talkIds, userId, genderList, minAge, maxAge, adCode + "%", talkQueryVO.getTagIds());
//            同上弃用
//            talkDOSTemp = talkRepository.queryTalkIdsTop10ByGenderAgeAndLikeAdCodeAndTagIds(talkQueryVO.getTagIds(), CommonStatus.otherCanSeeContentStatus, genderList, minAge, maxAge, talkIds, adCode + "%");
        }
        talkDOS.addAll(talkDOSTemp);
        if (talkDOS.size() > 10) {
            talkDOS = talkDOS.subList(0, 10);
        }
        //token不为空才获取user
//        List<TalkDO> talkDOS = talkRepository.findTop10ByStatusOrderByIdDesc(CommonStatus.enable);
        return talkDOS;
    }

/*    String homeType = talkQueryVO.getHomeType();
    Double lon = talkQueryVO.getLon();
    Double lat = talkQueryVO.getLat();*/
    //使用附近并且 lon和lat不为null
    //并且不能为新版本，为新版本就不走这里
    //只有老版本使用附近才走这里，新版本没有了附近功能，需不需要加上
        /*//toDO 先不加使用城市功能就可以替代
        if ((lon != null && lat != null) && !HomeType.homeTypes.contains(homeType)) {
            //初始查询大致两公里内，然后递增
            Double distance = 0.02;
            for (int i = 0; i < 10; i++) {
                //首页的情况下不查询tags
                //首页的情况下不查询tags
                if (CollectionUtils.isEmpty(talkQueryVO.getTagIds())) {
                    talkDOS.addAll(talkRepository.findTalksByPosition(CommonStatus.contentEnableStatus, genderList, minAge, maxAge, talkIds, lon, lat, distance, 10 - talkDOS.size()));
                } else {
                    talkDOS.addAll(talkRepository.findTalksByPositionAndTags(CommonStatus.contentEnableStatus, genderList, minAge, maxAge, talkIds, lon, lat, distance, 10 - talkDOS.size(), talkQueryVO.getTagIds()));
                }
                if (talkDOS.size() > 9) {
                    break;
                }
                talkIds.addAll(talkDOS.stream().map(TalkDO::getId).collect(Collectors.toList()));
                distance *= 2;
            }
        } else {*/

    /*
    public List<TalkDO> queryTalkListByFilter(TalkFilter queryVO) {
        StringBuilder queryString = new StringBuilder("SELECT t FROM Talk t where 1=1");
        if (queryVO != null) {
            if (queryVO.getOpenFilter()) {
                queryString.append("and t.user.age between :ageMin and :ageMax");
            }
        }
        queryString.append("order by t.createTime desc");
        TypedQuery<TalkDO> query = entityManager.createQuery(queryString.toString(), TalkDO.class);
        if (queryVO != null) {
            query.setParameter("ageMin", queryVO.getAgeMin());
            query.setParameter("ageMax", queryVO.getAgeMax());
        }
        return query.getResultList();
    }


    public List<TalkDO> queryTalkListByPosition() {
        Page<TalkDO> page = talkRepository.queryTalkByCommentsParentCommentIsNull();
        List<TalkDO> talks = page.getContent();

        return talks;
    }

    public List<TalkDO> queryAllTalkList() {
        List<TalkDO> pages = talkRepository.findAll();
        return pages;
    }

    public List<TalkDO> queryFollowTalkList(UserDO user) {
        Page<TalkDO> page = talkRepository.queryTalkListByContactType(user, 1);
        return page.getContent();
    }

    public List<TalkDO> queryMatchTalkList() {
        Page<TalkDO> page = talkRepository.findByTalkType(1);
        return page.getContent();
    }*/

}
