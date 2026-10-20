package co.yixiang.yshop.module.store.dal.mysql.storedesk;

import co.yixiang.yshop.framework.mybatis.core.mapper.BaseMapperX;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskQrDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 桌台二维码 Mapper
 *
 * @author yshop
 */
@Mapper
public interface StoreDeskQrMapper extends BaseMapperX<StoreDeskQrDO> {

    default StoreDeskQrDO selectByToken(String qrToken) {
        return selectOne(new LambdaQueryWrapperX<StoreDeskQrDO>()
                .eq(StoreDeskQrDO::getQrToken, qrToken));
    }

    default List<StoreDeskQrDO> selectListByDeskId(Long deskId) {
        return selectList(new LambdaQueryWrapperX<StoreDeskQrDO>()
                .eq(StoreDeskQrDO::getDeskId, deskId)
                .orderByAsc(StoreDeskQrDO::getSort)
                .orderByAsc(StoreDeskQrDO::getId));
    }

    default StoreDeskQrDO selectByDeskIdAndName(Long deskId, String qrName) {
        return selectOne(new LambdaQueryWrapperX<StoreDeskQrDO>()
                .eq(StoreDeskQrDO::getDeskId, deskId)
                .eq(StoreDeskQrDO::getQrName, qrName));
    }
}
