package co.yixiang.yshop.module.order.dal.dataobject.deskcart;

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

import java.math.BigDecimal;

/**
 * 桌台共享购物车 DO
 *
 * @author yshop
 */
@TableName("yshop_store_desk_cart")
@KeySequence("yshop_store_desk_cart_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeskCartDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 会话编号
     */
    private Long sessionId;
    /**
     * 门店编号
     */
    private Long shopId;
    /**
     * 添加人用户编号
     */
    private Long uid;
    /**
     * 添加人昵称
     */
    private String nickname;
    /**
     * 商品编号
     */
    private Long productId;
    /**
     * 规格（逗号分隔）
     */
    private String sku;
    /**
     * 规格展示文本
     */
    private String specText;
    /**
     * 商品名称
     */
    private String name;
    /**
     * 商品图片
     */
    private String image;
    /**
     * 单价
     */
    private BigDecimal price;
    /**
     * 数量
     */
    private Integer number;
}
