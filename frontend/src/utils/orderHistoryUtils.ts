import type {OrderHistory} from "../types/orderHistory";

const isPickupByCourier = (h: OrderHistory) =>
    h.pickupType === "PICKUP_BY_COURIER";

const isReturnLeg = (h: OrderHistory) =>
    h.stopType === "RETURN_TO_OFFICE";

export const getOrderHistoryActionText = (history: OrderHistory) => {
  const fromOffice = history.fromOfficeName ?? "";
  const toOffice = history.toOfficeName ?? "";

  switch (history.action) {
    case "PENDING":
      return "Đơn hàng đã được tạo";
    case "READY_FOR_PICKUP":
      return "Đơn hàng đã sẵn sàng để shipper đến lấy";
    case "TRANSIT_TO_OFFICE":
      return "Đơn hàng đang được chuyển về bưu cục gốc";
    case "CONFIRMED":
      return "Đơn hàng đã được xác nhận";
    case "URGENT_PICKUP":
      return "Đơn hàng ưu tiên cần shipper đến lấy ngay";
    case "PICKUP_ACCEPTED":
      return isReturnLeg(history)
          ? "Shipper đã nhận đơn hoàn để giao trả"
          : "Shipper đã nhận yêu cầu lấy hàng";
    case "PICKING_UP":
      return isReturnLeg(history)
          ? "Shipper bắt đầu đi nhận đơn hoàn trả"
          : "Shipper bắt đầu chuyến lấy hàng";
    case "PICKED_UP":
      if (isReturnLeg(history)) {
          return "Shipper đã nhận đơn hoàn lên xe";
      }
      if (isPickupByCourier(history)) {
          return "Shipper đã lấy hàng từ người gửi";
      }
      return "Shipper đã xác nhận hàng lên xe";
    case "PICKUP_FAILED_FINAL":
      return "Lấy hàng thất bại quá số lần cho phép";
    case "IMPORTED":
      return toOffice
          ? `Đơn hàng đã nhập kho ${toOffice}`
          : "Đơn hàng đã nhập kho";
    case "EXPORTED":
      return fromOffice
          ? `Đơn hàng đã xuất kho ${fromOffice}`
          : "Đơn hàng đã xuất kho";
    case "DELIVERING":
      return isReturnLeg(history)
          ? "Shipper đang chở đơn hoàn về bưu cục gốc"
          : "Đơn hàng đang được giao đến người nhận";
    case "DELIVERED":
      return "Đơn hàng đã giao thành công";
    case "FAILED_DELIVERY":
      return "Giao hàng không thành công";
    case "RETURNING":
      return "Đơn hàng đang được hoàn trả về kho";
    case "RETURN_AT_ORIGIN_OFFICE":
      return "Đơn hàng đã hoàn về tới bưu cục xuất phát";
    case "RETURN_RETRY":
      return "Đơn hàng đang chờ hoàn trả lại";
    case "RETURN_FAILED_FINAL":
      return "Hoàn hàng thất bại cuối cùng";
    case "RETURNED":
      return "Đơn hàng đã được hoàn trả thành công";
    case "CANCELLED":
      return "Đơn hàng đã bị hủy";
    case "AT_DEST_OFFICE":
      return isPickupByCourier(history)
          ? "Đơn hàng đã đến bưu cục đích (chuyển tiếp sang chuyến giao)"
          : "Đơn hàng đã đến bưu cục đích";
    case "AT_ORIGIN_OFFICE":
      return isReturnLeg(history)
          ? "Đơn hàng hoàn đã về bưu cục gốc"
          : "Đơn hàng đã được bàn giao tại bưu cục gửi";
    case "ASSIGNED_TO_DELIVERY_TRIP":
      return "Đơn hàng đã được phân công vào chuyến giao";
    case "ORDER_LOADED_ON_VEHICLE":
      return "Shipper đã xác nhận hàng lên xe";
    case "DELIVERY_STARTED":
      return "Đơn hàng bắt đầu được giao đến người nhận";
    default:
      return "Cập nhật trạng thái đơn hàng";
  }
};
