package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 桌台二维码 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StoreDeskQrRespVO extends StoreDeskQrBaseVO {

    @Schema(description = "二维码编号")
    private Long id;

    @Schema(description = "门店编号")
    private Long shopId;

    @Schema(description = "二维码令牌")
    private String qrToken;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
