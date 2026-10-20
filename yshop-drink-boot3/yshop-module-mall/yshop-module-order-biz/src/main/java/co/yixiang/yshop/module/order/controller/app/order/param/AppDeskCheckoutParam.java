package co.yixiang.yshop.module.order.controller.app.order.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppDeskCheckoutParam {

    @Schema(description = "会话编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会话编号不能为空")
    private Long sessionId;

    @Schema(description = "支付方式：weixin/yue/alipay", example = "yue")
    private String payType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "就餐人数")
    private Integer deskPeople;

    @Schema(description = "优惠券编号")
    private String couponId;

    @Schema(description = "联系电话")
    private String mobile;
}
