import React, { useEffect, useState } from "react";
import { Typography, message, Spin, Table, Card, Row, Col, Button } from "antd";
import "./ShippingRates.css";

import type { ServiceTypeWithShippingRatesResponse } from "../../../../types/serviceType";
import serviceTypeApi from "../../../../api/serviceTypeApi";
import { getShippingRateRegionTypeNote, translateShippingRateRegionType } from "../../../../utils/shippingRateUtils";

const { Title } = Typography;

const ShippingRatesBody: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState<any[]>([]);
  const [regionTypes, setRegionTypes] = useState<string[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await serviceTypeApi.getActiveServicesWithRates();
        if (result.success) {
          const list: ServiceTypeWithShippingRatesResponse[] = result.data || [];

          const uniqueRegions = Array.from(
            new Set(list.flatMap(s => s.rates.map(r => r.regionType)))
          );
          setRegionTypes(uniqueRegions);

          const merged: any[] = [];

          list.forEach(service => {
            const grouped: Record<string, any> = {};

            // Gom theo khoảng cân nặng
            service.rates.forEach(r => {
              const key = `${r.weightFrom}-${r.weightTo || "∞"}`;
              if (!grouped[key]) {
                grouped[key] = {
                  serviceName: service.name,
                  deliveryTime: service.deliveryTime,
                  weightFrom: r.weightFrom,
                  weightTo: r.weightTo || Infinity,
                  extraPrice: r.extraPrice,
                  unit: r.unit,
                };
              }
              grouped[key][r.regionType] = r.price;
            });

            const serviceRows = Object.values(grouped);
            serviceRows.forEach((row: any, index: number) => {
              row.serviceRowSpan = index === 0 ? serviceRows.length : 0;
            });

            merged.push(...serviceRows);
          });

          setRows(merged);
        }
      } catch {
        message.error("Không thể tải bảng giá");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const columns: any[] = [
    {
      title: "Gói dịch vụ",
      dataIndex: "serviceName",
      key: "serviceName",
      width: 200,
      align: "center",
      render: (text: string, row: any) => ({
        children: (
          <div className="shipping-rates-service-info">
            <span className="shipping-rates-service-name">{text}</span>
            <span className="shipping-rates-delivery-time">{row.deliveryTime}</span>
          </div>
        ),
        props: { rowSpan: row.serviceRowSpan }
      })
    },
    {
      title: "Khối lượng (kg)",
      key: "weight",
      width: 130,
      align: "center",
      render: (row: any) => (
        <span className="shipping-rates-item">
          {row.weightFrom} - {row.weightTo === Infinity ? "∞" : row.weightTo}
        </span>
      )
    },
    ...regionTypes.map(rt => ({
      title: translateShippingRateRegionType(rt),
      dataIndex: rt,
      key: rt,
      width: 100,
      align: "center",
      render: (price: number) =>
        <span className="shipping-rates-item">{price.toLocaleString()}</span>
    })),
    {
      title: "Phụ phí",
      dataIndex: "extraPrice",
      key: "extraPrice",
      width: 160,
      align: "center",
      render: (extra: number, row: any) => {
        if (!extra) return "";
        const unitLabel = row.unit % 1 === 0 ? row.unit : row.unit.toFixed(1);
        return (
          <span className="shipping-rates-item">
            Tăng {extra.toLocaleString()} mỗi {unitLabel}kg
          </span>
        );
      }
    }
  ];

  if (loading) {
    return (
      <div className="shipping-rates-layout">
        <div className="shipping-rates-loading">
          <Spin size="large" />
          <div className="shipping-rates-loading-text">Đang tải bảng giá...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="shipping-rates-layout">
      <div className="shipping-rates-content">
        <div className="shipping-rates-header">
          <Title level={2}>Bảng giá vận chuyển</Title>
        </div>

        <div className="shipping-rates-region-notes">
          <Title level={4}>Chú thích loại miền</Title>
          <ul>
            {regionTypes.map(rt => (
              <li key={rt}>
                <strong>{translateShippingRateRegionType(rt)}:</strong> {getShippingRateRegionTypeNote(rt)}
              </li>
            ))}
          </ul>
        </div>

        <div className="shipping-rates-table-container">
          <Table
            columns={columns}
            dataSource={rows}
            rowKey={(r) => `${r.serviceName}-${r.weightFrom}-${r.weightTo}`}
            scroll={{ x: 1000 }}
            pagination={false}
            className="shipping-rates-table"
          />
        </div>

        {/* Hướng dẫn sử dụng bảng giá */}
        <Card className="shipping-rates-guide-card">
          <Title level={3} className="shipping-rates-guide-title">
            Hướng dẫn sử dụng bảng giá
          </Title>

          <Row gutter={[24, 24]}>
            <Col span={12}>
              <div className="shipping-rates-guide-section">
                <Title level={4} className="shipping-rates-guide-subtitle">Cách tính phí</Title>
                <ol className="shipping-rates-guide-list">
                  <li>Chọn dịch vụ phù hợp nhu cầu giao hàng</li>
                  <li>Xác định khu vực gửi và nhận</li>
                  <li>Đo khối lượng đơn hàng</li>
                  <li>Tra cứu mức giá tương ứng trong bảng</li>
                  <li>Phí có thể tăng thêm nếu vượt quá mức khối lượng cơ bản</li>
                </ol>
              </div>
            </Col>

            <Col span={12}>
              <div className="shipping-rates-guide-section">
                <Title level={4} className="shipping-rates-guide-subtitle">Lưu ý</Title>
                <ul className="shipping-rates-guide-list">
                  <li>Một số khoản phí tiêu chuẩn (VAT, phí đóng gói...) có thể được áp dụng tùy theo đơn hàng</li>
                  <li>Giá có thể thay đổi theo từng thời điểm hoặc khu vực</li>
                  <li>Nên kiểm tra phí thực tế trước khi gửi hàng</li>
                </ul>
              </div>
            </Col>
          </Row>
        </Card>

      </div>
    </div>
  );
};

export default ShippingRatesBody;