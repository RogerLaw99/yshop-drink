package co.yixiang.yshop.module.store.service.storedesk;

import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskBatchCreateReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskCreateReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskPageReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskUpdateReqVO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;

import java.util.List;

/**
 * 门店桌台 Service 接口
 *
 * @author yshop
 */
public interface StoreDeskService {

    Long createDesk(StoreDeskCreateReqVO createReqVO);

    void updateDesk(StoreDeskUpdateReqVO updateReqVO);

    void deleteDesk(Long id);

    StoreDeskDO getDesk(Long id);

    /**
     * 按编号获取桌台（不做登录门店校验，供用户端扫码使用）
     */
    StoreDeskDO getDeskById(Long id);

    StoreDeskDO getByShopIdAndNumber(Long shopId, String deskNumber);

    StoreDeskDO getByQrToken(String qrToken);

    PageResult<StoreDeskDO> getDeskPage(StoreDeskPageReqVO pageReqVO);

    List<StoreDeskDO> getDeskListByShopId(Long shopId);

    List<Long> batchCreate(StoreDeskBatchCreateReqVO reqVO);

    String resetQrToken(Long id);
}
