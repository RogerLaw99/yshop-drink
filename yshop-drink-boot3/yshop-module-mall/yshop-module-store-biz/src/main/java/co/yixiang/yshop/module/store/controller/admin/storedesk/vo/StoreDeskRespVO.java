package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 桌台 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StoreDeskRespVO extends StoreDeskBaseVO {

    @Schema(description = "桌台编号", example = "1")
    private Long id;

    @Schema(description = "二维码令牌")
    private String qrToken;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
