package com.qingchi.server.controller;

import com.qingchi.base.common.ResultVO;
import com.qingchi.base.constant.CommonStatus;
import com.qingchi.base.model.user.UserDO;
import com.qingchi.base.model.user.ShellOrderDO;
import com.qingchi.base.repository.shell.ShellOrderRepository;
import com.qingchi.server.model.ShellOrderVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("shell")
public class ShellController {
    @Resource
    private ShellOrderRepository shellOrderRepository;

    @PostMapping("queryShells")
    public ResultVO<List<ShellOrderVO>> queryShells(UserDO user) {
        List<ShellOrderDO> shellOrderDOS = shellOrderRepository.findAllByUserIdAndStatusOrderByCreateTimeDesc(user.getId(), CommonStatus.enable);
        return new ResultVO<>(shellOrderDOS.stream().map(ShellOrderVO::new).collect(Collectors.toList()));
    }
}
