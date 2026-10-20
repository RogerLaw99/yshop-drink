package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 桌台二维码更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StoreDeskQrUpdateReqVO extends StoreDeskQrBaseVO {

    @Schema(description = "二维码编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "二维码编号不能为空")
    private Long id;
}
