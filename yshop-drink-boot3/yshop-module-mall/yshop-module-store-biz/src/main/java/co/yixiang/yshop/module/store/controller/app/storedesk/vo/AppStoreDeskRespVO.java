package co.yixiang.yshop.module.store.controller.app.storedesk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - 桌台信息 Response VO")
@Data
public class AppStoreDeskRespVO {

    @Schema(description = "桌台编号", example = "1")
    private Long id;

    @Schema(description = "租户编号", example = "1001")
    private Long tenantId;

    @Schema(description = "门店编号", example = "3")
    private Long shopId;

    @Schema(description = "桌号", example = "A01")
    private String deskNumber;

    @Schema(description = "区域/楼层", example = "大厅")
    private String area;

    @Schema(description = "座位数", example = "4")
    private Integer capacity;

    @Schema(description = "二维码编号", example = "1")
    private Long qrId;

    @Schema(description = "二维码名称，如 A01-左", example = "A01-左")
    private String qrName;
}
