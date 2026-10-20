package co.yixiang.yshop.module.order.service.desksession;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import co.yixiang.yshop.framework.common.exception.ServiceException;
import co.yixiang.yshop.module.member.dal.dataobject.user.MemberUserDO;
import co.yixiang.yshop.module.member.service.user.MemberUserService;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCartAddParam;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCartUpdateParam;
import co.yixiang.yshop.module.order.controller.app.order.param.AppDeskCheckoutParam;
import co.yixiang.yshop.module.order.controller.app.order.param.AppOrderParam;
import co.yixiang.yshop.module.order.controller.app.order.vo.AppDeskCartItemVO;
import co.yixiang.yshop.module.order.controller.app.order.vo.AppDeskSessionRespVO;
import co.yixiang.yshop.module.order.dal.dataobject.deskcart.DeskCartDO;
import co.yixiang.yshop.module.order.dal.dataobject.desksession.DeskSessionDO;
import co.yixiang.yshop.module.order.dal.mysql.deskcart.DeskCartMapper;
import co.yixiang.yshop.module.order.dal.mysql.desksession.DeskSessionMapper;
import co.yixiang.yshop.module.order.service.storeorder.AppStoreOrderService;
import co.yixiang.yshop.module.product.dal.dataobject.storeproduct.StoreProductDO;
import co.yixiang.yshop.module.product.dal.dataobject.storeproductattrvalue.StoreProductAttrValueDO;
import co.yixiang.yshop.module.product.dal.mysql.storeproductattrvalue.StoreProductAttrValueMapper;
import co.yixiang.yshop.module.product.service.storeproduct.StoreProductService;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskQrDO;
import co.yixiang.yshop.module.store.service.storedesk.StoreDeskQrService;
import co.yixiang.yshop.module.store.service.storedesk.StoreDeskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static co.yixiang.yshop.framework.common.exception.util.ServiceExceptionUtil.exception;
import static co.yixiang.yshop.module.order.enums.ErrorCodeConstants.*;

/**
 * 桌台共享会话 Service 实现类
 *
 * 规则：
 * - 同一桌台同时只允许一个“进行中”会话；
 * - 多人扫码自动加入同一会话，共享购物车；
 * - 任意一人可修改购物车；
 * - 结算生成一笔订单，支付成功后会话结束（这里在生成订单时关闭会话）；
 * - 超过 4 小时未结算的会话自动视为过期。
 *
 * @author yshop
 */
@Slf4j
@Service
@Validated
public class DeskSessionServiceImpl implements DeskSessionService {

    /** 会话最长存活时间（小时） */
    private static final int SESSION_MAX_HOURS = 4;

    @Resource
    private DeskSessionMapper sessionMapper;
    @Resource
    private DeskCartMapper cartMapper;
    @Resource
    private StoreDeskService storeDeskService;
    @Resource
    private StoreDeskQrService deskQrService;
    @Resource
    private StoreProductService storeProductService;
    @Resource
    private StoreProductAttrValueMapper attrValueMapper;
    @Resource
    private MemberUserService memberUserService;
    @Resource
    private AppStoreOrderService appStoreOrderService;
    @Resource
    private RedissonClient redissonClient;

    @Override
    public AppDeskSessionRespVO joinSession(String token, Long uid) {
        StoreDeskQrDO qr = deskQrService.getByToken(token);
        if (qr == null || qr.getStatus() == null || qr.getStatus() != 1) {
            throw exception(new co.yixiang.yshop.framework.common.exception.ErrorCode(
                    1008016005, "二维码不存在或已停用"));
        }
        StoreDeskDO desk = storeDeskService.getDeskById(qr.getDeskId());
        if (desk == null || desk.getStatus() == null || desk.getStatus() != 1) {
            throw exception(new co.yixiang.yshop.framework.common.exception.ErrorCode(
                    1008016005, "桌台不存在或已停用"));
        }
        RLock lock = redissonClient.getLock("desk:qr:session:" + qr.getId());
        lock.lock();
        try {
            DeskSessionDO session = sessionMapper.selectOpenByQrId(qr.getId());
            if (session != null && isExpired(session)) {
                session.setStatus(2);
                session.setCloseTime(LocalDateTime.now());
                sessionMapper.updateById(session);
                session = null;
            }
            if (session == null) {
                session = DeskSessionDO.builder()
                        .shopId(desk.getShopId())
                        .deskId(desk.getId())
                        .deskNumber(desk.getDeskNumber())
                        .qrId(qr.getId())
                        .qrName(qr.getQrName())
                        .status(0)
                        .build();
                sessionMapper.insert(session);
            }
            return buildResp(session);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public AppDeskSessionRespVO getSession(Long sessionId) {
        DeskSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw exception(DESK_SESSION_NOT_OPEN);
        }
        return buildResp(session);
    }

    @Override
    public AppDeskSessionRespVO addCartItem(Long uid, AppDeskCartAddParam param) {
        DeskSessionDO session = validateOpenSession(param.getSessionId());
        StoreProductDO product = storeProductService.getStoreProduct(param.getProductId());
        if (product == null) {
            throw exception(new co.yixiang.yshop.framework.common.exception.ErrorCode(
                    1008007005, "商品不存在或已下架"));
        }
        String sku = StrUtil.trim(param.getSku());
        StoreProductAttrValueDO attrValue = attrValueMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StoreProductAttrValueDO>()
                        .eq(StoreProductAttrValueDO::getProductId, param.getProductId())
                        .eq(StoreProductAttrValueDO::getSku, sku)
                        .last("LIMIT 1"));
        if (attrValue == null) {
            throw exception(new co.yixiang.yshop.framework.common.exception.ErrorCode(
                    1008007006, "规格不存在，请重新选择"));
        }

        DeskCartDO exists = cartMapper.selectBySessionProductSku(session.getId(), param.getProductId(), sku);
        if (exists != null) {
            exists.setNumber(exists.getNumber() + param.getNumber());
            cartMapper.updateById(exists);
        } else {
            MemberUserDO user = memberUserService.getUser(uid);
            String nickname = user != null && StrUtil.isNotBlank(user.getNickname())
                    ? user.getNickname() : ("用户" + uid);
            DeskCartDO cart = DeskCartDO.builder()
                    .sessionId(session.getId())
                    .shopId(session.getShopId())
                    .uid(uid)
                    .nickname(nickname)
                    .productId(param.getProductId())
                    .sku(sku)
                    .specText(sku.replace(",", " / "))
                    .name(product.getStoreName())
                    .image(StrUtil.isNotBlank(attrValue.getImage()) ? attrValue.getImage() : product.getImage())
                    .price(attrValue.getPrice() == null ? BigDecimal.ZERO : attrValue.getPrice())
                    .number(param.getNumber())
                    .build();
            cartMapper.insert(cart);
        }
        return buildResp(session);
    }

    @Override
    public AppDeskSessionRespVO updateCartItem(Long uid, AppDeskCartUpdateParam param) {
        DeskCartDO cart = cartMapper.selectById(param.getId());
        if (cart == null) {
            throw exception(DESK_CART_ITEM_NOT_EXISTS);
        }
        DeskSessionDO session = validateOpenSession(cart.getSessionId());
        if (param.getNumber() == null || param.getNumber() <= 0) {
            cartMapper.deleteById(cart.getId());
        } else {
            cart.setNumber(param.getNumber());
            cartMapper.updateById(cart);
        }
        return buildResp(session);
    }

    @Override
    public AppDeskSessionRespVO deleteCartItem(Long uid, Long id) {
        DeskCartDO cart = cartMapper.selectById(id);
        if (cart == null) {
            throw exception(DESK_CART_ITEM_NOT_EXISTS);
        }
        DeskSessionDO session = validateOpenSession(cart.getSessionId());
        cartMapper.deleteById(id);
        return buildResp(session);
    }

    @Override
    public void clearCart(Long uid, Long sessionId) {
        validateOpenSession(sessionId);
        cartMapper.deleteBySessionId(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkout(Long uid, AppDeskCheckoutParam param) {
        DeskSessionDO session = validateOpenSession(param.getSessionId());
        List<DeskCartDO> items = cartMapper.selectListBySessionId(session.getId());
        if (items.isEmpty()) {
            throw exception(DESK_CART_EMPTY);
        }

        List<String> productIds = new ArrayList<>();
        List<String> specs = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        Set<Long> participants = new HashSet<>();
        for (DeskCartDO item : items) {
            productIds.add(String.valueOf(item.getProductId()));
            specs.add(item.getSku().replace(",", "|"));
            numbers.add(String.valueOf(item.getNumber()));
            participants.add(item.getUid());
        }

        MemberUserDO user = memberUserService.getUser(uid);
        AppOrderParam orderParam = new AppOrderParam();
        orderParam.setOrderType("desk");
        orderParam.setShopId(String.valueOf(session.getShopId()));
        orderParam.setDeskNumber(session.getDeskNumber());
        orderParam.setDeskQrId(session.getQrId());
        orderParam.setDeskQrName(session.getQrName());
        orderParam.setDeskPeople(param.getDeskPeople() == null || param.getDeskPeople() < 1
                ? Math.max(1, participants.size()) : param.getDeskPeople());
        orderParam.setMobile(StrUtil.isNotBlank(param.getMobile()) ? param.getMobile()
                : (user == null ? "" : user.getMobile()));
        orderParam.setPayType(StrUtil.blankToDefault(param.getPayType(), "yue"));
        orderParam.setRemark(StrUtil.blankToDefault(param.getRemark(), ""));
        orderParam.setCouponId(StrUtil.blankToDefault(param.getCouponId(), "0"));
        orderParam.setGettime(0);
        orderParam.setProductId(productIds);
        orderParam.setSpec(specs);
        orderParam.setNumber(numbers);

        Map<String, Object> result = appStoreOrderService.createOrder(uid, orderParam);
        String orderId = String.valueOf(result.get("orderId"));

        session.setStatus(1);
        session.setPayerUid(uid);
        session.setOrderId(orderId);
        session.setCloseTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        cartMapper.deleteBySessionId(session.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("orderId", orderId);
        return response;
    }

    private DeskSessionDO validateOpenSession(Long sessionId) {
        DeskSessionDO session = sessionId == null ? null : sessionMapper.selectById(sessionId);
        if (session == null) {
            throw exception(DESK_SESSION_NOT_OPEN);
        }
        if (session.getStatus() == null || session.getStatus() != 0) {
            throw exception(DESK_SESSION_NOT_OPEN);
        }
        if (isExpired(session)) {
            session.setStatus(2);
            session.setCloseTime(LocalDateTime.now());
            sessionMapper.updateById(session);
            throw exception(DESK_SESSION_NOT_OPEN);
        }
        return session;
    }

    private boolean isExpired(DeskSessionDO session) {
        return session.getCreateTime() != null
                && session.getCreateTime().isBefore(LocalDateTime.now().minusHours(SESSION_MAX_HOURS));
    }

    private AppDeskSessionRespVO buildResp(DeskSessionDO session) {
        List<DeskCartDO> items = cartMapper.selectListBySessionId(session.getId());
        AppDeskSessionRespVO resp = new AppDeskSessionRespVO();
        resp.setSessionId(session.getId());
        resp.setDeskId(session.getDeskId());
        resp.setShopId(session.getShopId());
        resp.setDeskNumber(session.getDeskNumber());
        resp.setQrId(session.getQrId());
        resp.setQrName(session.getQrName());
        resp.setStatus(session.getStatus());

        List<AppDeskCartItemVO> cartList = items.stream().map(item -> {
            AppDeskCartItemVO vo = new AppDeskCartItemVO();
            BeanUtil.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());
        resp.setCartList(cartList);
        resp.setTotalNumber(items.stream().mapToInt(item -> item.getNumber() == null ? 0 : item.getNumber()).sum());
        BigDecimal total = items.stream()
                .map(item -> (item.getPrice() == null ? BigDecimal.ZERO : item.getPrice())
                        .multiply(BigDecimal.valueOf(item.getNumber() == null ? 0 : item.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalAmount(total);
        resp.setParticipantCount((int) items.stream().map(DeskCartDO::getUid).distinct().count());
        return resp;
    }
}
