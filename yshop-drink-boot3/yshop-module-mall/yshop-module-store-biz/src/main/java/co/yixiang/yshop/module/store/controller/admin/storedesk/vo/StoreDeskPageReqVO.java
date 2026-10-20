package co.yixiang.yshop.module.store.controller.admin.storedesk.vo;

import co.yixiang.yshop.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 桌台分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StoreDeskPageReqVO extends PageParam {

    @Schema(description = "门店编号", example = "3")
    private Long shopId;

    @Schema(description = "桌号", example = "A01")
    private String deskNumber;

    @Schema(description = "区域/楼层", example = "大厅")
    private String area;

    @Schema(description = "状态：0 禁用，1 启用", example = "1")
    private Integer status;
}
