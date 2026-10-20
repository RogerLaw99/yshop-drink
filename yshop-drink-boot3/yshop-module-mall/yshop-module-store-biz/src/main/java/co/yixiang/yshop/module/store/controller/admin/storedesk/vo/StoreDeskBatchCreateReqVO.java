package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 桌台批量创建 Request VO")
@Data
public class StoreDeskBatchCreateReqVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "门店不能为空")
    private Long shopId;

    @Schema(description = "桌号前缀", example = "A")
    @NotBlank(message = "桌号前缀不能为空")
    @Size(max = 16, message = "桌号前缀长度不能超过 16 个字符")
    private String prefix;

    @Schema(description = "起始序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "起始序号不能为空")
    @Min(value = 1, message = "起始序号必须大于 0")
    private Integer startNumber;

    @Schema(description = "结束序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "结束序号不能为空")
    @Max(value = 9999, message = "结束序号不能超过 9999")
    private Integer endNumber;

    @Schema(description = "区域/楼层", example = "大厅")
    private String area;

    @Schema(description = "座位数", example = "4")
    private Integer capacity;
}
