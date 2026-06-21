import React from "react";
import {Descriptions, Drawer} from "antd";
import dayjs from "dayjs";
import type {Promotion} from "../../../../types/promotion";

interface PromotionDetailsDrawerProps {
  open: boolean;
  promotion: Promotion | null;
  statusText: (value?: string) => string;
  promotionTypeText: (promotion: Promotion) => string;
  onClose: () => void;
}

const PromotionDetailsDrawer: React.FC<PromotionDetailsDrawerProps> = ({
  open,
  promotion,
  statusText,
  promotionTypeText,
  onClose,
}) => {
  return (
    <Drawer
      title="Chi tiết khuyến mãi"
      placement="right"
      width={620}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      {promotion && (
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Mã">
            {promotion.code}
          </Descriptions.Item>

          <Descriptions.Item label="Trạng thái">
            {statusText(promotion.status)}
          </Descriptions.Item>

          <Descriptions.Item label="Tiêu đề">
            {promotion.title || "-"}
          </Descriptions.Item>

          <Descriptions.Item label="Loại">
            {promotionTypeText(promotion)}
          </Descriptions.Item>

          <Descriptions.Item label="Kiểu giảm giá">
            {promotion.discountType === "PERCENTAGE"
              ? "Phần trăm"
              : "Số tiền cố định"}
          </Descriptions.Item>

          <Descriptions.Item label="Giá trị giảm">
            {promotion.discountType === "PERCENTAGE"
              ? `${promotion.discountValue}%`
              : `${promotion.discountValue.toLocaleString("vi-VN")}đ`}
          </Descriptions.Item>

          <Descriptions.Item label="Giảm tối đa">
            {promotion.maxDiscountAmount
              ? `${promotion.maxDiscountAmount.toLocaleString("vi-VN")}đ`
              : "-"}
          </Descriptions.Item>

          <Descriptions.Item label="Ngày bắt đầu">
            {dayjs(promotion.startDate).format("DD/MM/YYYY")}
          </Descriptions.Item>

          <Descriptions.Item label="Ngày kết thúc">
            {dayjs(promotion.endDate).format("DD/MM/YYYY")}
          </Descriptions.Item>

          <Descriptions.Item label="Số lượt dùng">
            {promotion.usedCount}
          </Descriptions.Item>

          <Descriptions.Item label="Giới hạn tổng">
            {promotion.usageLimit || "∞"}
          </Descriptions.Item>

          <Descriptions.Item label="Mô tả">
            {promotion.description || "-"}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Drawer>
  );
};

export default PromotionDetailsDrawer;