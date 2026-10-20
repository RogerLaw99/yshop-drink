package co.yixiang.yshop.module.store.service.storedesk;

import cn.hutool.core.util.StrUtil;
import co.yixiang.yshop.framework.security.core.util.SecurityFrameworkUtils;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskQrCreateReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskQrUpdateReqVO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskQrDO;
import co.yixiang.yshop.module.store.dal.mysql.storedesk.StoreDeskMapper;
import co.yixiang.yshop.module.store.dal.mysql.storedesk.StoreDeskQrMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

import static co.yixiang.yshop.framework.common.exception.util.ServiceExceptionUtil.exception;
import static co.yixiang.yshop.module.store.enums.ErrorCodeConstants.*;

/**
 * 桌台二维码 Service 实现类
 *
 * @author yshop
 */
@Service
@Validated
public class StoreDeskQrServiceImpl implements StoreDeskQrService {

    @Resource
    private StoreDeskQrMapper qrMapper;
    @Resource
    private StoreDeskMapper deskMapper;

    @Override
    public StoreDeskQrDO getQr(Long id) {
        return id == null ? null : qrMapper.selectById(id);
    }

    @Override
    public StoreDeskQrDO getByToken(String qrToken) {
        if (StrUtil.isBlank(qrToken)) {
            return null;
        }
        return qrMapper.selectByToken(qrToken.trim());
    }

    @Override
    public List<StoreDeskQrDO> getListByDeskId(Long deskId) {
        StoreDeskDO desk = deskId == null ? null : deskMapper.selectById(deskId);
        Long loginShopId = getLoginShopId();
        if (desk == null || (loginShopId != 0 && !loginShopId.equals(desk.getShopId()))) {
            throw exception(STORE_DESK_NOT_EXISTS);
        }
        return qrMapper.selectListByDeskId(deskId);
    }

    @Override
    public Long createQr(StoreDeskQrCreateReqVO createReqVO) {
        StoreDeskDO desk = validateDesk(createReqVO.getDeskId());
        validateNameUnique(null, desk.getId(), createReqVO.getQrName());
        StoreDeskQrDO qr = StoreDeskQrDO.builder()
                .shopId(desk.getShopId())
                .deskId(desk.getId())
                .qrName(StrUtil.trim(createReqVO.getQrName()))
                .qrToken(newToken())
                .status(createReqVO.getStatus() == null ? 1 : createReqVO.getStatus())
                .sort(createReqVO.getSort() == null ? 0 : createReqVO.getSort())
                .build();
        qrMapper.insert(qr);
        return qr.getId();
    }

    @Override
    public void updateQr(StoreDeskQrUpdateReqVO updateReqVO) {
        StoreDeskQrDO old = validateQr(updateReqVO.getId());
        StoreDeskDO desk = validateDesk(updateReqVO.getDeskId());
        validateNameUnique(updateReqVO.getId(), desk.getId(), updateReqVO.getQrName());
        StoreDeskQrDO update = StoreDeskQrDO.builder()
                .id(old.getId())
                .shopId(desk.getShopId())
                .deskId(desk.getId())
                .qrName(StrUtil.trim(updateReqVO.getQrName()))
                .status(updateReqVO.getStatus())
                .sort(updateReqVO.getSort())
                .build();
        qrMapper.updateById(update);
    }

    @Override
    public void deleteQr(Long id) {
        validateQr(id);
        qrMapper.deleteById(id);
    }

    @Override
    public String resetToken(Long id) {
        validateQr(id);
        String token = newToken();
        qrMapper.updateById(StoreDeskQrDO.builder().id(id).qrToken(token).build());
        return token;
    }

    @Override
    public void deleteByDeskId(Long deskId) {
        qrMapper.delete(new co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX<StoreDeskQrDO>()
                .eq(StoreDeskQrDO::getDeskId, deskId));
    }

    @Override
    public void createDefaultQr(StoreDeskDO desk) {
        StoreDeskQrDO qr = StoreDeskQrDO.builder()
                .shopId(desk.getShopId())
                .deskId(desk.getId())
                .qrName(desk.getDeskNumber())
                .qrToken(StrUtil.isNotBlank(desk.getQrToken()) ? desk.getQrToken() : newToken())
                .status(1)
                .sort(0)
                .build();
        qrMapper.insert(qr);
    }

    private StoreDeskQrDO validateQr(Long id) {
        StoreDeskQrDO qr = getQr(id);
        if (qr == null) {
            throw exception(STORE_DESK_QR_NOT_EXISTS);
        }
        Long loginShopId = getLoginShopId();
        if (loginShopId != 0 && !loginShopId.equals(qr.getShopId())) {
            throw exception(STORE_DESK_QR_NOT_EXISTS);
        }
        return qr;
    }

    private StoreDeskDO validateDesk(Long deskId) {
        StoreDeskDO desk = deskId == null ? null : deskMapper.selectById(deskId);
        if (desk == null) {
            throw exception(STORE_DESK_NOT_EXISTS);
        }
        Long loginShopId = getLoginShopId();
        if (loginShopId != 0 && !loginShopId.equals(desk.getShopId())) {
            throw exception(STORE_DESK_NOT_EXISTS);
        }
        return desk;
    }

    private void validateNameUnique(Long id, Long deskId, String qrName) {
        StoreDeskQrDO exists = qrMapper.selectByDeskIdAndName(deskId, StrUtil.trim(qrName));
        if (exists != null && !exists.getId().equals(id)) {
            throw exception(STORE_DESK_QR_NAME_EXISTS);
        }
    }

    private Long getLoginShopId() {
        if (SecurityFrameworkUtils.getLoginUser() == null
                || SecurityFrameworkUtils.getLoginUser().getShopId() == null) {
            return 0L;
        }
        return SecurityFrameworkUtils.getLoginUser().getShopId();
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
