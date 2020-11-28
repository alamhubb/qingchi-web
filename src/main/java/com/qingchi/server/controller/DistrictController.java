package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.redis.DistrictRedis;
import com.qingchi.base.redis.DistrictVO;
import com.qingchi.base.repository.district.DistrictRepository;
import com.qingchi.base.repository.user.UserDistrictRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author qinkaiyuan
 * @date 2018-11-18 20:45
 */

@RestController
@RequestMapping("district")
public class DistrictController {
    @Resource
    private DistrictRepository districtRepository;

    @Resource
    private UserDistrictRepository userDistrictRepository;

    @Resource
    private DistrictRedis districtRedis;

    //共前端城市选择picker使用，前台仅从这里使用了一个借口
    @PostMapping("queryProvinceDistricts")
    public ResultVO<List<DistrictVO>> queryProvinceDistricts() {
        return new ResultVO<>(districtRedis.getAllDistricts());
    }

    //11.21查看此接口应该已作废
    @PostMapping("queryHotProvinceDistricts")
    public ResultVO<List<DistrictVO>> queryHotProvinceDistricts() {
        return new ResultVO<>(districtRedis.getHotDistricts());
    }
/*
    //11.21查看此接口应该已作废
    @PostMapping("queryDistricts")
    public ResultVO<List<DistrictVO>> queryDistricts() {
        return new ResultVO<>(DistrictVO.districtDOToVOS(districtRepository.findAllByStatusOrderByAdCode(CommonStatus.normal)));
    }

    //11.21查看此接口应该已作废
    @PostMapping("queryHotDistricts")
    public ResultVO<List<DistrictVO>> queryHotDistricts() {
        List<DistrictDO> districtDOS = districtRepository.findTop16ByStatusOrderByCountDesc(CommonStatus.normal);
        return new ResultVO<>(DistrictVO.districtDOToVOS(districtDOS));
    }*/

    /*@PostMapping("queryUserRecentlyDistricts")
    public ResultVO<List<DistrictVO>> queryUserRecentlyDistricts(UserDO user) {
        List<UserDistrictDO> districtDOS = userDistrictRepository.findTop7ByUserIdOrderByUpdateTimeDesc(user.getId());

        List<DistrictVO> districtVOS = DistrictVO.districtDOToVOS(districtDOS.stream().map(userDistrictDO -> {
            Optional<DistrictDO> districtDOOptional = districtRepository.findFirstOneByAdCode(userDistrictDO.getDistrictAdCode());
            return districtDOOptional.get();
        }).collect(Collectors.toList()));
        return new ResultVO<>(districtVOS);
    }*/

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
