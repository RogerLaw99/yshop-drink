package co.yixiang.yshop.module.store.dal.dataobject.storedesk;

import co.yixiang.yshop.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 桌台二维码 DO
 *
 * @author yshop
 */
@TableName("yshop_store_desk_qr")
@KeySequence("yshop_store_desk_qr_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDeskQrDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 租户编号
     */
    private Long tenantId;
    /**
     * 门店编号
     */
    private Long shopId;
    /**
     * 桌台编号
     */
    private Long deskId;
    /**
     * 二维码名称，如 A01-左
     */
    private String qrName;
    /**
     * 二维码令牌
     */
    private String qrToken;
    /**
     * 状态：0 禁用，1 启用
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
}
