package co.yixiang.yshop.module.store.dal.mysql.storedesk;

import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.framework.mybatis.core.mapper.BaseMapperX;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.module.store.controller.admin.storedesk.vo.StoreDeskPageReqVO;
import co.yixiang.yshop.module.store.dal.dataobject.storedesk.StoreDeskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门店桌台 Mapper
 *
 * @author yshop
 */
@Mapper
public interface StoreDeskMapper extends BaseMapperX<StoreDeskDO> {

    default PageResult<StoreDeskDO> selectPage(StoreDeskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StoreDeskDO>()
                .eqIfPresent(StoreDeskDO::getShopId, reqVO.getShopId())
                .likeIfPresent(StoreDeskDO::getDeskNumber, reqVO.getDeskNumber())
                .likeIfPresent(StoreDeskDO::getArea, reqVO.getArea())
                .eqIfPresent(StoreDeskDO::getStatus, reqVO.getStatus())
                .orderByAsc(StoreDeskDO::getSort)
                .orderByAsc(StoreDeskDO::getId));
    }

    default StoreDeskDO selectByShopIdAndNumber(Long shopId, String deskNumber) {
        return selectOne(new LambdaQueryWrapperX<StoreDeskDO>()
                .eq(StoreDeskDO::getShopId, shopId)
                .eq(StoreDeskDO::getDeskNumber, deskNumber));
    }

    default StoreDeskDO selectByQrToken(String qrToken) {
        return selectOne(new LambdaQueryWrapperX<StoreDeskDO>()
                .eq(StoreDeskDO::getQrToken, qrToken));
    }

    default List<StoreDeskDO> selectListByShopId(Long shopId) {
        return selectList(new LambdaQueryWrapperX<StoreDeskDO>()
                .eq(StoreDeskDO::getShopId, shopId)
                .orderByAsc(StoreDeskDO::getSort)
                .orderByAsc(StoreDeskDO::getId));
    }
}
