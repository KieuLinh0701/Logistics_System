import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Select, Upload, type UploadProps, type UploadFile, Tooltip } from 'antd';
import type { UserProductForm } from '../../../../types/product';
import { PRODUCT_STATUS, PRODUCT_TYPES, translateProductStatus, translateProductType } from '../../../../utils/productUtils';
import { InfoCircleOutlined, UploadOutlined } from '@ant-design/icons';

interface AddEditModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  product: Partial<UserProductForm>;
  loading: boolean;
  onOk: () => void;
  onCancel: () => void;
  onProductChange: (product: Partial<UserProductForm>) => void;
  form: any;
}

const AddEditModal: React.FC<AddEditModalProps> = ({
  open,
  mode,
  product,
  loading,
  onOk,
  onCancel,
  onProductChange,
  form,
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  useEffect(() => {
    if (!open) {
      setFileList([]);
    } else if (product.image) {
      setFileList([
        {
          uid: '-1',
          name: 'image.png',
          status: 'done',
          url: product.image,
        },
      ]);
    } else {
      setFileList([]);
    }
  }, [open, product.image]);

  // Xử lý khi chọn file
  const handleFileChange: UploadProps['onChange'] = ({ fileList: newFileList }) => {
    const limitedFileList = newFileList.slice(-1);
    setFileList(limitedFileList);

    if (limitedFileList.length > 0 && limitedFileList[0].originFileObj) {
      const file = limitedFileList[0].originFileObj;

      onProductChange({ imageFile: file });

      const imageUrl = URL.createObjectURL(file);
      onProductChange({ image: imageUrl });
    }
  };

  // Xóa ảnh
  const handleRemove = () => {
    setFileList([]);
    onProductChange({ image: undefined, imageFile: undefined });
  };

  // Xem trước ảnh
  const handlePreview = async (file: UploadFile) => {
    if (!file.url && !file.preview) {
      file.preview = await getBase64(file.originFileObj as File);
    }

    const imageUrl = file.url || file.preview;
    if (imageUrl) {
      const newWindow = window.open();
      if (newWindow) {
        newWindow.document.write(`
          <html>
            <head><title>Xem trước ảnh</title></head>
            <body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;">
              <img src="${imageUrl}" style="max-width:90%;max-height:90%;object-fit:contain;" />
            </body>
          </html>
        `);
      }
    }
  };

  const customUpload: UploadProps['customRequest'] = (options) => {
    const { onSuccess } = options;
    setTimeout(() => {
      onSuccess?.('ok');
    }, 0);
  };

  // Hàm chuyển file sang base64 để preview
  const getBase64 = (file: File): Promise<string> =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = error => reject(error);
    });

  return (
    <Modal
      title={
        <span className='modal-title'>
          {mode === 'edit'
            ? `Chỉnh sửa sản phẩm`
            : 'Thêm sản phẩm mới'}
        </span>

      }
      open={open}
      onOk={onOk}
      onCancel={onCancel}
      okText={mode === 'edit' ? 'Cập nhật' : 'Thêm'}
      okButtonProps={{
        className: "modal-ok-button",
        loading: loading
      }}
      cancelButtonProps={{
        className: "modal-cancel-button"
      }}
      cancelText="Hủy"
      className="modal-hide-scrollbar"

    >
      <Form form={form} layout="vertical">
        <Form.Item
          label={<span className="modal-lable">Ảnh sản phẩm</span>}
        >
          <div className="modal-image-upload-container">
            <Upload
              listType="picture-card"
              fileList={fileList}
              onChange={handleFileChange}
              onPreview={handlePreview}
              onRemove={handleRemove}
              customRequest={customUpload}
              beforeUpload={() => false}
              maxCount={1}
              accept="image/*"
            >
              {fileList.length >= 1 ? null : (
                <div className="modal-upload-button-content">
                  <UploadOutlined />
                  <div className='modal-upload-button-lable'>Tải ảnh lên</div>
                </div>
              )}
            </Upload>

            <div className="modal-upload-notes">
              <div className="note-item">• Kích thước: 500 x 500 px</div>
              <div className="note-item">• Định dạng: JPG, PNG, JPEG</div>
              <div className="note-item">• Dung lượng: ≤ 2MB</div>
            </div>
          </div>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Tên sản phẩm</span>}
          name="name"
          rules={[{ required: true, message: 'Nhập tên sản phẩm!' }]}>
          <Input
            className="modal-custom-input"
            placeholder="Nhập tên sản phẩm"
            onChange={(e) => onProductChange({ ...product, name: e.target.value })}
          />
        </Form.Item>

        <Form.Item
          label={
            <span className="modal-label">
              Khối lượng quy đổi{" "}
              <Tooltip
                title={
                  "Khối lượng quy đổi = (Dài × Rộng × Cao) / 5000. So sánh với khối lượng thực tế và lấy giá trị lớn hơn để tính phí vận chuyển."
                }
              >
                <InfoCircleOutlined />
              </Tooltip>
            </span>
          }
          name="weight"
          rules={[{ required: true, message: 'Nhập trọng lượng sản phẩm!' }]}>
          <InputNumber
            className="modal-custom-input-number"
            min={0}
            placeholder="Nhập khối lượng sản phẩm"
            style={{ width: '100%' }}
            onChange={(val) => onProductChange({ ...product, weight: val ?? 0 })}
          />
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Giá sản phẩm (VNĐ)</span>}
          name="price"
          rules={[{ required: true, message: 'Nhập giá sản phẩm!' }]}>
          <InputNumber
            className="modal-custom-input-number"
            min={0}
            placeholder="Nhập giá sản phẩm"
            onChange={(val) => onProductChange({ ...product, price: val ?? 0 })}
          />
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Tồn kho</span>}
          name="stock">
          <InputNumber
            className="modal-custom-input-number"
            min={0}
            placeholder="Nhập số lượng tồn kho"
            defaultValue={0}
            onChange={(val) => onProductChange({ ...product, stock: val ?? 0 })}
          />
        </Form.Item>

        <Form.Item
          name="type"
          label={<span className="modal-lable">Loại</span>}
          rules={[{ required: true, message: 'Chọn loại sản phẩm!' }]}>
          <Select
            className="modal-custom-select"
            onChange={(val) => onProductChange({ ...product, type: val })}
            placeholder="Chọn loại..."
          >
            {PRODUCT_TYPES.map((t) => (
              <Select.Option key={t} value={t}>
                {translateProductType(t)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Trạng thái</span>}
          name="status">
          <Select
            className="modal-custom-select"
            onChange={(val) => onProductChange({ ...product, status: val as any })}
            defaultValue="ACTIVE"
          >
            {PRODUCT_STATUS.map((s) => (
              <Select.Option key={s} value={s}>
                {translateProductStatus(s)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AddEditModal;