package co.yixiang.yshop.module.order.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - 共享购物车项")
@Data
public class AppDeskCartItemVO {

    @Schema(description = "购物车项编号")
    private Long id;

    @Schema(description = "商品编号")
    private Long productId;

    @Schema(description = "规格（逗号分隔）")
    private String sku;

    @Schema(description = "规格展示文本")
    private String specText;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "单价")
    private BigDecimal price;

    @Schema(description = "数量")
    private Integer number;

    @Schema(description = "添加人用户编号")
    private Long uid;

    @Schema(description = "添加人昵称")
    private String nickname;
}
