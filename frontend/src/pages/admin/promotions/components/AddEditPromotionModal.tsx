import React from "react";
import type {FormInstance} from "antd";
import {Col, DatePicker, Divider, Form, Input, InputNumber, Modal, Row, Select, Switch} from "antd";
import type {AdminServiceType} from "../../../../types/serviceType";
import type {AdminUser} from "../../../../types/user";
import type {Promotion} from "../../../../types/promotion";
import "../../../hr/recruitment/job-posting/components/JobPostingComponents.css";

interface Option {
  label: string;
  value: string;
}

interface AddEditPromotionModalProps {
  open: boolean;
  editing: Promotion | null;
  form: FormInstance;
  submitting: boolean;
  isGlobal: boolean;
  statusOptions: Option[];
  discountTypeOptions: Option[];
  serviceTypes: AdminServiceType[];
  users: AdminUser[];
  onCancel: () => void;
  onSubmit: () => void;
  onIsGlobalChange: (checked: boolean) => void;
  onValuesChange: () => void;
}

const AddEditPromotionModal: React.FC<AddEditPromotionModalProps> = ({
  open,
  editing,
  form,
  submitting,
  isGlobal,
  statusOptions,
  discountTypeOptions,
  serviceTypes,
  users,
  onCancel,
  onSubmit,
  onIsGlobalChange,
  onValuesChange,
}) => {
  return (
    <Modal
      className="modal-hide-scrollbar hr-job-posting-modal"
      width={980}
      bodyStyle={{ minHeight: 420 }}
      title={<div className="modal-title">{editing ? "Cập nhật khuyến mãi" : "Thêm khuyến mãi"}</div>}
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText={editing ? "Cập nhật" : "Tạo mới"}
      confirmLoading={submitting}
      destroyOnClose
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
    >
      <Form className="hr-job-posting-form" form={form} layout="vertical" onValuesChange={onValuesChange}>
        <div className="modal-section">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="code" label={<span className="modal-lable">Mã khuyến mãi</span>} rules={[{ required: true, message: "Vui lòng nhập mã" }]}>
                <Input className="modal-input" placeholder="VD: SALE20" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="title" label={<span className="modal-lable">Tiêu đề</span>}>
                <Input className="modal-input" placeholder="Tiêu đề khuyến mãi" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="description" label={<span className="modal-lable">Mô tả</span>} style={{ gridColumn: "1 / 3" }}>
            <Input.TextArea className="modal-input modal-textarea" autoSize={{ minRows: 2, maxRows: 5 }} placeholder="Mô tả (tùy chọn)" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="discountType" label={<span className="modal-lable">Loại giảm giá</span>} rules={[{ required: true }]}> 
                <Select className="modal-custom-select">
                  {discountTypeOptions.map((option) => (
                    <Select.Option key={option.value} value={option.value}>
                      {option.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="discountValue" label={<span className="modal-lable">Giá trị giảm</span>} rules={[{ required: true }]}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={0.01} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="maxDiscountAmount" label={<span className="modal-lable">Giảm tối đa (đ)</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={0} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="startDate" label={<span className="modal-lable">Ngày bắt đầu</span>} rules={[{ required: true }]}> 
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="endDate" label={<span className="modal-lable">Ngày kết thúc</span>} rules={[{ required: true }]}> 
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">Loại khuyến mãi</Divider>

          <Form.Item name="isGlobal" label={<span className="modal-lable">Khuyến mãi chung</span>} valuePropName="checked">
            <Switch
              checked={isGlobal}
              onChange={(checked) => {
                onIsGlobalChange(checked);
                form.setFieldsValue({ isGlobal: checked, userIds: undefined });
              }}
            />
          </Form.Item>

          {!isGlobal && (
            <Form.Item name="userIds" label={<span className="modal-lable">Chọn user</span>}>
              <Select mode="multiple" className="modal-custom-select" placeholder="Chọn user">
                {users.map((user) => (
                  <Select.Option key={user.id} value={user.id}>
                    {user.firstName} {user.lastName} ({user.email})
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          )}

          <Divider orientation="left">Điều kiện áp dụng</Divider>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="minOrderValue" label={<span className="modal-lable">Đơn tối thiểu (đ)</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={0} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="minWeight" label={<span className="modal-lable">Trọng lượng từ (kg)</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={0} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="maxWeight" label={<span className="modal-lable">Trọng lượng đến (kg)</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={0} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="minOrdersCount" label={<span className="modal-lable">Số đơn tối thiểu</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={0} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="serviceTypeIds" label={<span className="modal-lable">Loại dịch vụ</span>}>
                <Select mode="multiple" className="modal-custom-select" placeholder="Chọn loại dịch vụ">
                  {serviceTypes.map((serviceType) => (
                    <Select.Option key={serviceType.id} value={serviceType.id}>
                      {serviceType.name}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="firstTimeUser" label={<span className="modal-lable">Chỉ người dùng mới</span>} valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">Giới hạn sử dụng</Divider>

          <Row gutter={16}>
            <Col span={6}>
              <Form.Item name="usageLimit" label={<span className="modal-lable">Tổng giới hạn</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={1} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="maxUsagePerUser" label={<span className="modal-lable">Mỗi user</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={1} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="dailyUsageLimitGlobal" label={<span className="modal-lable">Giới hạn/ngày</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={1} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="dailyUsageLimitPerUser" label={<span className="modal-lable">Giới hạn/ngày/user</span>}>
                <InputNumber className="modal-input" style={{ width: "100%" }} min={1} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="status" label={<span className="modal-lable">Trạng thái</span>} initialValue="ACTIVE">
            <Select className="modal-custom-select">
              {statusOptions.map((option) => (
                <Select.Option key={option.value} value={option.value}>
                  {option.label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </div>
      </Form>
    </Modal>
  );
};

export default AddEditPromotionModal;
