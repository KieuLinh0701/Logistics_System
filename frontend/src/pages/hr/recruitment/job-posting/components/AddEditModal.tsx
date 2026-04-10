import React from "react";
import { Form, Input, Modal, Select, InputNumber } from "antd";
import type { FormInstance } from "antd";
import type { JobPosting } from "../../../../../types/recruitment";
import { jobStatusOptions, roleTypeOptions } from "../../../../common/recruitment/recruitmentHelpers";
import { shiftOptions } from "../../../../../utils/recruitmentHelpers";
import "./JobPostingComponents.css";
import officeApi from "../../../../../api/officeApi";

interface OfficeOption {
  label: string;
  value: string;
}

interface AddEditModalProps {
  open: boolean;
  editingJob: JobPosting | null;
  actionLoading: boolean;
  form: FormInstance;
  officeOptions: OfficeOption[];
  onCancel: () => void;
  onSubmit: () => void;
  isManager?: boolean;
  managerOfficeId?: number | null;
}

const AddEditModal: React.FC<AddEditModalProps> = ({
  open,
  editingJob,
  actionLoading,
  form,
  officeOptions,
  onCancel,
  onSubmit,
  isManager,
  managerOfficeId,
}) => {
  React.useEffect(() => {
    if (open) {
      try {
        const fv = form.getFieldValue("officeId");

        // Nếu đang chỉnh sửa tin tuyển dụng, điền các giá trị vào form sau khi component mount
        if (editingJob) {
          setTimeout(() => {
            try {
              form.setFieldsValue({
                title: editingJob.title,
                description: editingJob.description,
                roleType: editingJob.roleType,
                officeId: editingJob.officeId !== undefined && editingJob.officeId !== null ? String(editingJob.officeId) : undefined,
                status: editingJob.status,
                quantityNeeded: editingJob.quantityNeeded ?? 1,
                shift: editingJob.shift ?? undefined,
              });
            } catch (e) {
              // Bỏ qua lỗi khi điền giá trị trễ
            }
          }, 0);
        }

        // Nếu người dùng là manager nhưng không có managerOfficeId, gọi API lấy bưu cục quản lý
        (async () => {
          try {
            if (isManager && !managerOfficeId) {
              const res: any = await officeApi.getManagerOffice();
              if (res && res.success && res.data && res.data.id) {
                setTimeout(() => {
                  form.setFieldsValue({ officeId: String(res.data.id) });
                }, 0);
              }
            } else if (isManager && managerOfficeId) {
              setTimeout(() => {
                form.setFieldsValue({ officeId: String(managerOfficeId) });
              }, 0);
            }
          } catch (e) {
            // Bỏ qua lỗi khi gọi API lấy bưu cục quản lý
          }
        })();
      } catch (e) {
        // ignore
      }
    }
  }, [open, isManager, managerOfficeId, form, editingJob]);
  return (
    <Modal
      className="modal-hide-scrollbar hr-job-posting-modal"
      width={940}
      bodyStyle={{ minHeight: 360 }}
      title={<div className="modal-title">{editingJob ? "Cập nhật tin tuyển dụng" : "Tạo tin tuyển dụng"}</div>}
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText={editingJob ? "Cập nhật" : "Tạo mới"}
      confirmLoading={actionLoading}
      destroyOnClose
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
    >
      <Form className="hr-job-posting-form" form={form} layout="vertical">
        <div className="modal-section">
          <div className="modal-grid">
            <Form.Item
              name="title"
              label={<span className="modal-lable">Tiêu đề</span>}
              rules={[{ required: true, message: "Vui lòng nhập tiêu đề" }]}
              className="form-item title-item"
              style={{ gridColumn: '1 / 2' }}
            >
              <Input className="modal-input" placeholder="Nhập tiêu đề..." />
            </Form.Item>

            <Form.Item
              name="officeId"
              label={<span className="modal-lable">Bưu Cục</span>}
              rules={[{ required: true, message: "Vui lòng chọn bưu cục" }]}
              className="form-item"
              style={{ gridColumn: '2 / 3' }}
            >
              {isManager ? (
                <div className="modal-input" style={{ paddingTop: 6 }}>
                  {(() => {
                    const val = form.getFieldValue("officeId");
                    const matched = officeOptions.find(o => String(o.value) === String(val));
                    return matched ? matched.label : "";
                  })()}
                </div>
              ) : (
                <Select
                  className="modal-custom-select"
                  showSearch
                  optionFilterProp="label"
                  options={officeOptions}
                  placeholder={form.getFieldValue("officeId") ? undefined : "Chọn bưu cục..."}
                  value={form.getFieldValue("officeId") ?? undefined}
                  onChange={(v) => form.setFieldsValue({ officeId: v })}
                />
              )}
            </Form.Item>

            <Form.Item
              name="description"
              label={<span className="modal-lable">Mô tả công việc</span>}
              rules={[{ required: true, message: "Vui lòng nhập mô tả" }]}
              className="form-item"
              style={{ gridColumn: '1 / 2' }}
            >
              <Input.TextArea className="modal-input modal-textarea" autoSize={{ minRows: 4, maxRows: 12 }} placeholder="Nhập mô tả công việc..." />
            </Form.Item>

            <div className="right-stack" style={{ gridColumn: '2 / 3' }}>
              <Form.Item
                name="quantityNeeded"
                label={<span className="modal-lable">Số lượng cần tuyển</span>}
                rules={[{ required: true, message: "Vui lòng nhập số lượng" }, { type: 'number', min: 1, message: 'Số lượng phải lớn hơn 0' }]}
                className="form-item"
              >
                <InputNumber className="modal-input" min={1} style={{ width: '100%' }} placeholder="Nhập số lượng" />
              </Form.Item>

              <Form.Item
                name="shift"
                label={<span className="modal-lable">Ca làm việc</span>}
                rules={[{ required: true, message: "Vui lòng chọn ca làm việc" }]}
                className="form-item"
              >
                <Select className="modal-custom-select" options={shiftOptions} placeholder="Chọn ca làm việc..." />
              </Form.Item>
            </div>

            <Form.Item
              name="roleType"
              label={<span className="modal-lable">Vị trí ứng tuyển</span>}
              rules={[{ required: true, message: "Vui lòng chọn vị trí" }]}
              className="form-item"
              style={{ gridColumn: '1 / 2' }}
            >
              <Select className="modal-custom-select" options={roleTypeOptions} placeholder="Chọn vị trí ứng tuyển..." />
            </Form.Item>

            <Form.Item name="status" label={<span className="modal-lable">Trạng thái</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: '2 / 3' }}>
              <Select className="modal-custom-select" options={jobStatusOptions} placeholder="Chọn trạng thái..." />
            </Form.Item>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default AddEditModal;
