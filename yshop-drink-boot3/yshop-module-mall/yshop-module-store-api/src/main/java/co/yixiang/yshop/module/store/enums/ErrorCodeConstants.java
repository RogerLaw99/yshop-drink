package co.yixiang.yshop.module.store.enums;

import co.yixiang.yshop.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {
    ErrorCode SHOP_NOT_EXISTS = new ErrorCode(1008016000, "门店管理不存在");
    ErrorCode WEB_PRINT_NOT_EXISTS = new ErrorCode(1008016001, "易联云打印机不存在");
    ErrorCode STORE_REVENUE_NOT_EXISTS = new ErrorCode(1008016002, "店铺收支明细不存在");
    ErrorCode WITHDRAWAL_NOT_EXISTS = new ErrorCode(1008016003, "提现管理不存在");
    ErrorCode USER_BANK_NOT_EXISTS = new ErrorCode(1008016004, "提现账户不存在");
    ErrorCode STORE_DESK_NOT_EXISTS = new ErrorCode(1008016005, "桌台不存在或已停用");
    ErrorCode STORE_DESK_NUMBER_EXISTS = new ErrorCode(1008016006, "该桌号已存在");
    ErrorCode STORE_DESK_BATCH_PARAM_ERROR = new ErrorCode(1008016007, "批量创建参数不正确");
    ErrorCode STORE_DESK_QR_NOT_EXISTS = new ErrorCode(1008016008, "桌台二维码不存在或已停用");
    ErrorCode STORE_DESK_QR_NAME_EXISTS = new ErrorCode(1008016009, "该桌台下的二维码名称已存在");
}
