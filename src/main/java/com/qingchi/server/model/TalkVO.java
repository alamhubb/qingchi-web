package com.qingchi.server.model;

import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.model.system.DistrictDO;
import com.qingchi.base.model.talk.TagDO;
import com.qingchi.base.model.talk.TalkDO;
import com.qingchi.base.model.talk.TalkImgDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.repository.follow.FollowRepository;
import com.qingchi.base.repository.hug.HugRepository;
import com.qingchi.base.repository.tag.TagRepository;
import com.qingchi.base.repository.talk.CommentRepository;
import com.qingchi.base.repository.talk.TalkImgRepository;
import com.qingchi.base.store.TagStoreUtils;
import com.qingchi.base.utils.RequestUtils;
import com.qingchi.base.utils.UserUtils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 不需要像帖子一样，每次有回复都刷新，因为不愁看，且你评论后的，有动静你会有通知
 */
@Data
@Component
public class TalkVO {
    public static final Logger logger = LoggerFactory.getLogger(TalkVO.class);

    private static CommentRepository commentRepository;
    private static FollowRepository followRepository;
    private static HugRepository hugRepository;
    private static TagRepository TagRepository;
    private static TalkImgRepository talkImgRepository;
    private static TagStoreUtils tagQueryRepository;

    @Resource
    public void setFollowRepository(FollowRepository followRepository) {
        TalkVO.followRepository = followRepository;
    }

    @Resource
    public void setHugRepository(HugRepository hugRepository) {
        TalkVO.hugRepository = hugRepository;
    }

    @Resource
    public void setCommentRepository(CommentRepository commentRepository) {
        TalkVO.commentRepository = commentRepository;
    }

    @Resource
    public void setTagRepository(com.qingchi.base.repository.tag.TagRepository tagRepository) {
        TagRepository = tagRepository;
    }

    @Resource
    public void setTalkImgRepository(TalkImgRepository talkImgRepository) {
        TalkVO.talkImgRepository = talkImgRepository;
    }

    @Resource
    public void setTagQueryRepository(TagStoreUtils tagQueryRepository) {
        TalkVO.tagQueryRepository = tagQueryRepository;
    }

    private Integer id;
    private TalkUserVO user;
    private String content;
    private List<TalkImgVO> imgs;
    //未来可以修改，但要有记录，修改过就显示已修改，显示修改记录
    private Date updateTime;
    private List<TalkCommentVO> comments;

    /**
     * 评论数量
     */
    private Integer commentNum;
    /**
     * 抱抱次数
     */
    private Integer hugNum;
    /**
     * 举报次数
     */
    private Integer reportNum;
    private Boolean hasHugged;
    private Boolean hasFollowed;
    /**
     * talk对应的地理位置
     */
    private TalkDistrictVO district;

    //省
    private String provinceName;
    //市
    private String cityName;
    //区县
    private String districtName;
    //统一标识
    private String adCode;

    private List<TalkTagVO> tags;

    /**
     * 距离,单位千米
     */
    private Double distance;
    private Integer globalTop;

    public TalkVO() {
    }

    //用户详情
    public TalkVO(UserDO user, TalkDO talkDO) {
        this(user, talkDO, false, null);
    }

    public TalkVO(UserDO user, TalkDO talkDO, TalkQueryVO talkQueryVO) {
        this(user, talkDO, false, talkQueryVO);
    }

    //talk详情
    public TalkVO(UserDO user, TalkDO talkDO, Boolean showAllComment) {
        this(user, talkDO, showAllComment, null);
    }

    /**
     * @param talkDO
     * @param showAllComment 如果是详情页则需要展示所有comment
     */
    public TalkVO(UserDO user, TalkDO talkDO, Boolean showAllComment, TalkQueryVO talkQueryVO) {
        this.id = talkDO.getId();
        UserDO talkUser = UserUtils.get(talkDO.getUserId());
        this.user = new TalkUserVO(talkUser);

        this.content = talkDO.getContent();
        //70毫秒，可缓存
        List<TalkImgDO> imgDOS = talkImgRepository.findTop3ByTalkId(talkDO.getId());
//        List<TalkImgDO> imgDOS = talkDO.getImgs();
        if (imgDOS != null && imgDOS.size() > 0) {
            this.imgs = TalkImgVO.talkImgDOToVOS(imgDOS);
        } else {
            this.imgs = new ArrayList<>();
        }
        //10毫秒
        if (showAllComment) {
            this.comments = TalkCommentVO.commentDOToVOS(user, commentRepository.findTop50ByTalkIdAndStatusInAndParentCommentIdIsNullOrderByUpdateTimeDesc(talkDO.getId(), CommonStatus.selfCanSeeContentStatus), true);
        } else {
            this.comments = TalkCommentVO.commentDOToVOS(user, commentRepository.findTop5ByTalkIdAndStatusInAndParentCommentIdIsNullOrderByUpdateTimeDesc(talkDO.getId(), CommonStatus.selfCanSeeContentStatus), false);
        }
        this.updateTime = talkDO.getUpdateTime();
        this.commentNum = talkDO.getCommentNum();
        this.hugNum = talkDO.getHugNum();
        this.reportNum = talkDO.getReportNum();
        this.globalTop = talkDO.getGlobalTop();
        UserDO mineUser = RequestUtils.getUser();
        //40毫秒
        if (mineUser != null) {
            //20毫秒
            Integer hugCount = hugRepository.countByTalkIdAndUserId(talkDO.getId(), mineUser.getId());
            if (hugCount > 0) {
                this.hasHugged = true;
            }
            //他人需要判断
            if (!mineUser.getId().equals(talkUser.getId())) {
                Integer followCount = followRepository.countByUserIdAndBeUserIdAndStatus(mineUser.getId(), talkUser.getId(), CommonStatus.normal);
                this.hasFollowed = followCount > 0;
            } else {
                //为自己不可关注
                this.hasFollowed = true;
            }
        } else {
            this.hasHugged = false;
            this.hasFollowed = false;
        }
        //60毫秒，可缓存

        DistrictDO district = new DistrictDO();
        this.adCode = talkDO.getAdCode();
        this.cityName = talkDO.getCityName();
        this.districtName = talkDO.getDistrictName();
        this.provinceName = talkDO.getProvinceName();

        district.setAdCode(this.adCode);
        district.setProvinceName(this.provinceName);
        district.setCityName(this.cityName);
        district.setDistrictName(this.districtName);

        this.district = new TalkDistrictVO(district);

        //10 毫秒
        List<TagDO> tagDOS = TagStoreUtils.getTagsByTalkId(talkDO.getId());

        //50毫秒
        if (tagDOS != null) {
            this.tags = TalkTagVO.tagDOToVOS(tagDOS);
        } else {
            this.tags = new ArrayList<>();
        }

        //如果经纬度为空

        //计算距离
        //如果查询条件有经纬度
        //耗时60毫秒
        if (talkQueryVO != null && talkQueryVO.getLon() != null) {
            Double dbLon = talkDO.getLon();
            Double dbLat = talkDO.getLat();
            //如果talk有记录经纬度
            if (dbLon != null) {
                //经纬度换约等于大概换算成千米，任何地点经度都大致相等
                Double talkLon = dbLon * 111;
                Double talkLat = dbLat * (Math.cos(Math.toRadians(dbLat)) * 111);
                //任何地点经度都大致相等,为111公里
                Double queryLon = talkQueryVO.getLon() * 111;
                //计算当前纬度，1纬度等于多少公里
                Double queryLat = talkQueryVO.getLat() * (Math.cos(Math.toRadians(talkQueryVO.getLat())) * 111);
                //两个经纬度求差
                double lonDiffAbs = Math.abs(talkLon - queryLon);
                double latDiffAbs = Math.abs(talkLat - queryLat);
                //经纬度差勾股求距离
                this.distance = Math.sqrt(Math.pow(lonDiffAbs, 2) + Math.pow(latDiffAbs, 2));
            }
        }
    }
}