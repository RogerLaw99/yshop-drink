package co.yixiang.yshop.module.store.service.storedesk;

import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskQrCreateReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskQrUpdateReqVO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskQrDO;

import java.util.List;

/**
 * 桌台二维码 Service 接口
 *
 * @author yshop
 */
public interface StoreDeskQrService {

    StoreDeskQrDO getQr(Long id);

    StoreDeskQrDO getByToken(String qrToken);

    List<StoreDeskQrDO> getListByDeskId(Long deskId);

    Long createQr(StoreDeskQrCreateReqVO createReqVO);

    void updateQr(StoreDeskQrUpdateReqVO updateReqVO);

    void deleteQr(Long id);

    String resetToken(Long id);

    /**
     * 为新建桌台创建默认二维码
     */
    void createDefaultQr(StoreDeskDO desk);

    /**
     * 删除桌台下的全部二维码（随桌台删除）
     */
    void deleteByDeskId(Long deskId);
}
