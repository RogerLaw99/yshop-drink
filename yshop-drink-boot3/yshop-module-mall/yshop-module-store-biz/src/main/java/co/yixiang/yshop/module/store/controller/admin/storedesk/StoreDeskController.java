package co.yixiang.yshop.module.store.controller.admin.storedesk;

import cn.hutool.core.bean.BeanUtil;
import co.yixiang.yshop.framework.common.pojo.CommonResult;
import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.*;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;
import co.yixiang.yshop.module.store.service.storedesk.StoreDeskService;
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

@Tag(name = "管理后台 - 桌台管理")
@RestController
@RequestMapping("/store/desk")
@Validated
public class StoreDeskController {

    @Resource
    private StoreDeskService deskService;

    @PostMapping("/create")
    @Operation(summary = "创建桌台")
    @PreAuthorize("@ss.hasPermission('store:desk:create')")
    public CommonResult<Long> createDesk(@Valid @RequestBody StoreDeskCreateReqVO createReqVO) {
        return success(deskService.createDesk(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新桌台")
    @PreAuthorize("@ss.hasPermission('store:desk:update')")
    public CommonResult<Boolean> updateDesk(@Valid @RequestBody StoreDeskUpdateReqVO updateReqVO) {
        deskService.updateDesk(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除桌台")
    @Parameter(name = "id", description = "桌台编号", required = true)
    @PreAuthorize("@ss.hasPermission('store:desk:delete')")
    public CommonResult<Boolean> deleteDesk(@RequestParam("id") Long id) {
        deskService.deleteDesk(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得桌台")
    @Parameter(name = "id", description = "桌台编号", required = true)
    @PreAuthorize("@ss.hasPermission('store:desk:query')")
    public CommonResult<StoreDeskRespVO> getDesk(@RequestParam("id") Long id) {
        return success(convert(deskService.getDesk(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得桌台分页")
    @PreAuthorize("@ss.hasPermission('store:desk:query')")
    public CommonResult<PageResult<StoreDeskRespVO>> getDeskPage(@Valid StoreDeskPageReqVO pageReqVO) {
        PageResult<StoreDeskDO> pageResult = deskService.getDeskPage(pageReqVO);
        return success(new PageResult<>(
                pageResult.getList().stream().map(this::convert).collect(Collectors.toList()),
                pageResult.getTotal()));
    }

    @GetMapping("/list-by-shop")
    @Operation(summary = "获得门店下的桌台列表")
    @Parameter(name = "shopId", description = "门店编号", required = true)
    @PreAuthorize("@ss.hasPermission('store:desk:query')")
    public CommonResult<List<StoreDeskRespVO>> getDeskListByShop(@RequestParam("shopId") Long shopId) {
        return success(deskService.getDeskListByShopId(shopId).stream()
                .map(this::convert).collect(Collectors.toList()));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建桌台")
    @PreAuthorize("@ss.hasPermission('store:desk:create')")
    public CommonResult<List<Long>> batchCreate(@Valid @RequestBody StoreDeskBatchCreateReqVO reqVO) {
        return success(deskService.batchCreate(reqVO));
    }

    @PostMapping("/reset-token")
    @Operation(summary = "重置桌台二维码令牌")
    @Parameter(name = "id", description = "桌台编号", required = true)
    @PreAuthorize("@ss.hasPermission('store:desk:update')")
    public CommonResult<String> resetToken(@RequestParam("id") Long id) {
        return success(deskService.resetQrToken(id));
    }

    private StoreDeskRespVO convert(StoreDeskDO desk) {
        if (desk == null) {
            return null;
        }
        StoreDeskRespVO vo = new StoreDeskRespVO();
        BeanUtil.copyProperties(desk, vo);
        return vo;
    }
}
