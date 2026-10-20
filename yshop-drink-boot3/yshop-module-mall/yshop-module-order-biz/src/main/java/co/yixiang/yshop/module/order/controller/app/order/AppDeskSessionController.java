package co.yixiang.yshop.module.order.controller.app.order;

import co.yixiang.yshop.framework.common.pojo.CommonResult;
import co.yixiang.yshop.framework.security.core.annotations.PreAuthenticated;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCartAddParam;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCartUpdateParam;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCheckoutParam;
import co.yixiang.yshop.module.order.controller.app.order.vo.AppDeskSessionRespVO;
import co.yixiang.yshop.module.order.service.desksession.DeskSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static co.yixiang.yshop.framework.common.pojo.CommonResult.success;
import static co.yixiang.yshop.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 APP - 桌台共享购物车")
@RestController
@RequestMapping("/order/desk")
@Validated
public class AppDeskSessionController {

    @Resource
    private DeskSessionService deskSessionService;

    @PreAuthenticated
    @GetMapping("/session")
    @Operation(summary = "扫码加入桌台会话")
    @Parameter(name = "token", description = "二维码令牌", required = true)
    public CommonResult<AppDeskSessionRespVO> joinSession(@RequestParam("token") String token) {
        return success(deskSessionService.joinSession(token, getLoginUserId()));
    }

    @PreAuthenticated
    @GetMapping("/session/by-id")
    @Operation(summary = "获取桌台会话详情")
    @Parameter(name = "sessionId", description = "会话编号", required = true)
    public CommonResult<AppDeskSessionRespVO> getSession(@RequestParam("sessionId") Long sessionId) {
        return success(deskSessionService.getSession(sessionId));
    }

    @PreAuthenticated
    @PostMapping("/cart/add")
    @Operation(summary = "添加商品到共享购物车")
    public CommonResult<AppDeskSessionRespVO> addCartItem(@Valid @RequestBody AppDeskCartAddParam param) {
        return success(deskSessionService.addCartItem(getLoginUserId(), param));
    }

    @PreAuthenticated
    @PostMapping("/cart/update")
    @Operation(summary = "修改共享购物车商品数量")
    public CommonResult<AppDeskSessionRespVO> updateCartItem(@Valid @RequestBody AppDeskCartUpdateParam param) {
        return success(deskSessionService.updateCartItem(getLoginUserId(), param));
    }

    @PreAuthenticated
    @DeleteMapping("/cart/delete")
    @Operation(summary = "删除共享购物车商品")
    @Parameter(name = "id", description = "购物车项编号", required = true)
    public CommonResult<AppDeskSessionRespVO> deleteCartItem(@RequestParam("id") Long id) {
        return success(deskSessionService.deleteCartItem(getLoginUserId(), id));
    }

    @PreAuthenticated
    @PostMapping("/cart/clear")
    @Operation(summary = "清空共享购物车")
    @Parameter(name = "sessionId", description = "会话编号", required = true)
    public CommonResult<Boolean> clearCart(@RequestParam("sessionId") Long sessionId) {
        deskSessionService.clearCart(getLoginUserId(), sessionId);
        return success(true);
    }

    @PreAuthenticated
    @PostMapping("/checkout")
    @Operation(summary = "共享购物车结算，生成一笔订单")
    public CommonResult<Map<String, Object>> checkout(@Valid @RequestBody AppDeskCheckoutParam param) {
        return success(deskSessionService.checkout(getLoginUserId(), param));
    }
}
