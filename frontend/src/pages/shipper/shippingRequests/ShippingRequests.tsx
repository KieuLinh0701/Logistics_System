import React, { useEffect, useState } from "react";
import { Button, Table, message, Card, Row, Col, Typography } from "antd";
import { connectWebSocket, disconnectWebSocket } from "../../../socket/socket";
import { getUserId } from "../../../utils/authUtils";
import shippingRequestApi from "../../../api/shippingRequestApi";
import type { ShippingRequest } from "../../../types/shippingRequest";

export default function ShippingRequests() {
  const [list, setList] = useState<ShippingRequest[]>([]);
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const res = await shippingRequestApi.listShipperShippingRequests();
      // axiosClient trả về response.data, nên `res` có kiểu ApiResponse<ListResponse<ShippingRequest>>
      const items = res?.data?.list || [];
      setList(items);
    } catch (e) {
      message.error("Không thể tải danh sách yêu cầu");
    } finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  useEffect(() => {
    const uid = getUserId();
    if (!uid) return;
    connectWebSocket(uid, (msg) => {
      message.info(msg.title || "Thông báo mới");
      // Nếu liên quan, tải lại danh sách
      if (msg.type === "assignment" || msg.type === "shipping_request_accepted") load();
    });
    return () => { disconnectWebSocket(); };
  }, []);

  async function accept(id: number) {
    try {
      const res = await shippingRequestApi.acceptShipperShippingRequest(id);
      if (res && res.success) {
        message.success("Đã nhận yêu cầu");
        load();
      } else {
        message.error(res?.message || "Lỗi");
      }
    } catch (e) {
      message.error("Lỗi khi nhận yêu cầu");
    }
  }

  const columns = [
    { title: "ID", dataIndex: "id", key: "id" },
    { title: "Đơn hàng", dataIndex: ["order", "trackingNumber"], key: "order" },
    { title: "Người nhận", dataIndex: ["order", "recipientName"], key: "recipient" },
    { title: "Nội dung", dataIndex: "requestContent", key: "content" },
    {
      title: "Hành động",
      key: "action",
      render: (text: any, record: ShippingRequest) => (
        <Button type="primary" onClick={() => accept(record.id)}>Nhận</Button>
      ),
    },
  ];

  const { Title } = Typography;

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Title level={3} className="list-page-title-main">Yêu cầu lấy hàng</Title>
        </Col>
        <Col />
      </Row>

      <Card bordered={false}>
        <Table rowKey="id" dataSource={list} columns={columns} loading={loading} />
      </Card>
    </div>
  );
}
