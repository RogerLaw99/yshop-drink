package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 桌台更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StoreDeskUpdateReqVO extends StoreDeskBaseVO {

    @Schema(description = "桌台编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "桌台编号不能为空")
    private Long id;
}
