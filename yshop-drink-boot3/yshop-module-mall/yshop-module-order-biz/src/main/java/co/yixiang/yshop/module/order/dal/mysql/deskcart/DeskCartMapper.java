package co.yixiang.yshop.module.order.dal.mysql.deskcart;

import co.yixiang.yshop.framework.mybatis.core.mapper.BaseMapperX;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.module.order.dal.dataobject.deskcart.DeskCartDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 桌台共享购物车 Mapper
 *
 * @author yshop
 */
@Mapper
public interface DeskCartMapper extends BaseMapperX<DeskCartDO> {

    default List<DeskCartDO> selectListBySessionId(Long sessionId) {
        return selectList(new LambdaQueryWrapperX<DeskCartDO>()
                .eq(DeskCartDO::getSessionId, sessionId)
                .orderByAsc(DeskCartDO::getId));
    }

    default DeskCartDO selectBySessionProductSku(Long sessionId, Long productId, String sku) {
        return selectOne(new LambdaQueryWrapperX<DeskCartDO>()
                .eq(DeskCartDO::getSessionId, sessionId)
                .eq(DeskCartDO::getProductId, productId)
                .eq(DeskCartDO::getSku, sku));
    }

    default void deleteBySessionId(Long sessionId) {
        delete(new LambdaQueryWrapperX<DeskCartDO>()
                .eq(DeskCartDO::getSessionId, sessionId));
    }
}
