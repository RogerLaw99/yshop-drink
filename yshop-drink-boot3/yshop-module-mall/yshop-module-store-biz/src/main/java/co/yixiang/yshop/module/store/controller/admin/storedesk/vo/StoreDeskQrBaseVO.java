package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 桌台二维码 Base VO")
@Data
public class StoreDeskQrBaseVO {

    @Schema(description = "桌台编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "桌台编号不能为空")
    private Long deskId;

    @Schema(description = "二维码名称，如 A01-左", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "二维码名称不能为空")
    @Size(max = 64, message = "二维码名称长度不能超过 64 个字符")
    private String qrName;

    @Schema(description = "状态：0 禁用，1 启用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
