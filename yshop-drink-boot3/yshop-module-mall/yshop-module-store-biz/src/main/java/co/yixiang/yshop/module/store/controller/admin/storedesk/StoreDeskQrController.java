package co.yixiang.yshop.module.store.controller.admin.storedesk;

import cn.hutool.core.bean.BeanUtil;
import co.yixiang.yshop.framework.common.pojo.CommonResult;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskQrCreateReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskQrRespVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskQrUpdateReqVO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskQrDO;
import co.yixiang.yshop.module.store.service.storedesk.StoreDeskQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static co.yixiang.yshop.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 桌台二维码")
@RestController
@RequestMapping("/store/desk/qr")
@Validated
public class StoreDeskQrController {

    @Resource
    private StoreDeskQrService qrService;

    @PostMapping("/create")
    @Operation(summary = "创建桌台二维码")
    @PreAuthorize("@ss.hasPermission('store:desk:create')")
    public CommonResult<Long> createQr(@Valid @RequestBody StoreDeskQrCreateReqVO createReqVO) {
        return success(qrService.createQr(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新桌台二维码")
    @PreAuthorize("@ss.hasPermission('store:desk:update')")
    public CommonResult<Boolean> updateQr(@Valid @RequestBody StoreDeskQrUpdateReqVO updateReqVO) {
        qrService.updateQr(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除桌台二维码")
    @Parameter(name = "id", description = "二维码编号", required = true)
    @PreAuthorize("@ss.hasPermission('store:desk:delete')")
    public CommonResult<Boolean> deleteQr(@RequestParam("id") Long id) {
        qrService.deleteQr(id);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获得桌台的二维码列表")
    @Parameter(name = "deskId", description = "桌台编号", required = true)
    @PreAuthorize("@ss.hasPermission('store:desk:query')")
    public CommonResult<List<StoreDeskQrRespVO>> getQrList(@RequestParam("deskId") Long deskId) {
        return success(qrService.getListByDeskId(deskId).stream()
                .map(this::convert).collect(Collectors.toList()));
    }

    @PostMapping("/reset-token")
    @Operation(summary = "重置二维码令牌")
    @Parameter(name = "id", description = "二维码编号", required = true)
    @PreAuthorize("@ss.hasPermission('store:desk:update')")
    public CommonResult<String> resetToken(@RequestParam("id") Long id) {
        return success(qrService.resetToken(id));
    }

    private StoreDeskQrRespVO convert(StoreDeskQrDO qr) {
        StoreDeskQrRespVO vo = new StoreDeskQrRespVO();
        BeanUtil.copyProperties(qr, vo);
        return vo;
    }
}
