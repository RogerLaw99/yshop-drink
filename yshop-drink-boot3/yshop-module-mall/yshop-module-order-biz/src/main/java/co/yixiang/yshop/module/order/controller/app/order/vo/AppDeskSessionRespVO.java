package co.yixiang.yshop.module.order.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "用户 APP - 桌台共享会话 Response VO")
@Data
public class AppDeskSessionRespVO {

    @Schema(description = "会话编号")
    private Long sessionId;

    @Schema(description = "桌台编号")
    private Long deskId;

    @Schema(description = "门店编号")
    private Long shopId;

    @Schema(description = "桌号")
    private String deskNumber;

    @Schema(description = "二维码编号")
    private Long qrId;

    @Schema(description = "二维码名称")
    private String qrName;

    @Schema(description = "会话状态：0 进行中，1 已下单，2 已取消")
    private Integer status;

    @Schema(description = "购物车项")
    private List<AppDeskCartItemVO> cartList;

    @Schema(description = "商品总数量")
    private Integer totalNumber;

    @Schema(description = "商品总金额")
    private BigDecimal totalAmount;

    @Schema(description = "参与人数")
    private Integer participantCount;
}
