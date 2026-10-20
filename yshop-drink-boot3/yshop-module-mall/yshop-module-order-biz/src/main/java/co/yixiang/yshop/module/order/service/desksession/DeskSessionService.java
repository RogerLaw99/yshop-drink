package co.yixiang.yshop.module.order.service.desksession;

import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCartAddParam;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCartUpdateParam;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCheckoutParam;
import co.yixiang.yshop.module.order.controller.app.order.vo.AppDeskSessionRespVO;

import java.util.Map;

/**
 * 桌台共享会话 Service 接口
 *
 * @author yshop
 */
public interface DeskSessionService {

    /**
     * 扫码加入/创建桌台会话
     */
    AppDeskSessionRespVO joinSession(String token, Long uid);

    /**
     * 获取会话详情
     */
    AppDeskSessionRespVO getSession(Long sessionId);

    /**
     * 添加商品到共享购物车
     */
    AppDeskSessionRespVO addCartItem(Long uid, AppDeskCartAddParam param);

    /**
     * 修改共享购物车商品数量（0 表示删除）
     */
    AppDeskSessionRespVO updateCartItem(Long uid, AppDeskCartUpdateParam param);

    /**
     * 删除共享购物车商品
     */
    AppDeskSessionRespVO deleteCartItem(Long uid, Long id);

    /**
     * 清空共享购物车
     */
    void clearCart(Long uid, Long sessionId);

    /**
     * 共享购物车结算，生成一笔订单
     */
    Map<String, Object> checkout(Long uid, AppDeskCheckoutParam param);
}
