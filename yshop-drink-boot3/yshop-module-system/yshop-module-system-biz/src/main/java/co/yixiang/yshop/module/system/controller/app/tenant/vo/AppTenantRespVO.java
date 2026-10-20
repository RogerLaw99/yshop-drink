package co.yixiang.yshop.module.system.controller.app.tenant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - 商户（租户）Response VO")
@Data
public class AppTenantRespVO {

    @Schema(description = "租户编号", example = "1")
    private Long id;

    @Schema(description = "商户名称", example = "某某餐饮")
    private String name;

    @Schema(description = "商户域名", example = "https://example.com")
    private String website;
}
