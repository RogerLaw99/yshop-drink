package co.yixiang.yshop.module.order.dal.dataobject.desksession;

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

import java.time.LocalDateTime;

/**
 * 桌台会话 DO
 *
 * @author yshop
 */
@TableName("yshop_store_desk_session")
@KeySequence("yshop_store_desk_session_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeskSessionDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 门店编号
     */
    private Long shopId;
    /**
     * 桌台编号
     */
    private Long deskId;
    /**
     * 桌号
     */
    private String deskNumber;
    /**
     * 二维码编号
     */
    private Long qrId;
    /**
     * 二维码名称
     */
    private String qrName;
    /**
     * 状态：0 进行中，1 已下单，2 已取消
     */
    private Integer status;
    /**
     * 支付人用户编号
     */
    private Long payerUid;
    /**
     * 生成的订单号
     */
    private String orderId;
    /**
     * 结束时间
     */
    private LocalDateTime closeTime;
}
