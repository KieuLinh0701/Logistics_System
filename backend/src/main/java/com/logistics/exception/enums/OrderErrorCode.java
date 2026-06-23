package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements BaseErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),
    ORDER_NOT_IN_TRANSIT(HttpStatus.BAD_REQUEST, "Đơn hàng %s không ở trạng thái IN_TRANSIT"),
    ORDER_SENDER_ADDRESS_NOT_BELONG(HttpStatus.BAD_REQUEST, "Địa chỉ người gửi không thuộc người dùng"),
    ORDER_SENDER_ADDRESS_INVALID(HttpStatus.NOT_FOUND, "Địa chỉ người gửi không hợp lệ"),
    ORDER_RECIPIENT_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Địa chỉ người nhận không tồn tại"),
    ORDER_OFFICE_CITY_MISMATCH(HttpStatus.BAD_REQUEST, "Địa chỉ gửi và bưu cục phải thuộc cùng thành phố"),
    ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "Đơn hàng đã chuyển sang xử lý, không thể hủy"),
    ORDER_CANNOT_CONFIRM(HttpStatus.BAD_REQUEST, "Trạng thái đơn hàng hoặc hình thức lấy hàng không phù hợp"),
    ORDER_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "Đơn hàng đã chuyển sang xử lý, không thể xóa"),
    ORDER_SHIPPING_FEE_CHANGED(HttpStatus.BAD_REQUEST, "Phí vận chuyển vừa được cập nhật. Vui lòng kiểm tra lại trước khi tạo đơn."),
    ORDER_PRODUCT_INFO_CHANGED(HttpStatus.BAD_REQUEST, "Thông tin của sản phẩm đã thay đổi. Vui lòng kiểm tra lại các sản phẩm đã chọn trước khi tạo đơn."),
    ORDER_PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "Sản phẩm '%s' đã ngưng bán, không thể tạo đơn"),
    ORDER_PICKUP_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng của bạn không hợp lệ để chuyển"),
    ORDER_NOT_FOUND_TO_PRINT(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng nào để in"),
    ORDER_CANNOT_EDIT(HttpStatus.BAD_REQUEST, "Đơn hàng đã hoàn thành không thể chỉnh sửa"),
    ORDER_FIELD_UPDATE_DENIED(HttpStatus.BAD_REQUEST, "Bạn không thể thay đổi '%s' khi đơn hàng đang ở trạng thái %s"),
    ORDER_INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Không thể chuyển trạng thái đơn hàng từ '%s' sang '%s'."),
    ORDER_SENDER_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ người gửi"),
    ORDER_INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái đơn hàng không hợp lệ"),
    ORDER_INVALID_PICKUP_TYPE(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng không hợp lệ"),
    ORDER_INVALID_PAYER(HttpStatus.BAD_REQUEST, "Người thanh toán không hợp lệ"),
    ORDER_RECIPIENT_ADDRESS_INVALID(HttpStatus.NOT_FOUND, "Địa chỉ người nhận không hợp lệ"),
    ORDER_INVALID_RECIPIENT_PHONE(HttpStatus.BAD_REQUEST, "Số điện thoại người nhận phải đúng 10 chữ số"),
    ORDER_INVALID_SENDER_PHONE(HttpStatus.BAD_REQUEST, "Số điện thoại người gửi phải đúng 10 chữ số"),
    ORDER_INVALID_SENDER_CITY_CODE(HttpStatus.BAD_REQUEST, "Thành phố người gửi không hợp lệ"),
    ORDER_INVALID_SENDER_WARD_CODE(HttpStatus.BAD_REQUEST, "Phường/Xã người gửi không hợp lệ"),
    ORDER_INVALID_SENDER_COORDINATES(HttpStatus.BAD_REQUEST, "Tọa độ người gửi không hợp lệ"),
    ORDER_INVALID_RECIPIENT_CITY_CODE(HttpStatus.BAD_REQUEST, "Thành phố người nhận không hợp lệ"),
    ORDER_INVALID_RECIPIENT_WARD_CODE(HttpStatus.BAD_REQUEST, "Phường/Xã ngươời nhận không hợp lệ"),
    ORDER_INVALID_RECIPIENT_COORDINATES(HttpStatus.BAD_REQUEST, "Tọa độ không hợp lệ"),
    ORDER_INVALID_WEIGHT(HttpStatus.BAD_REQUEST, "Khối lượng phải lớn hơn 0"),
    ORDER_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "Chiều dài phải lớn hơn 0"),
    ORDER_INVALID_WIDTH(HttpStatus.BAD_REQUEST, "Chiều rộng phải lớn hơn 0"),
    ORDER_INVALID_HEIGHT(HttpStatus.BAD_REQUEST, "Chiều cao phải lớn hơn 0"),
    ORDER_INVALID_COD_VALUE(HttpStatus.BAD_REQUEST, "Phí thu hộ không hợp lệ"),
    ORDER_INVALID_ORDER_VALUE(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng phải lớn hơn hoặc bằng 0"),
    ORDER_INVALID_SERVICE_TYPE_ID(HttpStatus.BAD_REQUEST, "Dịch vụ không hợp lệ"),
    ORDER_INVALID_PROMOTION_ID(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không hợp lệ"),
    ORDER_FROM_OFFICE_REQUIRED(HttpStatus.BAD_REQUEST, "Bưu cục nhận hàng không được để trống"),
    ORDER_NOTE_TOO_LONG(HttpStatus.BAD_REQUEST, "Ghi chú tối đa 1000 ký tự"),
    ORDER_INVALID_PRODUCT(HttpStatus.BAD_REQUEST, "Thông tin sản phẩm không hợp lệ"),
    ORDER_INVALID_PRODUCT_QUANTITY(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm không hợp lệ"),
    ORDERS_INELIGIBLE_FOR_PRINT(HttpStatus.BAD_REQUEST, "Không đủ điều kiện để in"),
    ORDER_OFFICE_MISMATCH(HttpStatus.FORBIDDEN, "Đơn hàng không thuộc bưu cục của bạn"),
    ORDER_NOT_ASSIGNED(HttpStatus.FORBIDDEN, "Chỉ shipper được gán mới có thể thao tác"),
    ORDER_NOT_DELIVERING(HttpStatus.BAD_REQUEST, "Chỉ có thể báo khi đơn đang giao"),
    ORDER_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "Số lượng không hợp lệ"),
    ORDER_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "Số lượng vượt quá số lượng còn lại hoặc đã có thay đổi"),
    ORDER_INVALID_CLAIM_STATUS(HttpStatus.BAD_REQUEST, "Chỉ có thể nhận đơn ở trạng thái đã xác nhận, đã đến bưu cục đích hoặc sẵn sàng lấy"),
    ORDER_ALREADY_CLAIMED(HttpStatus.CONFLICT, "Đơn hàng đã được nhận bởi shipper khác"),
    ORDER_PARTIAL_DELIVERY_INVALID(HttpStatus.BAD_REQUEST, "Đơn đang giao 1 phần, không đi qua luồng delivery retry"),
    ORDER_MISSING_FAIL_REASON(HttpStatus.BAD_REQUEST, "failReason là bắt buộc"),
    ORDER_INVALID_DELIVERY_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái giao hàng không hợp lệ"),
    ORDERS_NOT_FOUND_TO_PRINT(HttpStatus.BAD_REQUEST, "Không tìm thấy đơn hàng nào để in"),
    ORDER_AUTO_ASSIGN_OFFICE_FAILED(HttpStatus.BAD_REQUEST,"Không thể tự động gán bưu cục xuất");
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
