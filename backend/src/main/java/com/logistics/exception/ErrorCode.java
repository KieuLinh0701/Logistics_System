package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // System
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
  INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "Invalid enum value"),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
  MISSING_REQUIRED_FIELDS(HttpStatus.BAD_REQUEST, "Thiếu thông tin bắt buộc: %s"),
  EXPORT_EXCEL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra trong quá trình xuất file Excel"),

  // Cloudinary
  CLOUDINARY_FOLDER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary folder not found for key: %s"),
  CLOUDINARY_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary upload failed"),
  CLOUDINARY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary delete failed"),

  // Address
  MAX_ADDRESS_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Chỉ được tạo tối đa 10 địa chỉ"),
  ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"),
  ADDRESS_IS_DEFAULT(HttpStatus.BAD_REQUEST, "Vui lòng chọn địa chỉ mặc định khác trước khi xóa"),

  // Account
  ACCOUNT_LOCKED_DUE_TO_OVERDUE(HttpStatus.BAD_REQUEST, "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới."),

  // Bank Account
  BANK_ACCOUNT_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Chỉ được tạo tối đa 5 tài khoản ngân hàng"),
  BANK_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản ngân hàng"),
  BANK_ACCOUNT_IS_DEFAULT(HttpStatus.BAD_REQUEST, "Vui lòng chọn tài khoản mặc định khác trước khi xóa"),
  BANK_ACCOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "Bạn cần thêm tài khoản ngân hàng trong hồ sơ cá nhân để nhận tiền COD hoặc thanh toán khi tạo đơn hàng."),

  // Employee
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"),
  EMPLOYEE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên nhân viên này"),
  EMPLOYEE_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin phân quyền"),
  EMPLOYEE_ALREADY_IN_ANOTHER_SHOP(HttpStatus.BAD_REQUEST, "Tài khoản này đã thuộc một cửa hàng khác"),
  EMPLOYEE_PHONE_NUMBER_EXISTED(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng"),
  EMPLOYEE_HAS_ACTIVE_ROLE(HttpStatus.BAD_REQUEST, "Nhân viên này đang có quyền khác, vui lòng thu hồi trước khi gán quyền mới"),

  // Order
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"),
  ORDER_SENDER_ADDRESS_NOT_BELONG(HttpStatus.BAD_REQUEST, "Địa chỉ người gửi không thuộc người dùng"),
  ORDER_SENDER_ADDRESS_INVALID(HttpStatus.NOT_FOUND, "Địa chỉ người gửi không hợp lệ"),
  ORDER_RECIPIENT_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Địa chỉ người nhận không tồn tại"),
  ORDER_OFFICE_CITY_MISMATCH(HttpStatus.BAD_REQUEST, "Địa chỉ gửi và bưu cục phải thuộc cùng thành phố"),
  ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "Đơn hàng đã chuyển sang xử lý, không thể hủy"),
  ORDER_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "Đơn hàng đã chuyển sang xử lý, không thể xóa"),
  ORDER_SHIPPING_FEE_CHANGED(HttpStatus.BAD_REQUEST, "Phí vận chuyển vừa được cập nhật. Vui lòng kiểm tra lại trước khi tạo đơn."),
  ORDER_PRODUCT_INFO_CHANGED(HttpStatus.BAD_REQUEST, "Thông tin của sản phẩm đã thay đổi. Vui lòng kiểm tra lại các sản phẩm đã chọn trước khi tạo đơn."),
  ORDER_PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "Sản phẩm '%s' đã ngưng bán, không thể tạo đơn"),
  ORDER_PICKUP_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng của bạn không hợp lệ để chuyển"),
  ORDERS_NOT_FOUND_TO_PRINT(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng nào để in"),
  ORDER_CANNOT_EDIT(HttpStatus.BAD_REQUEST, "Đơn hàng đã hoàn thành không thể chỉnh sửa"),
  ORDER_FIELD_UPDATE_DENIED(HttpStatus.BAD_REQUEST, "Bạn không thể thay đổi '%s' khi đơn hàng đang ở trạng thái %s"),
  ORDER_INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Không thể chuyển trạng thái đơn hàng từ '%s' sang '%s'."),
  ORDER_SENDER_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ người gửi"),
  ORDER_INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái đơn hàng không hợp lệ"),
  ORDER_INVALID_PICKUP_TYPE(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng không hợp lệ"),
  ORDER_INVALID_PAYER(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng không hợp lệ"),
  ORDER_RECIPIENT_ADDRESS_INVALID(HttpStatus.NOT_FOUND, "Địa chỉ người nhận không hợp lệ"),
  ORDER_INVALID_RECIPIENT_PHONE(HttpStatus.BAD_REQUEST, "Số điện thoại người nhận phải đúng 10 chữ số"),
  ORDER_INVALID_RECIPIENT_CITY_CODE(HttpStatus.BAD_REQUEST, "Mã Thành phố không hợp lệ"),
  ORDER_INVALID_RECIPIENT_WARD_CODE(HttpStatus.BAD_REQUEST, "Mã Phường/Xã không hợp lệ"),
  ORDER_INVALID_RECIPIENT_COORDINATES(HttpStatus.BAD_REQUEST, "Tọa độ không hợp lệ"),
  ORDER_INVALID_WEIGHT(HttpStatus.BAD_REQUEST, "Khối lượng phải lớn hơn 0"),
  ORDER_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "Chiều dài phải lớn hơn 0"),
  ORDER_INVALID_WIDTH(HttpStatus.BAD_REQUEST, "Chiều rộng phải lớn hơn 0"),
  ORDER_INVALID_HEIGHT(HttpStatus.BAD_REQUEST, "Chiều cao phải lớn hơn 0"),
  ORDER_INVALID_COD_VALUE(HttpStatus.BAD_REQUEST, "Phí thu hộ không hợp lệ"),
  ORDER_INVALID_ORDER_VALUE(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng phải lớn hơn hoặc bằng 0"),
  ORDER_INVALID_PROMOTION_ID(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không hợp lệ"),
  ORDER_FROM_OFFICE_REQUIRED(HttpStatus.BAD_REQUEST, "Bưu cục nhận hàng không được để trống"),
  ORDER_NOTE_TOO_LONG(HttpStatus.BAD_REQUEST, "Ghi chú tối đa 1000 ký tự"),
  ORDER_INVALID_PRODUCT(HttpStatus.BAD_REQUEST, "Thông tin sản phẩm không hợp lệ"),
  ORDER_INVALID_PRODUCT_QUANTITY(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm không hợp lệ"),

  // Product
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"),
  PRODUCT_NOT_OWNED(HttpStatus.FORBIDDEN, "Sản phẩm %s không thuộc quyền sở hữu của bạn"),
  PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "Sản phẩm %s đã ngưng bán"),
  PRODUCT_INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm %s vượt quá tồn kho (%d)"),
  PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm %s đã hết hàng"),

  // ServiceType
  SERVICE_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "Dịch vụ vận chuyển không tồn tại"),
  SERVICE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Mã dịch vụ không hợp lệ"),

  // Promotion
  PROMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Khuyến mãi không tồn tại"),
  PROMOTION_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "Bạn không đủ điều kiện để dùng mã giảm giá"),
  PROMOTION_EXPIRED(HttpStatus.BAD_REQUEST, "Khuyến mãi bạn chọn có thể đã thay đổi, hết hạn hoặc hết lượt sử dụng."),
  PROMOTION_INVALID(HttpStatus.NOT_FOUND, "Khuyến mãi không hợp lệ"),

  // Office
  OFFICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Bưu cục không tồn tại"),

  // SettlementBatch
  SETTLEMENT_NO_PENDING_DEBT(HttpStatus.BAD_REQUEST, "Không có khoản nợ nào cần thanh toán"),
  SETTLEMENT_MIN_PAYMENT_REQUIRED(HttpStatus.BAD_REQUEST, "Tổng nợ phải từ %s₫ trở lên"),

  // Settlement Transaction
  TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch: %s"),
  TRANSACTION_INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, "Chữ ký không hợp lệ"),
  TRANSACTION_PROCESSING_ERROR(HttpStatus.BAD_REQUEST, "Không có giao dịch để xử lý"),

  ;

  private final HttpStatus httpStatus;
  private final String message;

  public String getCode() {
    return this.name();
  }
}
