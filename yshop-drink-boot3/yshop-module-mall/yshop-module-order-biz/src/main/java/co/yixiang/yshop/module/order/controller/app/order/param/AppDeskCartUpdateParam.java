package co.yixiang.yshop.module.order.controller.app.order.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppDeskCartUpdateParam {

    @Schema(description = "购物车项编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "购物车项编号不能为空")
    private Long id;

    @Schema(description = "数量，0 表示删除", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能小于 0")
    private Integer number;
}
