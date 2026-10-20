package co.yixiang.yshop.module.order.dal.mysql.desksession;

import co.yixiang.yshop.framework.mybatis.core.mapper.BaseMapperX;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.module.order.dal.dataobject.desksession.DeskSessionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 桌台会话 Mapper
 *
 * @author yshop
 */
@Mapper
public interface DeskSessionMapper extends BaseMapperX<DeskSessionDO> {

    default DeskSessionDO selectOpenByQrId(Long qrId) {
        return selectOne(new LambdaQueryWrapperX<DeskSessionDO>()
                .eq(DeskSessionDO::getQrId, qrId)
                .eq(DeskSessionDO::getStatus, 0)
                .orderByDesc(DeskSessionDO::getId)
                .last("LIMIT 1"));
    }

    default DeskSessionDO selectOpenByDeskId(Long deskId) {
        return selectOne(new LambdaQueryWrapperX<DeskSessionDO>()
                .eq(DeskSessionDO::getDeskId, deskId)
                .eq(DeskSessionDO::getStatus, 0)
                .orderByDesc(DeskSessionDO::getId)
                .last("LIMIT 1"));
    }
}
