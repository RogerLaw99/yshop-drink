package co.yixiang.yshop.module.store.controller.app.storedesk;

import cn.hutool.core.bean.BeanUtil;
import co.yixiang.yshop.framework.common.pojo.CommonResult;
import co.yixiang.yshop.framework.tenant.core.aop.TenantIgnore;
import co.yixiang.yshop.module.store.controller.app.storedesk.vo.AppStoreDeskRespVO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskQrDO;
import co.yixiang.yshop.module.store.service.storedesk.StoreDeskQrService;
import co.yixiang.yshop.module.store.service.storedesk.StoreDeskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static co.yixiang.yshop.framework.common.exception.util.ServiceExceptionUtil.exception;
import static co.yixiang.yshop.framework.common.pojo.CommonResult.success;
import static co.yixiang.yshop.module.store.enums.ErrorCodeConstants.STORE_DESK_NOT_EXISTS;

@Tag(name = "用户 APP - 桌台")
@RestController
@RequestMapping("/store/desk")
@Validated
public class AppStoreDeskController {

    @Resource
    private StoreDeskService deskService;
    @Resource
    private StoreDeskQrService qrService;

    @GetMapping("/get")
    @Operation(summary = "根据二维码令牌获得桌台信息")
    @Parameter(name = "token", description = "二维码令牌", required = true)
    @TenantIgnore
    public CommonResult<AppStoreDeskRespVO> getDeskByToken(@RequestParam("token") String token) {
        StoreDeskQrDO qr = qrService.getByToken(token);
        if (qr == null || qr.getStatus() == null || qr.getStatus() != 1) {
            throw exception(STORE_DESK_NOT_EXISTS);
        }
        StoreDeskDO desk = deskService.getDeskById(qr.getDeskId());
        if (desk == null || desk.getStatus() == null || desk.getStatus() != 1) {
            throw exception(STORE_DESK_NOT_EXISTS);
        }
        AppStoreDeskRespVO vo = new AppStoreDeskRespVO();
        BeanUtil.copyProperties(desk, vo);
        vo.setTenantId(desk.getTenantId() != null ? desk.getTenantId() : qr.getTenantId());
        vo.setQrId(qr.getId());
        vo.setQrName(qr.getQrName());
        return success(vo);
    }
}
