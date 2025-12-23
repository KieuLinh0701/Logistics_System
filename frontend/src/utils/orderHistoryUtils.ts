import type { OrderHistory } from "../types/orderHistory";

export const getOrderHistoryActionText = (history: OrderHistory) => {
  const fromOffice = history.fromOfficeName ?? "";
  const toOffice = history.toOfficeName ?? "";

  switch (history.action) {
    case "PENDING":
      return "Đơn hàng đã được tạo";
    case "READY_FOR_PICKUP":
      return "Đơn hàng đã sẵn sàng để shipper đến lấy";
    case "PICKING_UP":
      return `Shipper đang đến lấy hàng từ người gửi`;
    case "PICKED_UP":
      return `Đơn hàng đã được lấy từ người gửi`;
    case "IMPORTED":
      return `Đơn hàng đã nhập kho ${toOffice}`;
    case "EXPORTED":
      return `Đơn hàng đã xuất kho ${fromOffice}`;
    case "DELIVERING":
      return `Đơn hàng đang được giao đến người nhận`;
    case "DELIVERED":
      return `Đơn hàng đã giao thành công`;
    case "FAILED_DELIVERY":
      return `Giao hàng không thành công`;
    case "RETURNING":
      return `Đơn hàng đang được hoàn trả về kho`;
    case "RETURNED":
      return `Đơn hàng đã được hoàn trả thành công`;
    case "CANCELLED":
      return `Đơn hàng đã bị hủy`;
    default:
      return `Cập nhật trạng thái đơn hàng`;
  }
};
