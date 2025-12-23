import React, { useState } from "react";
import { Typography, Collapse } from "antd";
import dayjs from "dayjs";
import type { OrderHistory } from "../../../../../types/orderHistory";
import { getOrderHistoryActionText } from "../../../../../utils/orderHistoryUtils";

const { Title } = Typography;
const { Panel } = Collapse;

interface OrderHistoryCardProps {
  histories: OrderHistory[];
}

const OrderHistoryCard: React.FC<OrderHistoryCardProps> = ({ histories }) => {
  const [activeKey, setActiveKey] = useState<string[]>([]);

  return (
    <Collapse
      activeKey={activeKey}
      onChange={(keys) => setActiveKey(keys as string[])}
      bordered={false}
      expandIconPosition="end"
      className="order-detail-history-collapse"
    >
      <Panel
        header={<Title level={5} className="order-detail-history-panel-header">Lịch sử đơn hàng</Title>}
        key="1"
        className="order-detail-history-panel"
      >
        {histories.length === 0 ? (
          <div className="order-detail-history-no-history">Không có lịch sử</div>
        ) : (
          <div className="order-detail-history-timeline-container">
            <div className="order-detail-history-timeline-line" />
            {histories.map((item, index) => (
              <div key={index} className="order-detail-history-timeline-item">
                <div className="order-detail-history-timeline-dot" />
                <div className="order-detail-history-timeline-content">
                  <div className="order-detail-history-timeline-header">
                    <span>{getOrderHistoryActionText(item)}</span>
                    <span className="order-detail-history-timeline-time">
                      {dayjs(item.actionTime).format("DD/MM/YYYY HH:mm")}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Panel>
    </Collapse>
  );
};

export default OrderHistoryCard;