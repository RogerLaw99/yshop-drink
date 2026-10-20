package co.yixiang.yshop.module.store.service.storeshop;

import co.yixiang.yshop.module.store.controller.app.storeshop.vo.AppStoreShopVO;
import co.yixiang.yshop.module.store.dal.dataobject.storeshop.StoreShopDO;
import co.yixiang.yshop.module.store.dal.mysql.storeshop.StoreShopMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 门店管理 Service 实现类
 *
 * @author yshop
 */
@Service
@Validated
public class AppStoreShopServiceImpl extends ServiceImpl<StoreShopMapper, StoreShopDO> implements AppStoreShopService {

    @Resource
    private StoreShopMapper shopMapper;


    /**
     * 获取门店
     * @param lon 经度
     * @param lat 纬度
     * @param name 店铺名称
     * @return
     */
    @Override
    public List<AppStoreShopVO> getStoreList(double lon, double lat,String name,Integer shopId) {
        return shopMapper.getStoreList(lon,lat,name,shopId);
    }

    @Override
    public boolean isOpen(StoreShopDO shop) {
        if (shop == null || shop.getStartTime() == null || shop.getEndTime() == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        LocalTime start = shop.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        LocalTime end = shop.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        // 跨天营业，如 22:00 - 02:00
        return !now.isBefore(start) || !now.isAfter(end);
    }
}
