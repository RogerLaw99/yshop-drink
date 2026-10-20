package co.yixiang.yshop.module.order.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - 桌台订单汇总 Response VO")
@Data
public class AppDeskOrderSummaryVO {

    @Schema(description = "桌台编号", example = "1")
    private Long deskId;

    @Schema(description = "进行中的订单数", example = "3")
    private Integer orderCount;

    @Schema(description = "进行中订单合计金额", example = "128.50")
    private BigDecimal totalAmount;
}
