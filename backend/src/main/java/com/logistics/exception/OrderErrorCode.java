package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"),
    NOT_IN_TRANSIT(HttpStatus.BAD_REQUEST, "Đơn hàng %s không ở trạng thái IN_TRANSIT"),
    SENDER_ADDRESS_NOT_BELONG(HttpStatus.BAD_REQUEST, "Địa chỉ người gửi không thuộc người dùng"),
    SENDER_ADDRESS_INVALID(HttpStatus.NOT_FOUND, "Địa chỉ người gửi không hợp lệ"),
    RECIPIENT_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Địa chỉ người nhận không tồn tại"),
    OFFICE_CITY_MISMATCH(HttpStatus.BAD_REQUEST, "Địa chỉ gửi và bưu cục phải thuộc cùng thành phố"),
    CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "Đơn hàng đã chuyển sang xử lý, không thể hủy"),
    CANNOT_DELETE(HttpStatus.BAD_REQUEST, "Đơn hàng đã chuyển sang xử lý, không thể xóa"),
    SHIPPING_FEE_CHANGED(HttpStatus.BAD_REQUEST, "Phí vận chuyển vừa được cập nhật. Vui lòng kiểm tra lại trước khi tạo đơn."),
    PRODUCT_INFO_CHANGED(HttpStatus.BAD_REQUEST, "Thông tin của sản phẩm đã thay đổi. Vui lòng kiểm tra lại các sản phẩm đã chọn trước khi tạo đơn."),
    PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "Sản phẩm '%s' đã ngưng bán, không thể tạo đơn"),
    PICKUP_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng của bạn không hợp lệ để chuyển"),
    NOT_FOUND_TO_PRINT(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng nào để in"),
    CANNOT_EDIT(HttpStatus.BAD_REQUEST, "Đơn hàng đã hoàn thành không thể chỉnh sửa"),
    FIELD_UPDATE_DENIED(HttpStatus.BAD_REQUEST, "Bạn không thể thay đổi '%s' khi đơn hàng đang ở trạng thái %s"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Không thể chuyển trạng thái đơn hàng từ '%s' sang '%s'."),
    SENDER_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ người gửi"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái đơn hàng không hợp lệ"),
    INVALID_PICKUP_TYPE(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng không hợp lệ"),
    INVALID_PAYER(HttpStatus.BAD_REQUEST, "Hình thức lấy hàng không hợp lệ"),
    RECIPIENT_ADDRESS_INVALID(HttpStatus.NOT_FOUND, "Địa chỉ người nhận không hợp lệ"),
    INVALID_RECIPIENT_PHONE(HttpStatus.BAD_REQUEST, "Số điện thoại người nhận phải đúng 10 chữ số"),
    INVALID_RECIPIENT_CITY_CODE(HttpStatus.BAD_REQUEST, "Mã Thành phố không hợp lệ"),
    INVALID_RECIPIENT_WARD_CODE(HttpStatus.BAD_REQUEST, "Mã Phường/Xã không hợp lệ"),
    INVALID_RECIPIENT_COORDINATES(HttpStatus.BAD_REQUEST, "Tọa độ không hợp lệ"),
    INVALID_WEIGHT(HttpStatus.BAD_REQUEST, "Khối lượng phải lớn hơn 0"),
    INVALID_LENGTH(HttpStatus.BAD_REQUEST, "Chiều dài phải lớn hơn 0"),
    INVALID_WIDTH(HttpStatus.BAD_REQUEST, "Chiều rộng phải lớn hơn 0"),
    INVALID_HEIGHT(HttpStatus.BAD_REQUEST, "Chiều cao phải lớn hơn 0"),
    INVALID_COD_VALUE(HttpStatus.BAD_REQUEST, "Phí thu hộ không hợp lệ"),
    INVALID_ORDER_VALUE(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng phải lớn hơn hoặc bằng 0"),
    INVALID_PROMOTION_ID(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không hợp lệ"),
    FROM_OFFICE_REQUIRED(HttpStatus.BAD_REQUEST, "Bưu cục nhận hàng không được để trống"),
    NOTE_TOO_LONG(HttpStatus.BAD_REQUEST, "Ghi chú tối đa 1000 ký tự"),
    INVALID_PRODUCT(HttpStatus.BAD_REQUEST, "Thông tin sản phẩm không hợp lệ"),
    INVALID_PRODUCT_QUANTITY(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm không hợp lệ"),

    // Order assignment / delivery errors (formerly in ShipperErrorCode)
    OFFICE_MISMATCH(HttpStatus.FORBIDDEN, "Đơn hàng không thuộc bưu cục của bạn"),
    NOT_ASSIGNED(HttpStatus.FORBIDDEN, "Chỉ shipper được gán mới có thể thao tác"),
    NOT_DELIVERING(HttpStatus.BAD_REQUEST, "Chỉ có thể báo khi đơn đang giao"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "Số lượng không hợp lệ"),
    QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "Số lượng vượt quá số lượng còn lại hoặc đã có thay đổi"),
    INVALID_CLAIM_STATUS(HttpStatus.BAD_REQUEST, "Chỉ có thể nhận đơn ở trạng thái đã xác nhận, đã đến bưu cục đích hoặc sẵn sàng lấy"),
    PARTIAL_DELIVERY_INVALID(HttpStatus.BAD_REQUEST, "Đơn đang giao 1 phần, không đi qua luồng delivery retry"),
    MISSING_FAIL_REASON(HttpStatus.BAD_REQUEST, "failReason là bắt buộc"),
    INVALID_DELIVERY_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái giao hàng không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
