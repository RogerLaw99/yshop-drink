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
 * 门店桌台 DO
 *
 * @author yshop
 */
@TableName("yshop_store_desk")
@KeySequence("yshop_store_desk_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDeskDO extends BaseDO {

    /**
     * 桌台编号
     */
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
     * 桌号
     */
    private String deskNumber;
    /**
     * 区域/楼层
     */
    private String area;
    /**
     * 座位数
     */
    private Integer capacity;
    /**
     * 状态：0 禁用，1 启用
     */
    private Integer status;
    /**
     * 二维码令牌（不可猜测）
     */
    private String qrToken;
    /**
     * 排序
     */
    private Integer sort;
}
