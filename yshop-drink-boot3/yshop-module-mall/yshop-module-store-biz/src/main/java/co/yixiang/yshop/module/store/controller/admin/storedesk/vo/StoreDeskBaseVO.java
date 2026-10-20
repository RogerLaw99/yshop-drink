package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 桌台 Base VO")
@Data
public class StoreDeskBaseVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "门店不能为空")
    private Long shopId;

    @Schema(description = "桌号", requiredMode = Schema.RequiredMode.REQUIRED, example = "A01")
    @NotBlank(message = "桌号不能为空")
    @Size(max = 32, message = "桌号长度不能超过 32 个字符")
    private String deskNumber;

    @Schema(description = "区域/楼层", example = "大厅")
    @Size(max = 64, message = "区域长度不能超过 64 个字符")
    private String area;

    @Schema(description = "座位数", example = "4")
    private Integer capacity;

    @Schema(description = "状态：0 禁用，1 启用", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;
}
