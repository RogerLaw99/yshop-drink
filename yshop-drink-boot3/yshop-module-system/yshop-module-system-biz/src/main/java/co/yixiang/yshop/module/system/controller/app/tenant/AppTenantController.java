package co.yixiang.yshop.module.system.controller.app.tenant;

import cn.hutool.core.bean.BeanUtil;
import co.yixiang.yshop.framework.common.pojo.CommonResult;
import co.yixiang.yshop.framework.tenant.core.aop.TenantIgnore;
import co.yixiang.yshop.module.system.controller.app.tenant.vo.AppTenantRespVO;
import co.yixiang.yshop.module.system.dal.dataobject.tenant.TenantDO;
import co.yixiang.yshop.module.system.dal.mysql.tenant.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static co.yixiang.yshop.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 APP - 商户（租户）")
@RestController
@RequestMapping("/tenant")
@Validated
public class AppTenantController {

    @Resource
    private TenantMapper tenantMapper;

    @GetMapping("/list")
    @Operation(summary = "获取可用的商户列表")
    @TenantIgnore
    public CommonResult<List<AppTenantRespVO>> getTenantList() {
        List<TenantDO> list = tenantMapper.selectList(new LambdaQueryWrapper<TenantDO>()
                .eq(TenantDO::getStatus, 0)
                .gt(TenantDO::getExpireTime, LocalDateTime.now())
                .orderByAsc(TenantDO::getId));
        return success(list.stream().map(tenant -> {
            AppTenantRespVO vo = new AppTenantRespVO();
            BeanUtil.copyProperties(tenant, vo);
            return vo;
        }).collect(Collectors.toList()));
    }
}
