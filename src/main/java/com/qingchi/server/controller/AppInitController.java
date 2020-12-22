package com.qingchi.server.controller;

import com.qingchi.base.config.AppConfigConst;
import com.qingchi.base.common.ConfigMapRefreshService;
import com.qingchi.base.common.ResultVO;
import com.qingchi.base.common.ViolationKeywordsService;
import com.qingchi.base.config.redis.RedisUtil;
import com.qingchi.base.config.websocket.WebsocketServer;
import com.qingchi.base.constant.AppUpdateType;
import com.qingchi.base.constant.CommonConst;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.constant.ViolateType;
import com.qingchi.base.model.system.FrontErrorLogDO;
import com.qingchi.base.model.system.HomeSwiperDO;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.modelVO.ChatVO;
import com.qingchi.base.platform.qq.QQPayResult;
import com.qingchi.base.redis.DistrictRedis;
import com.qingchi.base.redis.DistrictVO;
import com.qingchi.base.redis.TagRedis;
import com.qingchi.base.repository.district.DistrictRepository;
import com.qingchi.base.repository.log.FrontErrorLogRepository;
import com.qingchi.base.repository.config.HomeSwiperRepository;
import com.qingchi.base.repository.tag.TagRepository;
import com.qingchi.base.store.TagStoreUtils;
import com.qingchi.base.utils.QingLogger;
import com.qingchi.server.model.*;
import com.qingchi.server.platform.AliAPI;
import com.qingchi.server.service.ChatUserService;
import com.qingchi.server.service.LoginService;
import com.qingchi.server.service.TagService;
import com.qingchi.server.service.TalkQueryService;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author qinkaiyuan
 * @date 2019-09-28 11:09
 * 前端初始化内容
 */
@RestController
@RequestMapping("app")
public class AppInitController {
    public static final Logger logger = LoggerFactory.getLogger(AppInitController.class);

    @Resource
    private TagRepository tagRepository;
    @Resource
    private TagStoreUtils tagQueryRepository;
    @Resource
    private TagRedis tagRedis;
    @Resource
    private DistrictRepository districtRepository;
    @Resource
    private ViolationKeywordsService violationKeywordsService;
    @Resource
    private ConfigMapRefreshService configMapRefreshService;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ChatUserService chatUserService;
    @Resource
    private HomeSwiperRepository homeSwiperRepository;
    @Resource
    private TagService tagService;

    @Resource
    private DistrictRedis districtRedis;
    @Resource
    private TalkQueryService talkQueryService;
    @Resource
    private LoginService loginService;
    @Resource
    private FrontErrorLogRepository frontErrorLogRepository;

    @RequestMapping("test")
    public ResultVO<QQPayResult> testUrl(String msg) {
        /*String access_token = "4E7E4518643A94B528F812A3DAD9A667";
        String url = "https://graph.qq.com/oauth2.0/me?access_token=" + access_token + "&unionid=1";
        ResponseEntity<Object> responseEntity = restTemplate.getForEntity(url, Object.class);
        Object wxLoginResult = responseEntity.getBody();*/
        //https://graph.qq.com/oauth2.0/me?access_token=ACCESSTOKEN&unionid=1
        //AppLoginVO(openId=D912200A06C3CE1BD16B7568FF24F258,
        // access_token=4E7E4518643A94B528F812A3DAD9A667,
        // loginType=, provider=qq, platform=app, birthday=1994,
        // avatarUrl=http://qzapp.qlogo.cn/qzapp/1104455702/D912200A06C3CE1BD16B7568FF24F258/30,
        // gender=1, nickName=清池恋爱交友app客服, city=唐山)

        QQPayResult qqPayResult = new QQPayResult();
        qqPayResult.setReturn_msg("fsadf");
        qqPayResult.setPrepay_id("fsadf");
        qqPayResult.setResult_code("fsadf");
        qqPayResult.setRetcode("fsadf");
        qqPayResult.setAppid("fsadf");
        qqPayResult.setCode_url("fsadf");
        return new ResultVO<>(qqPayResult);
    }

    //    , @RequestBody TalkQueryVO queryVO
    /*@PostMapping("queryAppInitDataLoad")
    public ResultVO<?> queryAppInitDataLoad(UserDO user, @RequestBody TalkQueryVO queryVO) {
        String platform = queryVO.getPlatform();
        //如果为null，则为旧版本，提示更新
        if (platform == null) {
            // 发版之后这里提示异常
            return new ResultVO<>("应用版本过旧，不可使用，请前往应用商店升级为最新版本使用！");
        }
        AppInitDataVO appInitData;
        if (user != null) {
            *//*String userPlatform = user.getPlatform();
            //如果用户平台不为小程序，为app平台
            if (!PlatformType.mp.equals(userPlatform)) {

            }*//*
            appInitData = loginService.getUserInitData(user, false);
        } else {
            appInitData = new AppInitDataVO();
            //chats
            List<ChatVO> chatVOs = chatUserService.getChats();
            appInitData.setChats(chatVOs);
        }
        //定位相关
        String queryAdCode = queryVO.getAdCode();
        Boolean openPosition = queryVO.getOpenPosition();
        DistrictVO districtVO = new DistrictVO();
        //不为空，且不为已开启定位
        //不为空，或者为null（老版本），或者不为true，则开启定位不走这里
        if (queryAdCode != null && (openPosition == null || !openPosition)) {
            RectangleVO rectangleVO = AliAPI.getRectangle();
            //如果用户为初始，或者为定位状态
            if (CommonConst.initAdCode.equals(queryAdCode) || CommonConst.positionAdCode.equals(queryAdCode)) {
                if (rectangleVO != null) {
                    String curAdCode;
                    //获取用户首次定位城市
                    if (StringUtils.isEmpty(rectangleVO.getAdCode())) {
                        //判断不为空，并且取市区adCode
                        curAdCode = CommonConst.chinaDistrictCode;
                    } else {
                        curAdCode = rectangleVO.getAdCode();
                        curAdCode = StringUtils.substring(curAdCode, 0, 4) + "00";
                    }
                    if (districtDOOptional.isPresent()) {
                        districtVO = new DistrictVO(districtDOOptional.get());
                    }
                }
            }
            if (rectangleVO != null) {
                districtVO.setLon(rectangleVO.getLon());
                districtVO.setLat(rectangleVO.getLat());
            }
            appInitData.setDistrict(districtVO);
        }
        String homeType = queryVO.getHomeType();
        if (homeType != null) {
            if (queryVO.getLat() == null) {
                if (districtVO.getLat() != null) {
                    queryVO.setLat(districtVO.getLat());
                    queryVO.setLon(districtVO.getLon());
                }
            }
            // 不为关注，且user不为null，就是 为关注的时候必须存在用户
            ResultVO<List<TalkVO>> resultVO = talkQueryService.queryTalkListParamHandle(user, queryVO);
            if (resultVO.hasError()) {
                return resultVO;
            }
            appInitData.setTalks(resultVO.getData());
        }
        //appConfig
        appInitData.setAppConfig(AppConfigConst.appConfigMap);
        //homeSwipers
        List<HomeSwiperDO> homeSwiperDOS = homeSwiperRepository.findAllByStatusOrderByTopLevelAscIdDesc(CommonStatus.normal);
        appInitData.setHomeSwipers(homeSwiperDOS.stream().map(HomeSwiperVO::new).collect(Collectors.toList()));
        return new ResultVO<>(appInitData);
    }*/

    @PostMapping("queryAppInitDataLoad")
    public ResultVO<?> queryAppInitDataLoad(UserDO user, @RequestBody TalkQueryVO queryVO) {
        String platform = queryVO.getPlatform();
        //如果为null，则为旧版本，提示更新
        if (platform == null) {
            // 发版之后这里提示异常
            return new ResultVO<>("应用版本过旧，不可使用，请前往应用商店升级为最新版本使用！");
        }
        AppInitDataVO appInitData = new AppInitDataVO();
        //定位相关
        String queryAdCode = queryVO.getAdCode();
        Boolean openPosition = queryVO.getOpenPosition();
        DistrictVO districtVO = new DistrictVO();
        //不为空，且不为已开启定位
        //不为空，或者为null（老版本），或者不为true，则开启定位不走这里
        //前台开启了定位就不走这里了，跟没开启定位也没关系啊，
        //这里的主要逻辑就是，如果初始，给用户设置默认城市和经纬度
        //没有为queryAdCode没有为null的时候除了老版本可能。
        //老版本也给返回值也没问题啊
        //如果用户的位置为空

        //前台只有初始的时候，会设置district
        //未开启定位的时候会，设置lon和lat
        //未开启定位和初始，其他比如lon为null的时候也只能是未开启定位，或者初始，如果是后台获取不到，则只能靠查询说说的时候获取了，如果开启了定位也获取不到也只能能考查询获取了
        //如果用户为初始
        if (CommonConst.initAdCode.equals(queryAdCode)) {
            RectangleVO rectangleVO = AliAPI.getRectangle();
            String curAdCode;
            //如果后台定位不到位置
            if (rectangleVO != null) {
                //获取定位城市码
                curAdCode = rectangleVO.getAdCode();
                //获取用户首次定位城市
                if (StringUtils.isEmpty(curAdCode)) {
                    //判断不为空，并且取市区adCode
                    curAdCode = CommonConst.chinaDistrictCode;
                } else {
                    curAdCode = StringUtils.substring(curAdCode, 0, 4) + "00";
                }
                if (rectangleVO.getLon() != null) {
                    districtVO.setLon(rectangleVO.getLon());
                    districtVO.setLat(rectangleVO.getLat());
                }
            } else {
                curAdCode = CommonConst.chinaDistrictCode;
            }
            districtVO.setAdCode(curAdCode);

            //只有初始，或者queryCode是null，才设置lon和lat，他查的地方和他的地方不冲突
            appInitData.setDistrict(districtVO);
            //用户未在前台开启定位，需要靠后台获取经纬度定位
        } else if (BooleanUtils.isNotTrue(openPosition)) {
            RectangleVO rectangleVO = AliAPI.getRectangle();
            //只有满足这两个条件才修改 用户adcode
            //如果用户的位置为空
            if (rectangleVO != null && rectangleVO.getLon() != null) {
                //如果定位位置不为空，且用户未开启前台定位，则更新用户的地理位置,或者用户当前经纬度为null
                districtVO.setLon(rectangleVO.getLon());
                districtVO.setLat(rectangleVO.getLat());
            } else {
                districtVO.setLon(queryVO.getLon());
                districtVO.setLat(queryVO.getLat());
            }
        }

        String homeType = queryVO.getHomeType();
        if (homeType != null) {
            //使用最新的
            if (districtVO.getLat() != null) {
                queryVO.setLat(districtVO.getLat());
                queryVO.setLon(districtVO.getLon());
            }
            // 不为关注，且user不为null，就是 为关注的时候必须存在用户
            ResultVO<List<TalkVO>> resultVO = talkQueryService.queryTalkListParamHandle(user, queryVO);
            if (resultVO.hasError()) {
                return resultVO;
            }
            appInitData.setTalks(resultVO.getData());
        }
        //appConfig
        appInitData.setAppConfig(AppConfigConst.appConfigMap);
        //homeSwipers
        List<HomeSwiperDO> homeSwiperDOS = homeSwiperRepository.findAllByStatusOrderByTopLevelAscIdDesc(CommonStatus.enable);
        appInitData.setHomeSwipers(homeSwiperDOS.stream().map(HomeSwiperVO::new).collect(Collectors.toList()));
        return new ResultVO<>(appInitData);
    }


    @PostMapping("queryAppInitDataReady")
    public ResultVO<?> queryAppInitDataReady(UserDO user) {
        AppInitDataVO appInitData;
        if (user != null) {
            appInitData = loginService.getUserInitData(user, false);
        } else {
            appInitData = new AppInitDataVO();
            //chats
            List<ChatVO> chatVOs = chatUserService.getChats();
            appInitData.setChats(chatVOs);
        }
        //tags
        //前台搜索，和新增tag搜索时展示使用
        appInitData.setTags(tagRedis.getHotTags());
        //tagTypes
        appInitData.setTagTypes(tagRedis.getHotTagTypes());
        //reportTypes
        appInitData.setReportTypes(ViolateType.frontShowReportTypes);
        //districts
        List<DistrictVO> districtVOS = districtRedis.getHotDistricts();
        appInitData.setDistricts(districtVOS);
        //onlineUsersCount
        appInitData.setOnlineUsersCount(WebsocketServer.getOnlineCount());
        return new ResultVO<>(appInitData);
    }


    //    , @RequestBody TalkQueryVO queryVO
    //此接口已作废
    @PostMapping("queryAppInitData")
    public ResultVO<?> queryAppInitData(UserDO user, @RequestBody TalkQueryVO queryVO) {
        String platform = queryVO.getPlatform();
        //如果为null，则为旧版本，提示更新
        if (platform == null) {
            // 发版之后这里提示异常
            return new ResultVO<>("应用版本过旧，不可使用，请前往应用商店升级为最新版本使用！");
        }
        AppInitDataVO appInitData = new AppInitDataVO();
        //定位相关
        String queryAdCode = queryVO.getAdCode();
        Boolean openPosition = queryVO.getOpenPosition();
        DistrictVO districtVO = new DistrictVO();
        //不为空，且不为已开启定位
        //不为空，或者为null（老版本），或者不为true，则开启定位不走这里
        //前台开启了定位就不走这里了，跟没开启定位也没关系啊，
        //这里的主要逻辑就是，如果初始，给用户设置默认城市和经纬度
        //没有为queryAdCode没有为null的时候除了老版本可能。
        //老版本也给返回值也没问题啊
        //如果用户的位置为空

        //前台只有初始的时候，会设置district
        //未开启定位的时候会，设置lon和lat
        //未开启定位和初始，其他比如lon为null的时候也只能是未开启定位，或者初始，如果是后台获取不到，则只能靠查询说说的时候获取了，如果开启了定位也获取不到也只能能考查询获取了
        //如果用户为初始
        if (CommonConst.initAdCode.equals(queryAdCode)) {
            RectangleVO rectangleVO = AliAPI.getRectangle();
            String curAdCode;
            //如果后台定位不到位置
            if (rectangleVO != null) {
                //获取定位城市码
                curAdCode = rectangleVO.getAdCode();
                //获取用户首次定位城市
                if (StringUtils.isEmpty(curAdCode)) {
                    //判断不为空，并且取市区adCode
                    curAdCode = CommonConst.chinaDistrictCode;
                } else {
                    curAdCode = StringUtils.substring(curAdCode, 0, 4) + "00";
                }
                if (rectangleVO.getLon() != null) {
                    districtVO.setLon(rectangleVO.getLon());
                    districtVO.setLat(rectangleVO.getLat());
                }
            } else {
                curAdCode = CommonConst.chinaDistrictCode;
            }
            districtVO.setAdCode(curAdCode);

            //只有初始，或者queryCode是null，才设置lon和lat，他查的地方和他的地方不冲突
            appInitData.setDistrict(districtVO);
            //用户未在前台开启定位，需要靠后台获取经纬度定位
        } else if (BooleanUtils.isNotTrue(openPosition)) {
            RectangleVO rectangleVO = AliAPI.getRectangle();
            //只有满足这两个条件才修改 用户adcode
            //如果用户的位置为空
            if (rectangleVO != null && rectangleVO.getLon() != null) {
                //如果定位位置不为空，且用户未开启前台定位，则更新用户的地理位置,或者用户当前经纬度为null
                districtVO.setLon(rectangleVO.getLon());
                districtVO.setLat(rectangleVO.getLat());
            } else {
                districtVO.setLon(queryVO.getLon());
                districtVO.setLat(queryVO.getLat());
            }
        }

        String homeType = queryVO.getHomeType();
        if (homeType != null) {
            //使用最新的
            if (districtVO.getLat() != null) {
                queryVO.setLat(districtVO.getLat());
                queryVO.setLon(districtVO.getLon());
            }
            // 不为关注，且user不为null，就是 为关注的时候必须存在用户
            ResultVO<List<TalkVO>> resultVO = talkQueryService.queryTalkListParamHandle(user, queryVO);
            if (resultVO.hasError()) {
                return resultVO;
            }
            appInitData.setTalks(resultVO.getData());
        }

        //appConfig
        appInitData.setAppConfig(AppConfigConst.appConfigMap);
        //tags
        appInitData.setTags(tagRedis.getHotTags());
        //tagTypes
        appInitData.setTagTypes(tagRedis.getHotTagTypes());
        //reportTypes
        appInitData.setReportTypes(ViolateType.frontShowReportTypes);
        //homeSwipers
        List<HomeSwiperDO> homeSwiperDOS = homeSwiperRepository.findAllByStatusOrderByTopLevelAscIdDesc(CommonStatus.enable);
        appInitData.setHomeSwipers(homeSwiperDOS.stream().map(HomeSwiperVO::new).collect(Collectors.toList()));
        //districts
        List<DistrictVO> districtVOS = districtRedis.getHotDistricts();
        appInitData.setDistricts(districtVOS);
        appInitData.setDistrictProvinces(districtVOS);
        //onlineUsersCount
        appInitData.setOnlineUsersCount(WebsocketServer.getOnlineCount());
        return new ResultVO<>(appInitData);
    }

    @RequestMapping("refreshRedis")
    public ResultVO<String> redisOnlineCount(String key) {
        ResultVO<String> resultVO = new ResultVO<>();
        if ("qky".equals(key)) {
            redisUtil.del(WebsocketServer.onlineUsersCountKey);
            redisUtil.set(WebsocketServer.onlineUsersCountKey, 0);
            QingLogger.logger.info("redis:{}", redisUtil.get(WebsocketServer.onlineUsersCountKey));
            resultVO.setData("刷新成功");
        } else {
            resultVO.setData("刷新失败");
        }
        return resultVO;
    }

    @RequestMapping("refreshKeywords")
    public ResultVO<String> refreshKeywords(String key) {
        ResultVO<String> resultVO = new ResultVO<>();
        if ("qky".equals(key)) {
            violationKeywordsService.refreshKeywords();
            resultVO.setData("刷新成功");
        } else {
            resultVO.setData("刷新失败");
        }
        return resultVO;
    }

    @RequestMapping("refreshConfigMap")
    public ResultVO<String> refreshConfigMap(String key) {
        ResultVO<String> resultVO = new ResultVO<>();
        if ("qky".equals(key)) {
            configMapRefreshService.refreshConfigMap();
            resultVO.setData("刷新成功");
        } else {
            resultVO.setData("刷新失败");
        }
        return resultVO;
    }

    @PostMapping("checkUpdate")
    public ResultVO<AppUpdateResultVO> checkUpdate(@RequestBody @Valid @NotNull AppUpdateVO updateVO) {
        Integer appHotUpdateVersion = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.appHotUpdateVersionKey);
        Integer appAppUpdateVersion = (Integer) AppConfigConst.appConfigMap.get(AppConfigConst.appAppUpdateVersionKey);

        Integer appVersion = updateVO.getVersion();

        AppUpdateResultVO appUpdateResultVO;
        //如果小于最低可用版本，升级app
        if (appVersion < appAppUpdateVersion) {
            appUpdateResultVO = new AppUpdateResultVO(AppUpdateType.install, AppConfigConst.appMarketUrl, "应用有新版本需要安装，点击安装即可更新");
            //否则，如果小于当前版本，则热更新。
        } else if (appVersion < appHotUpdateVersion) {
            appUpdateResultVO = new AppUpdateResultVO(AppUpdateType.hot, "wegturl");
            //否则，不需要更新
        } else {
            appUpdateResultVO = new AppUpdateResultVO(AppUpdateType.none);
        }
        return new ResultVO<>(appUpdateResultVO);
    }


    @PostMapping("sendErrorLog")
    public ResultVO<Object> sendErrorLog(UserDO user, @RequestBody FrontErrorLogVO frontErrorLogVO) {
        FrontErrorLogDO frontErrorLogDO = frontErrorLogVO.toDO(user);
        frontErrorLogRepository.save(frontErrorLogDO);
        return new ResultVO<>();
    }
}
