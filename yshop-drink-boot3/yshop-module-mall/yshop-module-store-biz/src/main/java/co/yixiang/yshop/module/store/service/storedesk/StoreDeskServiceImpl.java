package co.yixiang.yshop.module.store.service.storedesk;

import cn.hutool.core.util.StrUtil;
import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.framework.security.core.util.SecurityFrameworkUtils;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskBatchCreateReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskCreateReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskPageReqVO;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskUpdateReqVO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;
import co.yixiang.yshop.module.store.dal.mysql.storedesk.StoreDeskMapper;
import co.yixiang.yshop.module.store.dal.mysql.storeshop.StoreShopMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static co.yixiang.yshop.framework.common.exception.util.ServiceExceptionUtil.exception;
import static co.yixiang.yshop.module.store.enums.ErrorCodeConstants.*;

/**
 * 门店桌台 Service 实现类
 *
 * @author yshop
 */
@Service
@Validated
public class StoreDeskServiceImpl implements StoreDeskService {

    @Resource
    private StoreDeskMapper deskMapper;
    @Resource
    private StoreShopMapper shopMapper;
    @Resource
    private StoreDeskQrService deskQrService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDesk(StoreDeskCreateReqVO createReqVO) {
        applyLoginShop(createReqVO);
        validateShopExists(createReqVO.getShopId());
        validateDeskNumberUnique(null, createReqVO.getShopId(), createReqVO.getDeskNumber());
        StoreDeskDO desk = StoreDeskDO.builder()
                .shopId(createReqVO.getShopId())
                .deskNumber(StrUtil.trim(createReqVO.getDeskNumber()))
                .area(createReqVO.getArea())
                .capacity(createReqVO.getCapacity() == null ? 4 : createReqVO.getCapacity())
                .status(createReqVO.getStatus() == null ? 1 : createReqVO.getStatus())
                .sort(createReqVO.getSort() == null ? 0 : createReqVO.getSort())
                .qrToken(newToken())
                .build();
        deskMapper.insert(desk);
        // 新建桌台自动创建一个默认二维码（名称 = 桌号）
        deskQrService.createDefaultQr(desk);
        return desk.getId();
    }

    @Override
    public void updateDesk(StoreDeskUpdateReqVO updateReqVO) {
        validateDeskOwnership(updateReqVO.getId());
        Long loginShopId = getLoginShopId();
        if (loginShopId != 0) {
            updateReqVO.setShopId(loginShopId);
        }
        validateShopExists(updateReqVO.getShopId());
        validateDeskNumberUnique(updateReqVO.getId(), updateReqVO.getShopId(), updateReqVO.getDeskNumber());
        StoreDeskDO desk = StoreDeskDO.builder()
                .id(updateReqVO.getId())
                .shopId(updateReqVO.getShopId())
                .deskNumber(StrUtil.trim(updateReqVO.getDeskNumber()))
                .area(updateReqVO.getArea())
                .capacity(updateReqVO.getCapacity())
                .status(updateReqVO.getStatus())
                .sort(updateReqVO.getSort())
                .build();
        deskMapper.updateById(desk);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDesk(Long id) {
        validateDeskOwnership(id);
        deskQrService.deleteByDeskId(id);
        deskMapper.deleteById(id);
    }

    @Override
    public StoreDeskDO getDesk(Long id) {
        validateDeskOwnership(id);
        return deskMapper.selectById(id);
    }

    @Override
    public StoreDeskDO getDeskById(Long id) {
        return id == null ? null : deskMapper.selectById(id);
    }

    @Override
    public StoreDeskDO getByShopIdAndNumber(Long shopId, String deskNumber) {
        if (shopId == null || StrUtil.isBlank(deskNumber)) {
            return null;
        }
        return deskMapper.selectByShopIdAndNumber(shopId, StrUtil.trim(deskNumber));
    }

    @Override
    public StoreDeskDO getByQrToken(String qrToken) {
        if (StrUtil.isBlank(qrToken)) {
            return null;
        }
        return deskMapper.selectByQrToken(qrToken.trim());
    }

    @Override
    public PageResult<StoreDeskDO> getDeskPage(StoreDeskPageReqVO pageReqVO) {
        Long loginShopId = getLoginShopId();
        if (loginShopId != 0) {
            pageReqVO.setShopId(loginShopId);
        }
        return deskMapper.selectPage(pageReqVO);
    }

    @Override
    public List<StoreDeskDO> getDeskListByShopId(Long shopId) {
        Long loginShopId = getLoginShopId();
        return deskMapper.selectListByShopId(loginShopId != 0 ? loginShopId : shopId);
    }

    @Override
    public List<Long> batchCreate(StoreDeskBatchCreateReqVO reqVO) {
        Long loginShopId = getLoginShopId();
        if (loginShopId != 0) {
            reqVO.setShopId(loginShopId);
        }
        validateShopExists(reqVO.getShopId());
        if (reqVO.getStartNumber() == null || reqVO.getEndNumber() == null
                || reqVO.getEndNumber() < reqVO.getStartNumber()) {
            throw exception(STORE_DESK_BATCH_PARAM_ERROR);
        }
        int width = Math.max(2, String.valueOf(reqVO.getEndNumber()).length());
        List<Long> ids = new ArrayList<>();
        for (int i = reqVO.getStartNumber(); i <= reqVO.getEndNumber(); i++) {
            String deskNumber = reqVO.getPrefix() + String.format("%0" + width + "d", i);
            if (deskMapper.selectByShopIdAndNumber(reqVO.getShopId(), deskNumber) != null) {
                continue;
            }
            StoreDeskDO desk = StoreDeskDO.builder()
                    .shopId(reqVO.getShopId())
                    .deskNumber(deskNumber)
                    .area(reqVO.getArea())
                    .capacity(reqVO.getCapacity() == null ? 4 : reqVO.getCapacity())
                    .status(1)
                    .sort(i)
                    .qrToken(newToken())
                    .build();
            deskMapper.insert(desk);
            deskQrService.createDefaultQr(desk);
            ids.add(desk.getId());
        }
        return ids;
    }

    @Override
    public String resetQrToken(Long id) {
        validateDeskOwnership(id);
        String token = newToken();
        StoreDeskDO desk = StoreDeskDO.builder().id(id).qrToken(token).build();
        deskMapper.updateById(desk);
        return token;
    }

    private Long getLoginShopId() {
        if (SecurityFrameworkUtils.getLoginUser() == null
                || SecurityFrameworkUtils.getLoginUser().getShopId() == null) {
            return 0L;
        }
        return SecurityFrameworkUtils.getLoginUser().getShopId();
    }

    private void applyLoginShop(StoreDeskCreateReqVO reqVO) {
        Long loginShopId = getLoginShopId();
        if (loginShopId != 0) {
            reqVO.setShopId(loginShopId);
        }
    }

    private void validateDeskOwnership(Long id) {
        StoreDeskDO desk = validateDeskExists(id);
        Long loginShopId = getLoginShopId();
        if (loginShopId != 0 && !loginShopId.equals(desk.getShopId())) {
            throw exception(STORE_DESK_NOT_EXISTS);
        }
    }

    private void validateShopExists(Long shopId) {
        if (shopId == null || shopMapper.selectById(shopId) == null) {
            throw exception(SHOP_NOT_EXISTS);
        }
    }

    private StoreDeskDO validateDeskExists(Long id) {
        StoreDeskDO desk = id == null ? null : deskMapper.selectById(id);
        if (desk == null) {
            throw exception(STORE_DESK_NOT_EXISTS);
        }
        return desk;
    }

    private void validateDeskNumberUnique(Long id, Long shopId, String deskNumber) {
        StoreDeskDO exists = deskMapper.selectByShopIdAndNumber(shopId, StrUtil.trim(deskNumber));
        if (exists != null && !exists.getId().equals(id)) {
            throw exception(STORE_DESK_NUMBER_EXISTS);
        }
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
