import React from "react";
import {Descriptions, Drawer, List, Spin} from "antd";
import type {AdminOrder} from "../../../../types/order";

interface OrderDetailsDrawerProps {
  open: boolean;
  loading: boolean;
  selectedOrder: AdminOrder | null;
  statusText: (status?: string) => string;
  paymentText: (paymentStatus?: string, payer?: string) => string;
  onClose: () => void;
}

const OrderDetailsDrawer: React.FC<OrderDetailsDrawerProps> = ({
  open,
  loading,
  selectedOrder,
  statusText,
  paymentText,
  onClose,
}) => {
  const senderFullAddress =
    selectedOrder?.senderFullAddress ||
    selectedOrder?.senderAddress?.fullAddress ||
    selectedOrder?.senderDetail ||
    "-";

  const recipientFullAddress =
    selectedOrder?.recipientFullAddress ||
    selectedOrder?.recipientAddress?.fullAddress ||
    selectedOrder?.recipientDetail ||
    "-";

  return (
    <Drawer title="Chi tiết đơn hàng" placement="right" width={620} open={open} onClose={onClose}>
      {loading ? (
        <div style={{ textAlign: "center", padding: 40 }}>
          <Spin />
        </div>
      ) : selectedOrder ? (
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Mã vận đơn">{selectedOrder.trackingNumber}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">{statusText(selectedOrder.status)}</Descriptions.Item>
          <Descriptions.Item label="Bưu cục xuất">{selectedOrder.fromOffice?.name || "-"}</Descriptions.Item>
          <Descriptions.Item label="Bưu cục nhận">{selectedOrder.toOffice?.name || "-"}</Descriptions.Item>
          <Descriptions.Item label="Người gửi">{selectedOrder.senderName || "-"}</Descriptions.Item>
          <Descriptions.Item label="SĐT người gửi">{selectedOrder.senderPhone || "-"}</Descriptions.Item>
          <Descriptions.Item label="Người nhận">{selectedOrder.recipientAddress?.name || selectedOrder.recipientName || "-"}</Descriptions.Item>
          <Descriptions.Item label="SĐT người nhận">{selectedOrder.recipientAddress?.phoneNumber || selectedOrder.recipientPhone || "-"}</Descriptions.Item>
          <Descriptions.Item label="Địa chỉ người gửi">{senderFullAddress}</Descriptions.Item>
          <Descriptions.Item label="Địa chỉ người nhận">{recipientFullAddress}</Descriptions.Item>
          <Descriptions.Item label="Dịch vụ">{selectedOrder.serviceType?.name || selectedOrder.serviceTypeName || "-"}</Descriptions.Item>
          <Descriptions.Item label="Trọng lượng">{selectedOrder.weight ? `${selectedOrder.weight} kg` : "-"}</Descriptions.Item>
          <Descriptions.Item label="Phí vận chuyển">
            {selectedOrder.shippingFee != null
              ? `${selectedOrder.shippingFee.toLocaleString("vi-VN")} VNĐ`
              : selectedOrder.totalFee != null
                ? `${selectedOrder.totalFee.toLocaleString("vi-VN")} VNĐ`
                : "-"}
          </Descriptions.Item>
          <Descriptions.Item label="COD">{selectedOrder.cod ? `${selectedOrder.cod.toLocaleString("vi-VN")} VNĐ` : "0 VNĐ"}</Descriptions.Item>
          <Descriptions.Item label="Tổng phí">{selectedOrder.totalFee ? `${selectedOrder.totalFee.toLocaleString("vi-VN")} VNĐ` : "0 VNĐ"}</Descriptions.Item>
          <Descriptions.Item label="Thanh toán">{paymentText(selectedOrder.paymentStatus, selectedOrder.payer)}</Descriptions.Item>
          <Descriptions.Item label="Sản phẩm">
            {Array.isArray(selectedOrder.orderProducts) && selectedOrder.orderProducts.length > 0 ? (
              <List
                size="small"
                dataSource={selectedOrder.orderProducts}
                renderItem={(item) => (
                  <List.Item>
                    <div style={{ display: "flex", justifyContent: "space-between", width: "100%" }}>
                      <div>{item.productName || item.productCode || "-"} x{item.quantity}</div>
                      <div>{item.price ? `${item.price.toLocaleString("vi-VN")}đ` : ""}</div>
                    </div>
                  </List.Item>
                )}
              />
            ) : (
              "-"
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Ghi chú">{selectedOrder.notes || "-"}</Descriptions.Item>
          <Descriptions.Item label="Ngày tạo">{selectedOrder.createdAt ? new Date(selectedOrder.createdAt).toLocaleString() : "-"}</Descriptions.Item>
        </Descriptions>
      ) : null}
    </Drawer>
  );
};

export default OrderDetailsDrawer;
