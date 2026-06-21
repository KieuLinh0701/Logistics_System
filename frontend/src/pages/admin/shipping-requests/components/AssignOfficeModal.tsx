import React from "react";
import {Button, Modal, Select, Space} from "antd";
import type {OfficeOption} from "../../../../types/shippingRequest";

import "../../../hr/recruitment/job-posting/components/JobPostingComponents.css";

interface AssignOfficeModalProps {
  open: boolean;
  offices: OfficeOption[];
  selectedOfficeId?: number;
  submitting: boolean;
  onChangeOffice: (officeId?: number) => void;
  onSubmit: () => void;
  onCancel: () => void;
}

const AssignOfficeModal: React.FC<AssignOfficeModalProps> = ({
  open,
  offices,
  selectedOfficeId,
  submitting,
  onChangeOffice,
  onSubmit,
  onCancel,
}) => {
  return (
    <Modal
      open={open}
      centered
      footer={null} // ❗ bỏ footer mặc định
      onCancel={onCancel}
      title={
        <div
          className="modal-title"
          style={{
            textAlign: "center",
            width: "100%",
          }}
        >
          Phân công cho bưu cục
        </div>
      }
    >
      {/* BODY */}
      <div
        className="hr-job-posting-form"
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 20,
        }}
      >
        {/* LABEL */}
        <div>
          <div className="modal-lable" style={{ marginBottom: 6 }}>
            Bưu cục
          </div>

          <Select
            showSearch
            optionFilterProp="label"
            className="modal-custom-select"
            style={{ width: "100%" }}
            placeholder="Chọn bưu cục"
            value={selectedOfficeId}
            allowClear
            onChange={(value) => onChangeOffice(value)}
            options={offices.map((office) => ({
              value: office.id,
              label: office.name,
            }))}
          />
        </div>

        {/* FOOTER CUSTOM */}
        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            marginTop: 12,
            paddingTop: 16,
            borderTop: "1px solid #f0f0f0",
          }}
        >
          <Space size={12}>
            <Button onClick={onCancel}>Hủy</Button>

            <Button
              type="primary"
              loading={submitting}
              disabled={!selectedOfficeId}
              onClick={onSubmit}
            >
              Phân công
            </Button>
          </Space>
        </div>
      </div>
    </Modal>
  );
};

export default AssignOfficeModal;