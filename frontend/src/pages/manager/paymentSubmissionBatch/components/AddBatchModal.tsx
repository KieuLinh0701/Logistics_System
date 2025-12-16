import React, { useEffect, useState } from 'react';
import { Modal, Form, Select, InputNumber, message, Button } from 'antd';
import type { ManagerEmployee, ManagerEmployeeSearchRequest } from '../../../../types/employee';
import employeeApi from '../../../../api/employeeApi';
import paymentSubmissionBatchApi from '../../../../api/paymentSubmissionBatchApi';
import { DeleteOutlined, UserOutlined } from '@ant-design/icons';
import SelectEmployeeModal from './SelectEmployeeModal'; // import modal chọn nhân viên

interface AddBatchModalProps {
  open: boolean;
  onSuccess: () => void;
  onCancel: () => void;
}

const AddBatchModal: React.FC<AddBatchModalProps> = ({
  open,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [searchText, setSearchText] = useState("");
  const [loading, setLoading] = useState(false);
  const [employees, setEmployees] = useState<ManagerEmployee[]>([]);
  const [selectedEmployee, setSelectedEmployee] = useState<ManagerEmployee | null>(null);
  const [employeeModalOpen, setEmployeeModalOpen] = useState(false);

  useEffect(() => {
    if (open) {
      form.resetFields();
      fetchEmployees();
    }
  }, [open]);

  const fetchEmployees = async () => {
    try {
      setLoading(true);
      const param: ManagerEmployeeSearchRequest = {
        page: currentPage,
        limit: pageSize,
        search: searchText || undefined,
      };

      const result = await employeeApi.getManagerActiveShippers(param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setEmployees(list);
        setTotal(result.data.pagination?.total || 0);
        setCurrentPage(1);
      } else {
        message.error(result.message || "Lấy danh sách nhân viên thất bại");
      }
    } catch (error: any) {
      message.error('Có lỗi xảy ra: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!selectedEmployee) {
      message.error("Vui lòng chọn nhân viên giao hàng!");
      return;
    }

    try {
      setLoading(true);
      const values = await form.validateFields();
      const result = await paymentSubmissionBatchApi.createManagerPaymentSubmissionBatch({
        shipperId: selectedEmployee?.id,
        totalActualAmount: values.totalAmount,
      });

      if (result.success) {
        message.success(result.message || "Tạo phiên đối soát thành công");
        onSuccess();
        onCancel();
      } else {
        message.error(result.message || "Tạo phiên đối soát thất bại");
      }
    } catch (error: any) {
      message.error('Có lỗi xảy ra: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectEmployee = (employee: ManagerEmployee) => {
    setSelectedEmployee(employee);
    setEmployeeModalOpen(false);
  };

  return (
    <>
      <Modal
        open={open}
        title={<span className='modal-title'>Tạo phiên đối soát mới</span>}
        onOk={handleSubmit}
        onCancel={onCancel}
        okButtonProps={{ className: "modal-ok-button", loading }}
        cancelButtonProps={{ className: "modal-cancel-button" }}
        className="modal-hide-scrollbar"
        okText="Tạo mới"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label={<span className="modal-lable">Nhân viên giao hàng</span>}
            name="selectedEmployee"
            rules={[
              {
                validator: () => {
                  if (selectedEmployee) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error("Chọn nhân viên giao hàng!"));
                },
              },
            ]}
          >
            <div className='manager-batchs-contain-select'>
              <Button
                icon={<UserOutlined />}
                className="modal-cancel-button"
                onClick={() => setEmployeeModalOpen(true)}
              >
                Chọn nhân viên
              </Button>

              {selectedEmployee && (
                <div className="shipment-add-edit-selected-office" style={{ flex: 1 }}>
                  <div className="shipment-add-edit-select-contain">
                    <div className="shipment-add-edit-selected-header">
                      <span className="shipment-add-edit-select-name">
                        {selectedEmployee.lastName} {selectedEmployee.firstName}
                      </span>
                      <Button
                        type="text"
                        danger
                        size="small"
                        className="shipment-add-edit-remove-btn"
                        onClick={() => setSelectedEmployee(null)}
                        icon={<DeleteOutlined />}
                      />
                    </div>
                    <span>Số điện thoại: {selectedEmployee.phoneNumber || "Unknown"}</span>
                    <span>Email: {selectedEmployee.email || "Unknown"}</span>
                  </div>
                </div>
              )}
            </div>
          </Form.Item>

          <Form.Item
            label={<span className="modal-lable">Tổng tiền thu</span>}
            name="totalAmount"
            rules={[{ required: true, message: 'Nhập tổng tiền thu!' }]}
          >
            <InputNumber
              className="modal-custom-input-number"
              min={0}
              formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={value => value?.replace(/\$\s?|(,*)/g, '') as any}
            />
          </Form.Item>
        </Form>
      </Modal>

      <SelectEmployeeModal
        open={employeeModalOpen}
        employees={employees}
        page={currentPage}
        limit={pageSize}
        total={total}
        selectedEmployee={selectedEmployee}
        onClose={() => setEmployeeModalOpen(false)}
        onSearch={() => { }}
        onSelectEmployee={handleSelectEmployee}
        onPageChange={() => { }}
      />
    </>
  );
};

export default AddBatchModal;