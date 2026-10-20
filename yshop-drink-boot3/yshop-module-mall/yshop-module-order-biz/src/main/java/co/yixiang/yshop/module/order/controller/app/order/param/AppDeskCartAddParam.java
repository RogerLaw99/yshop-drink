package co.yixiang.yshop.module.order.controller.app.order.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppDeskCartAddParam {

    @Schema(description = "会话编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会话编号不能为空")
    private Long sessionId;

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品编号不能为空")
    private Long productId;

    @Schema(description = "规格（逗号分隔）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规格不能为空")
    private String sku;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer number;
}
