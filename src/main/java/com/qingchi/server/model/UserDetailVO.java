package com.qingchi.server.model;

import com.qingchi.base.constant.ExpenseType;
import com.qingchi.base.model.chat.ChatUserDO;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.redis.DistrictVO;
import com.qingchi.base.repository.chat.ChatUserRepository;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.MatchConstants;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.UserContactDO;
import com.qingchi.base.repository.shell.UserContactRepository;
import com.qingchi.base.repository.follow.FollowRepository;
import com.qingchi.base.model.user.UserDetailDO;
import com.qingchi.base.repository.user.UserDetailRepository;
import com.qingchi.base.model.user.UserImgDO;
import com.qingchi.base.repository.user.UserImgRepository;
import com.qingchi.server.common.FollowConst;
import com.qingchi.server.service.ChatService;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author qinkaiyuan 查询结果可以没有set和空构造，前台传值可以没有get
 * @date 2019-08-13 23:34
 */
@Data
@Component
public class UserDetailVO {
    private static UserDetailRepository userDetailRepository;


    private static UserContactRepository userContactRepository;
    private static ChatUserRepository chatUserRepository;
    private static UserImgRepository userImgRepository;

    @Resource
    public void setUserDetailRepository(UserDetailRepository userDetailRepository) {
        UserDetailVO.userDetailRepository = userDetailRepository;
    }

    @Resource
    public void setUserContactRepository(UserContactRepository userContactRepository) {
        UserDetailVO.userContactRepository = userContactRepository;
    }

    @Resource
    public void setUserImgRepository(UserImgRepository userImgRepository) {
        UserDetailVO.userImgRepository = userImgRepository;
    }

    private static FollowRepository followRepository;
    private static ChatService chatService;

    @Resource
    public void setChatService(ChatService chatService) {
        UserDetailVO.chatService = chatService;
    }

    @Resource
    public void setFollowRepository(FollowRepository followRepository) {
        UserDetailVO.followRepository = followRepository;
    }

    @Resource
    public void setChatUserRepository(ChatUserRepository chatUserRepository) {
        UserDetailVO.chatUserRepository = chatUserRepository;
    }

    private Integer id;
    private String nickname;
    private String gender;
    private Integer age;
    private String location;
    private String avatar;
    private String birthday;
    private String idCardStatus;
    //显示用户颜值
    private Integer faceRatio;
    private Integer likeCount;
    private List<UserImgVO> imgs;

    private Boolean onlineFlag;
    //用户最后在线时间，
    private Date lastOnlineTime;
    //是否为vip，
    private Boolean vipFlag;
    //已开vip多少个月
    private Boolean yearVipFlag;

    //邀请码，你分享给别人的邀请码
    private String inviteCode;
    //邀请人，谁邀请的你
    private MatchUserVO registerInviteUser;
    //vip到期时间
    private Date vipEndDate;
    //清池币数量
    private Integer qcb;

    private Integer fansNum;
    private Integer followNum;
    private String userType;

    /**
     * 微信账户，用户可以关联微信账户，方便添加好友
     */
    private String wxAccount;
    private String qqAccount;
    private String wbAccount;
    private String contactAccount;
    //他人发起消息需要支付的金额
    private Integer beSponsorMsgShell;
    private Boolean openContact;
    private Boolean showUserContact;
    private DistrictVO talkQueryDistrict;
    private DistrictVO talkAddDistrict;
    private Boolean hasFollowed;
    private Boolean beFollow;
    //暂时不使用这个字段
//    private Integer 使用附近;
    private String followStatus;
    private Integer loveValue;
    private Integer justiceValue;
    private String phoneNum;
    private Integer reportNum;
    private Boolean isMine;
    //认证次数，3次以上需要咨询客服
    private Integer authNum;
    //是否进行了本人照片认证
    private Boolean isSelfAuth;

    private Integer experience;
    private Integer shell;
    //经验等级
    private Integer gradeLevel;
    private Integer wealthLevel;
    //是否显示发送消息需要支付5B标示
    //通过对方是否关注了你，和你是否已经支付过5b
    private Boolean allowSendMsg;
    //通过对方是否关注了你，你是否是对方的好友，如果不是，你是否已经购买过msg
//    private Boolean showBuyMsg;
    private ChatVO chat;

    public UserDetailVO() {
    }

    public UserDetailVO(UserDO user) {
        this(user, false);
    }

    public UserDetailVO(UserDO user, UserDO mineUser) {
        this(user, false, mineUser);
    }

    public UserDetailVO(UserDO user, Boolean isMine) {
        this(user, isMine, null);
    }

    public UserDetailVO(UserDO user, Boolean isMine, UserDO mineUser) {
        this.isMine = isMine;
        //toDO 查询mine的时候显示手机号，出生年月，别人详情时后台就控制不返回此类信息
        this.id = user.getId();
        this.nickname = StringUtils.substring(user.getNickname(), 0, 6);
        this.gender = user.getGender();
        this.location = user.getLocation();
        this.age = user.getAge();
        //满分10W /1千，得到百分之颜值分
        this.faceRatio = (int) Math.ceil((double) user.getFaceRatio() / MatchConstants.FACE_RATIO_BASE_MULTIPLE);
        //todo 这里可以修改为用户存着LIst《userImdId》然后每次不需要连表查询，只根据id查找就行
        List<UserImgDO> imgDOS = userImgRepository.findTop3ByUserIdAndStatusInOrderByCreateTimeDesc(user.getId(), CommonStatus.otherCanSeeContentStatus);
        this.imgs = UserImgVO.userImgDOToVOS(imgDOS);
        this.onlineFlag = user.getOnlineFlag();
        this.lastOnlineTime = user.getLastOnlineTime();
        this.vipFlag = user.getVipFlag();
        this.avatar = user.getAvatar();
        this.fansNum = user.getFansNum();
        this.loveValue = user.getLoveValue();
        this.justiceValue = user.getJusticeValue();
        this.userType = user.getType();
        this.isSelfAuth = user.getIsSelfAuth();
//        this.inviteCode = user.getInviteCode();
//        this.yearVipFlag = user.getYearVipFlag();
//        this.registerInviteUser = new MatchUserVO(user.getRegisterInviteUser());
//        this.qcb = user.getQcb();
        Optional<UserDetailDO> optionalUserDetailDO = userDetailRepository.findFirstOneByUserId(user.getId());
        optionalUserDetailDO.ifPresent(userDetailDO -> {
            //this.talkQueryDistrict = new DistrictVO(userDetailDO.getTalkQueryDistrict());
            this.wxAccount = userDetailDO.getWxAccount();
            this.qqAccount = userDetailDO.getQqAccount();
            this.wbAccount = userDetailDO.getWbAccount();
            this.openContact = userDetailDO.getOpenContact();

            this.beSponsorMsgShell = userDetailDO.getBeSponsorMsgShell();
            this.allowSendMsg = false;

            String contactAccount = userDetailDO.getContactAccount();
            if (isMine) {
                this.contactAccount = contactAccount;
                this.experience = userDetailDO.getExperience();
            } else {
                //如果未填写，则显示未填写
                //还有对方是否填写了联系方式
                if (StringUtils.isEmpty(contactAccount)) {
                    this.contactAccount = "";
                } else {
                    //是否开启了
                    if (this.openContact) {
                        //需要判断用户是否开启了openContact.如果未开启，则showUserContact为false
                        this.contactAccount = "***" + StringUtils.substring(contactAccount, -3);
                        this.showUserContact = false;
                        //如果为查看别人的详情，则会带着自己的用户信息
                        if (mineUser != null) {
                            Optional<UserContactDO> userContactDOOptional = userContactRepository.findFirstByUserIdAndBeUserIdAndStatusAndType(
                                    mineUser.getId(), user.getId(), CommonStatus.normal, ExpenseType.contact);
                            if (userContactDOOptional.isPresent()) {
                                //这里需要确认用户是否已获取过对方的联系方式
                                this.contactAccount = contactAccount;
                                this.showUserContact = true;
                            }
                        }
                    }
                }
            }
        });
        //如果为自己
        if (this.isMine) {
            //为自己才显示的内容
            //为自己不可关注
            this.hasFollowed = false;
            this.beFollow = false;
            String realPhoneNum = user.getPhoneNum();
            this.vipEndDate = user.getVipEndDate();
            this.likeCount = user.getLikeCount();
            this.birthday = user.getBirthday();
            this.idCardStatus = user.getIdCardStatus();
            this.followNum = user.getFollowNum();
            if (StringUtils.isNotEmpty(realPhoneNum)) {
                this.phoneNum = realPhoneNum.substring(0, 3) + "*****" + realPhoneNum.substring(8);
            }
            this.shell = user.getShell();

            this.authNum = user.getAuthNum();
            this.gradeLevel = user.getGradeLevel();
            this.wealthLevel = user.getWealthLevel();
        } else {
            if (mineUser != null && !mineUser.getId().equals(user.getId())) {
                Integer followCount = followRepository.countByUserIdAndBeUserIdAndStatus(mineUser.getId(), user.getId(), CommonStatus.normal);
                this.hasFollowed = followCount > 0;
                //查询对方是否关注了自己
                Integer beFollowCount = followRepository.countByUserIdAndBeUserIdAndStatus(user.getId(), mineUser.getId(), CommonStatus.normal);
                this.beFollow = beFollowCount > 0;
                //查询出来chatUser，用来判断用户是否购买了。
//                this.showBuyMsg = true;
                //如果被对方关注了，
                this.chat  = chatService.getSingleChatVO(mineUser, user.getId());
            } else {
                //未登录所有人都显示可关注
                this.hasFollowed = false;
                this.beFollow = false;
            }
            //他人需要判断
        }
        if (this.hasFollowed) {
            if (this.beFollow) {
                this.followStatus = FollowConst.eachFollow;
            } else {
                this.followStatus = FollowConst.followed;
            }
        } else {
            this.followStatus = FollowConst.follow;
        }
    }

    public static List<UserDetailVO> userDOToVOS(List<UserDO> userDOs) {
        return userDOs.stream().map(UserDetailVO::new).collect(Collectors.toList());
    }
}
