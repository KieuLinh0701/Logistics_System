import React from "react";
import { Descriptions, Drawer, List, Spin } from "antd";
import type { AdminOrder, Order } from "../../../../types/order";

interface OrderDetailsDrawerProps {
  open: boolean;
  loading: boolean;
  selectedOrder: Order | AdminOrder | null;
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
          <Descriptions.Item label="Người nhận">{(selectedOrder as Order).recipientAddress?.name || selectedOrder.recipientName || "-"}</Descriptions.Item>
          <Descriptions.Item label="SĐT người nhận">{(selectedOrder as Order).recipientAddress?.phoneNumber || (selectedOrder as any).recipientPhone || "-"}</Descriptions.Item>
          <Descriptions.Item label="Dịch vụ">{(selectedOrder as any).serviceTypeName || (selectedOrder as Order).serviceType?.name || "-"}</Descriptions.Item>
          <Descriptions.Item label="Trọng lượng">{(selectedOrder as any).weight ? `${(selectedOrder as any).weight} kg` : "-"}</Descriptions.Item>
          <Descriptions.Item label="Phí vận chuyển">
            {(selectedOrder as any).shippingFee != null
              ? `${(selectedOrder as any).shippingFee.toLocaleString("vi-VN")} VNĐ`
              : selectedOrder.totalFee != null
                ? `${selectedOrder.totalFee.toLocaleString("vi-VN")} VNĐ`
                : "-"}
          </Descriptions.Item>
          <Descriptions.Item label="COD">{(selectedOrder as any).cod ? `${(selectedOrder as any).cod.toLocaleString("vi-VN")} VNĐ` : "0 VNĐ"}</Descriptions.Item>
          <Descriptions.Item label="Tổng phí">{selectedOrder.totalFee ? `${selectedOrder.totalFee.toLocaleString("vi-VN")} VNĐ` : "0 VNĐ"}</Descriptions.Item>
          <Descriptions.Item label="Thanh toán">{paymentText((selectedOrder as any).paymentStatus, (selectedOrder as any).payer)}</Descriptions.Item>
          <Descriptions.Item label="Sản phẩm">
            {Array.isArray((selectedOrder as any).orderProducts) && (selectedOrder as any).orderProducts.length > 0 ? (
              <List
                size="small"
                dataSource={(selectedOrder as any).orderProducts}
                renderItem={(item: any) => (
                  <List.Item>
                    <div style={{ display: "flex", justifyContent: "space-between", width: "100%" }}>
                      <div>{item.name} x{item.quantity}</div>
                      <div>{item.price ? `${item.price.toLocaleString("vi-VN")}đ` : ""}</div>
                    </div>
                  </List.Item>
                )}
              />
            ) : (
              "-"
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Ghi chú">{(selectedOrder as any).notes || "-"}</Descriptions.Item>
          <Descriptions.Item label="Ngày tạo">{selectedOrder.createdAt ? new Date(selectedOrder.createdAt).toLocaleString() : "-"}</Descriptions.Item>
        </Descriptions>
      ) : null}
    </Drawer>
  );
};

export default OrderDetailsDrawer;
