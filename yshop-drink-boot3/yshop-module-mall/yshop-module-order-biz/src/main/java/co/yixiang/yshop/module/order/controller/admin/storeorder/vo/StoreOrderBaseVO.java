package co.yixiang.yshop.module.order.controller.admin.storeorder.vo;

import co.yixiang.yshop.framework.desensitize.core.slider.annotation.MobileDesensitize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static co.yixiang.yshop.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
* 订单 Base VO，提供给添加、修改、详细的子 VO 使用
* 如果子 VO 存在差异的字段，请不要添加到这里，影响 Swagger 文档生成
*/
@Data
public class StoreOrderBaseVO {

    @Schema(description = "订单号", required = true, example = "20527")
    @NotNull(message = "订单号不能为空")
    private String orderId;

    @Schema(description = "额外订单号", example = "12452")
    private String extendOrderId;

    @Schema(description = "用户id", required = true, example = "8323")
    @NotNull(message = "用户id不能为空")
    private Long uid;

    @Schema(description = "用户姓名", required = true, example = "张三")
    //@NotNull(message = "用户姓名不能为空")
    private String realName;

    //@MobileDesensitize
    @Schema(description = "用户电话", required = true)
    //@NotNull(message = "用户电话不能为空")
    private String userPhone;

    @Schema(description = "详细地址", required = true)
   // @NotNull(message = "详细地址不能为空")
    private String userAddress;

    @Schema(description = "购物车id", required = true, example = "23301")
    //@NotNull(message = "购物车id不能为空")
    private String cartId;

    @Schema(description = "运费金额", required = true, example = "637")
    //@NotNull(message = "运费金额不能为空")
    private BigDecimal freightPrice;

    @Schema(description = "订单商品总数", required = true)
    //@NotNull(message = "订单商品总数不能为空")
    private Integer totalNum;

    @Schema(description = "订单总价", required = true, example = "31659")
    //@NotNull(message = "订单总价不能为空")
    private BigDecimal totalPrice;

    @Schema(description = "邮费", required = true)
   // @NotNull(message = "邮费不能为空")
    private BigDecimal totalPostage;

    @Schema(description = "实际支付金额", required = true, example = "19682")
   // @NotNull(message = "实际支付金额不能为空")
    private BigDecimal payPrice;

    @Schema(description = "支付邮费", required = true)
    //@NotNull(message = "支付邮费不能为空")
    private BigDecimal payPostage;

    @Schema(description = "抵扣金额", required = true, example = "16463")
    //@NotNull(message = "抵扣金额不能为空")
    private BigDecimal deductionPrice;

    @Schema(description = "优惠券id", required = true, example = "3299")
    //@NotNull(message = "优惠券id不能为空")
    private Integer couponId;

    @Schema(description = "优惠券金额", required = true, example = "22157")
    //@NotNull(message = "优惠券金额不能为空")
    private BigDecimal couponPrice;

    @Schema(description = "支付状态", required = true, example = "11728")
    //@NotNull(message = "支付状态不能为空")
    private Integer paid;

    @Schema(description = "支付时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime payTime;

    @Schema(description = "支付方式", required = true, example = "2")
    //@NotNull(message = "支付方式不能为空")
    private String payType;

    @Schema(description = "订单状态（-1 : 申请退款 -2 : 退货成功 0：待发货；1：待收货；2：已收货；3：已完成；-1：已退款）", required = true, example = "1")
    //@NotNull(message = "订单状态（-1 : 申请退款 -2 : 退货成功 0：待发货；1：待收货；2：已收货；3：已完成；-1：已退款）不能为空")
    private Integer status;

    @Schema(description = "0 未退款 1 申请中 2 已退款", required = true, example = "2")
    //@NotNull(message = "0 未退款 1 申请中 2 已退款不能为空")
    private Integer refundStatus;

    @Schema(description = "退款图片")
    private String refundReasonWapImg;

    @Schema(description = "退款用户说明")
    private String refundReasonWapExplain;

    @Schema(description = "退款时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime refundReasonTime;

    @Schema(description = "前台退款原因")
    private String refundReasonWap;

    @Schema(description = "不退款的理由", example = "不喜欢")
    private String refundReason;

    @Schema(description = "退款金额", required = true, example = "7547")
    //@NotNull(message = "退款金额不能为空")
    private BigDecimal refundPrice;

    @Schema(description = "快递公司编号")
    private String deliverySn;

    @Schema(description = "快递名称/送货人姓名", example = "张三")
    private String deliveryName;

    @Schema(description = "发货类型", example = "1")
    private String deliveryType;

    @Schema(description = "快递单号/手机号", example = "24798")
    private String deliveryId;

    @Schema(description = "消费赚取积分", required = true)
    //@NotNull(message = "消费赚取积分不能为空")
    private BigDecimal gainIntegral;

    @Schema(description = "使用积分", required = true)
    //@NotNull(message = "使用积分不能为空")
    private BigDecimal useIntegral;

    @Schema(description = "实际支付积分", required = true)
    //@NotNull(message = "实际支付积分不能为空")
    private BigDecimal payIntegral;

    @Schema(description = "给用户退了多少积分")
    private BigDecimal backIntegral;

    @Schema(description = "备注", required = true)
    //@NotNull(message = "备注不能为空")
    private String mark;

    @Schema(description = "唯一id(md5加密)类似id", required = true)
    //@NotNull(message = "唯一id(md5加密)类似id不能为空")
    private String unique;

    @Schema(description = "管理员备注", example = "随便")
    private String remark;

    @Schema(description = "商户ID", required = true, example = "8499")
    //@NotNull(message = "商户ID不能为空")
    private Integer merId;

    @Schema(description = "拼团产品id0一般产品", example = "3865")
    private Long combinationId;

    @Schema(description = "拼团id 0没有拼团", required = true, example = "8463")
    //@NotNull(message = "拼团id 0没有拼团不能为空")
    private Long pinkId;

    @Schema(description = "成本价", required = true)
    //@NotNull(message = "成本价不能为空")
    private BigDecimal cost;

    @Schema(description = "秒杀产品ID", required = true, example = "21525")
    //@NotNull(message = "秒杀产品ID不能为空")
    private Long seckillId;

    @Schema(description = "砍价id", example = "5132")
    private Integer bargainId;

    @Schema(description = "核销码", required = true)
    //@NotNull(message = "核销码不能为空")
    private String verifyCode;

    @Schema(description = "门店id", required = true, example = "12064")
    //@NotNull(message = "门店id不能为空")
    private Integer storeId;

    @Schema(description = "配送方式 1=快递 ，2=门店自提", required = true, example = "2")
    //@NotNull(message = "配送方式 1=快递 ，2=门店自提不能为空")
    private Integer shippingType;

    @Schema(description = "支付渠道(0微信公众号1微信小程序)")
    private Integer isChannel;

    @Schema(description = "系统删除")
    private Integer isSystemDel;

    @Schema(description = "订单类型")
    private String orderType;

    /**
     * 取餐👌
     */
    private Long numberId;

    /**
     * 门店ID
     */
    private Long shopId;

    /**
     * 门店名称
     */
    private String shopName;

    private LocalDateTime getTime;

    /**
     * 桌面ID
     */
    private Long deskId;


    /**
     * 桌号
     */
    private String deskNumber;

    /**
     * 就餐人数
     */
    private Integer deskPeople;

    /**
     * 桌台二维码编号
     */
    private Long deskQrId;

    /**
     * 桌台二维码名称
     */
    private String deskQrName;

}
