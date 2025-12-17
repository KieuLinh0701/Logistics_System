import React, { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { Button, Select, Card, Space, Row, Col, message } from "antd";
import { PrinterOutlined, SettingOutlined } from "@ant-design/icons";
import type { OrderPrint } from "../../../types/order";
import orderApi from "../../../api/orderApi";
import locationApi from "../../../api/locationApi";
import "./UserWaybillPrint.css";

const { Option } = Select;

const getFullAddress = async (detail: string, cityCode: number | string, wardCode: number | string) => {
  try {
    const cityName = await locationApi.getCityNameByCode(Number(cityCode)) || "Unknown";
    const wardName = await locationApi.getWardNameByCode(Number(cityCode), Number(wardCode)) || "Unknown";
    return `${detail}, ${wardName}, ${cityName}`;
  } catch {
    return `${detail}, Unknown Ward, Unknown City`;
  }
};

const ContactInfo: React.FC<{
  name: string;
  detail: string;
  wardCode: number;
  cityCode: number;
  phone: string;
}> = ({ name, detail, wardCode, cityCode, phone }) => {
  const [fullAddress, setFullAddress] = useState("Đang tải...");

  useEffect(() => {
    getFullAddress(detail, cityCode, wardCode).then(setFullAddress);
  }, [detail, cityCode, wardCode]);

  return (
    <div className="waybill-print-section-content">
      <div className="waybill-print-contact-name">{name}</div>
      <div className="waybill-print-contact-address">{fullAddress}</div>
      <div className="waybill-print-contact-phone">{phone}</div>
    </div>
  );
};

const ProductsList: React.FC<{ products?: OrderPrint['orderProducts'] }> = ({ products }) => (
  <div className="waybill-print-products-list">
    {products?.length ? products.map((p, idx) => (
      <div key={idx} className="waybill-print-product-item">
        {idx + 1}. {p.productName}, SL: {p.quantity}
      </div>
    )) : <div className="waybill-print-product-item">Không có thông tin sản phẩm</div>}
  </div>
);

const CodWeightSection: React.FC<{ cod: number; weight: number }> = ({ cod, weight }) => {
  const formatCurrency = (amount: number) => amount.toLocaleString("vi-VN") + " VND";
  return (
    <Row gutter={16}>
      <Col span={12}>
        <div className="waybill-print-cod-amount">
          <div className="waybill-print-amount-label">Tiền thu Người nhận:</div>
          <div className="waybill-print-amount-value">{formatCurrency(cod)}</div>
        </div>
      </Col>
      <Col span={12}>
        <div className="waybill-print-weight-info">
          <div className="waybill-print-weight-label">Khối lượng tối đa:</div>
          <div className="waybill-print-weight-value">{weight} Kg</div>
        </div>
      </Col>
    </Row>
  );
};

const UserWaybillPrint: React.FC = () => {
  const [orders, setOrders] = useState<OrderPrint[]>([]);
  const [pageSize, setPageSize] = useState<"A4" | "A5" | "A6">("A4");
  const [orientation, setOrientation] = useState<"portrait" | "landscape">("portrait");
  const [loading, setLoading] = useState(true);
  const location = useLocation();
  const hasShownMessage = useRef(false);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const orderIdsParam = params.get("orderIds");
    if (!orderIdsParam) return;

    const orderIds = orderIdsParam.split(",").map(Number);

    const fetchOrders = async () => {
      setLoading(true);
      try {
        const result = await orderApi.printUserOrders(orderIds);
        if (result.success && result.data) {
          if (result.data.length === 0 && !hasShownMessage.current) {
            message.warning("Không có đơn hàng nào đủ điều kiện in");
            hasShownMessage.current = true;
          } else if (!hasShownMessage.current) {
            setOrders(result.data);
            message.success(`Lấy thành công ${result.data.length} đơn hàng đủ điều kiện in phiếu vận đơn`);
            hasShownMessage.current = true;
          }
        } else if (!hasShownMessage.current) {
          message.error(result.message || "Lấy thông tin in ấn thất bại");
          hasShownMessage.current = true;
        }
      } catch {
        if (!hasShownMessage.current) {
          message.error("Lỗi khi lấy thông tin in ấn");
          hasShownMessage.current = true;
        }
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, [location.search]);

  const handlePrint = () => window.print();

  const getPaperDimensions = () => {
    const dims = { A4: { w: 210, h: 297 }, A5: { w: 148, h: 210 }, A6: { w: 105, h: 148 } };
    const d = dims[pageSize];
    return orientation === "landscape"
      ? { width: `${d.h}mm`, height: `${d.w}mm` }
      : { width: `${d.w}mm`, height: `${d.h}mm` };
  };

  if (loading) return (
    <div className="waybill-print-loading">
      <div className="waybill-print-spinner"></div>
      <p>Đang tải thông tin vận đơn...</p>
    </div>
  );
  if (!orders.length) return <div className="waybill-print-empty"><p>Không có vận đơn để in</p></div>;

  const dimensions = getPaperDimensions();

  return (
    <div className="waybill-print-container">
      <div className="waybill-print-control-panel">
        <Card className="waybill-print-control-card">
          <div className="waybill-print-control-header">
            <SettingOutlined className="waybill-print-control-icon" />
            <h3 className="waybill-print-control-title">Cài đặt in ấn</h3>
          </div>
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <div>
              <label className="modal-lable">Kích thước giấy:</label>
              <Select value={pageSize} onChange={setPageSize} className="modal-custom-select waybill-print-control-select">
                <Option value="A4">A4 (210 × 297 mm)</Option>
                <Option value="A5">A5 (148 × 210 mm)</Option>
                <Option value="A6">A6 (105 × 148 mm)</Option>
              </Select>
            </div>
            <div>
              <label className="modal-lable">Hướng giấy:</label>
              <Select value={orientation} onChange={setOrientation} className="modal-custom-select waybill-print-control-select">
                <Option value="portrait">Dọc</Option>
                <Option value="landscape">Ngang</Option>
              </Select>
            </div>
            <div>
              <label className="modal-lable">Số lượng vận đơn:</label>
              <div className="waybill-print-order-count">{orders.length} vận đơn</div>
            </div>
            <Button type="primary" icon={<PrinterOutlined />} onClick={handlePrint} size="large" className="primary-button waybill-print-btn">
              In tất cả vận đơn
            </Button>
          </Space>
        </Card>
      </div>

      {/* Preview Area */}
      <div className="waybill-print-preview-area">
        <div className="waybill-print-preview-header">
          <h3 className="waybill-print-preview-title">Xem trước khi in</h3>
          <div className="waybill-print-preview-info">
            <span className="waybill-print-paper-size">{pageSize} {orientation === "portrait" ? "Dọc" : "Ngang"}</span>
            <span className="waybill-print-dimensions">{dimensions.width} × {dimensions.height}</span>
          </div>
        </div>

        <div className="waybill-print-area" style={{ maxWidth: dimensions.width, margin: '0 auto' }}>
          {orders.map((order, idx) => (
            <div key={order.trackingNumber} className={`waybill-print-card waybill-print-${pageSize} waybill-print-${orientation}`} style={{ pageBreakAfter: idx < orders.length - 1 ? 'always' : 'auto' }}>

              {/* Header */}
              <div className="waybill-print-header">
                <div className="waybill-print-header-top">
                  <div className="waybill-print-tracking-info">
                    <div className="waybill-print-tracking-label">Mã vận đơn:</div>
                    <div className="waybill-print-tracking-number">{order.trackingNumber}</div>
                    {order.barcodeTrackingNumber && <img src={`data:image/png;base64,${order.barcodeTrackingNumber}`} alt="Barcode" className="waybill-print-barcode" />}
                  </div>
                  <div className="waybill-print-office-info">
                    <div className="waybill-print-office-code">
                      <div className="waybill-print-office-label">Bưu cục gửi:</div>
                      <div className="waybill-print-office-value">{order.fromOfficeCode}</div>
                    </div>
                    {order.qrFromOfficeCode && <img src={`data:image/png;base64,${order.qrFromOfficeCode}`} alt="QR" className="waybill-print-office-qr" />}
                  </div>
                </div>
                <hr className="waybill-print-divider" />
              </div>

              {/* From & To */}
              <div className="waybill-print-contact-row">
                <div className="waybill-print-contact-column">
                  <div className="waybill-print-section-title">TỪ</div>
                  <ContactInfo
                    name={order.senderName}
                    detail={order.senderDetail}
                    wardCode={order.senderWardCode}
                    cityCode={order.senderCityCode}
                    phone={order.senderPhone}
                  />
                </div>
                <div className="waybill-print-contact-column">
                  <div className="waybill-print-section-title">ĐẾN</div>
                  <ContactInfo
                    name={order.recipientAddress.name}
                    detail={order.recipientAddress.detail}
                    wardCode={order.recipientAddress.wardCode}
                    cityCode={order.recipientAddress.cityCode}
                    phone={order.recipientAddress.phoneNumber}
                  />
                </div>
              </div>
              <hr className="waybill-print-divider" />

              {/* Products */}
              <div className="waybill-print-section">
                <div className="waybill-print-section-title">Nội dung hàng (Tổng SL: {order.orderProducts?.length || 0})</div>
                <ProductsList products={order.orderProducts} />
              </div>
              <hr className="waybill-print-divider" />

              {/* COD & Weight */}
              <CodWeightSection cod={order.codAmount} weight={order.weight} />
              <hr className="waybill-print-divider" />

              {/* Instructions */}
              <div className="waybill-print-section">
                <div className="waybill-print-section-title">Chỉ dẫn giao hàng</div>
                <div className="waybill-print-instructions">
                  <div className="waybill-print-instruction-item">Không đóng kiểm;</div>
                  <div className="waybill-print-instruction-item">Chuyển hoàn sau 3 lần phát;</div>
                  <div className="waybill-print-instruction-item">Lưu kho tối đa 5 ngày.</div>
                </div>
              </div>

              {/* Signature */}
              <div className="waybill-print-signature-section">
                <div className="waybill-print-signature-title">Chữ ký người nhận</div>
                <div className="waybill-print-signature-line"></div>
                <div className="waybill-print-signature-note">Xác nhận hàng nguyên vẹn, không móp/méo, bể/vỡ</div>
              </div>

              {/* Footer */}
              <div className="waybill-print-footer">Ngày in: {new Date().toLocaleDateString('vi-VN')}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default UserWaybillPrint;