import React, { useState, useEffect } from "react";
import { Modal, Input, Radio, List, Pagination } from "antd";
import type { Promotion } from "../../../../../types/promotion";
import { SearchOutlined } from "@ant-design/icons";

interface Props {
  open: boolean;
  onCancel: () => void;
  promotions: Promotion[];
  selectedPromotion: Promotion | null;
  setSelectedPromotion: (value: Promotion | null) => void;
  onSearch: (value: string) => void;
  onPageChange: (page: number, pageSize: number) => void;
  serviceFee: number;
  total: number;
  currentPage: number;
  pageSize: number;
  loading: boolean;
}

const SelectedPromoModal: React.FC<Props> = ({
  open,
  onCancel,
  promotions,
  selectedPromotion,
  setSelectedPromotion,
  onSearch,
  onPageChange,
  serviceFee,
  total,
  currentPage,
  pageSize,
  loading,
}) => {
  const [pendingPromo, setPendingPromo] = useState<Promotion | null>(null);
  const [discountAmount, setDiscountAmount] = useState(0);

  useEffect(() => {
    if (open) {
      setPendingPromo(selectedPromotion);
    }
  }, [open, selectedPromotion]);

  const handleConfirm = () => {
    setSelectedPromotion(pendingPromo);
    onCancel();
  };

  useEffect(() => {
    if (!pendingPromo) {
      setDiscountAmount(0);
      return;
    }

    let discount = 0;

    if (pendingPromo.discountType === 'FIXED') {
      discount = pendingPromo.discountValue;
    } else if (pendingPromo.discountType === 'PERCENTAGE') {
      discount = (serviceFee * pendingPromo.discountValue) / 100;
    }

    if (pendingPromo.maxDiscountAmount) {
      discount = Math.min(discount, pendingPromo.maxDiscountAmount);
    }

    discount = Math.floor(discount);

    if (serviceFee < pendingPromo.minOrderValue) {
      setSelectedPromotion(null);
      setDiscountAmount(0);
      return;
    }

    setDiscountAmount(discount);
  }, [pendingPromo]);

  return (
    <Modal
      title={<span className="modal-title">Chọn mã khuyến mãi</span>}
      open={open}
      onCancel={onCancel}
      width={600}
      className="modal-hide-scrollbar"
      footer={
        <div className="create-order-select-promo-modal footer-container">
          <div>
            {pendingPromo ? (
              <span className="create-order-select-promo-modal expiry-label">
                Đã chọn: <span className="create-order-select-promo-modal content-strong">{pendingPromo.code}</span> - Giá giảm: <span className="create-order-select-promo-modal content-strong">{discountAmount}đ</span>
              </span>
            ) : (
              <span className="create-order-select-promo-modal expiry-label">Chưa chọn mã khuyến mãi</span>
            )}
          </div>

          <div className="create-order-select-promo-modal buttons-container">
            <button
              className="modal-cancel-button"
              onClick={onCancel}
            >
              Hủy
            </button>
            <button
              className="modal-ok-button"
              onClick={handleConfirm}
              disabled={!pendingPromo}
            >
              Chọn
            </button>
          </div>
        </div>
      }
    >

      <div className="create-order-select-promo-modal modal-content-wrapper">
        <Input
          className="search-input"
          placeholder="Tìm theo mã khuyến mãi, mô tả..."
          allowClear
          onChange={(e) => onSearch(e.target.value)}
          prefix={<SearchOutlined />}
        />

        <div className="create-order-select-promo-modal promo-list-container">
          <Radio.Group
            value={pendingPromo?.id || null}
            className="create-order-select-promo-modal radio-group"
            onChange={(e) => {
              const id = e.target.value;
              const promo = promotions.find((p) => p.id === id) || null;
              setPendingPromo(promo);
            }}
          >
            <List
              dataSource={promotions}
              loading={loading} 
              renderItem={(promo) => (
                <List.Item
                  className={`create-order-select-promo-modal promo-item ${pendingPromo?.id === promo.id ? "promo-item-selected" : ""
                    }`}
                  onClick={() => setPendingPromo(promo)}
                >
                  <Radio value={promo.id} className="create-order-select-promo-modal promo-radio">
                    <div className="create-order-select-promo-modal promo-content">

                      <div className="create-order-select-promo-modal promo-header">
                        <div className="create-order-select-promo-modal promo-code-wrapper">
                          <strong className="create-order-select-promo-modal promo-code">
                            {promo.code}
                          </strong>
                          <span className="create-order-select-promo-modal promo-badge">
                            {promo.discountType === "PERCENTAGE"
                              ? `-${promo.discountValue}%`
                              : `-${promo.discountValue.toLocaleString()}đ`}
                          </span>
                        </div>
                      </div>

                      <div className="create-order-select-promo-modal promo-details">
                        <span className="create-order-select-promo-modal promo-title">
                          {promo.title}
                        </span>

                        <div className="create-order-select-promo-modal promo-conditions">
                          {promo.maxDiscountAmount > 0 && (
                            <span className="create-order-select-promo-modal promo-condition">
                              <span className="create-order-select-promo-modal condition-label">
                                Giảm tối đa:
                              </span>
                              <span className="create-order-select-promo-modal condition-value">
                                {promo.maxDiscountAmount.toLocaleString()}đ
                              </span>
                            </span>
                          )}
                          {promo.minOrderValue > 0 && (
                            <span className="create-order-select-promo-modal promo-condition">
                              <span className="create-order-select-promo-modal condition-label">
                                Đơn tối thiểu:
                              </span>
                              <span className="create-order-select-promo-modal condition-value">
                                {promo.minOrderValue.toLocaleString()}đ
                              </span>
                            </span>
                          )}

                          {promo.endDate && (
                            <span className="create-order-select-promo-modal promo-expiry">
                              <span className="create-order-select-promo-modal expiry-label">
                                HSD:
                              </span>
                              <span className="create-order-select-promo-modal expiry-value">
                                {new Date(promo.endDate).toLocaleDateString("vi-VN")}
                              </span>
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </Radio>
                </List.Item>
              )}
            />
          </Radio.Group>
        </div>


        <div className="create-order-select-promo-modal pagination-container">
          <Pagination
            current={currentPage}
            pageSize={pageSize}
            total={total}
            onChange={onPageChange}
            showSizeChanger={false}
            showQuickJumper
            size="small"
            className="create-order-select-promo-modal pagination"
          />
        </div>
      </div>
    </Modal>
  );
};

export default SelectedPromoModal;