import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Alert, Upload, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { RcFile, UploadFile } from 'antd/es/upload/interface';
import type { ShippingRequest } from '../../../../../types/shippingRequest';
import {
  canEmptyRequestContentShippingRequest,
  canEmptyTrackingNumberShippingRequest,
  getShippingRequestMessage,
  SHIPPING_REQUEST_TYPES,
  translateShippingRequestType
} from '../../../../../utils/shippingRequestUtils';
import shippingRequestApi from '../../../../../api/shippingRequestApi';

interface AddEditModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  request: Partial<ShippingRequest>;
  onSuccess: () => void;
  onCancel: () => void;
}

type MyUploadFile = UploadFile & {
  isPdf?: boolean;
};

const AddEditModal: React.FC<AddEditModalProps> = ({
  open,
  mode,
  request,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [selectedType, setSelectedType] = useState<string | undefined>(undefined);
  const [fileList, setFileList] = useState<MyUploadFile[]>([]);

  useEffect(() => {
    if (open) {
      form.resetFields();
      const type = mode === "edit" ? request.requestType : undefined;
      setSelectedType(type);

      if (mode === "edit" && request.requestAttachments) {
        const files: MyUploadFile[] = request.requestAttachments.map(att => {
          const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(att.fileName);

          return {
            uid: att.id?.toString() || att.fileName,
            name: att.fileName,
            status: 'done',
            url: att.url,
            preview: isImage ? att.url : undefined,
            thumbUrl: isImage ? att.url : getFileIcon(att.fileName),
          };
        });

        setFileList(files);
      }

      form.setFieldsValue({
        trackingNumber: request.orderTrackingNumber,
        requestContent: request.requestContent,
        ...(mode === 'edit' && { requestType: type }),
      });
    }
  }, [open, request, mode, form]);

  const getFileIcon = (fileName: string): string | undefined => {
    if (/\.(jpg|jpeg|png|gif|webp)$/i.test(fileName)) return undefined;
    if (/\.pdf$/i.test(fileName)) return 'https://cdn-icons-png.flaticon.com/512/337/337946.png';
    if (/\.docx?$/i.test(fileName)) return 'https://cdn-icons-png.flaticon.com/512/281/281760.png';
    if (/\.xlsx?$/i.test(fileName)) return 'https://cdn-icons-png.flaticon.com/512/732/732220.png';
    return 'https://cdn-icons-png.flaticon.com/512/1091/1091007.png';
  };

  const beforeUpload = (file: RcFile) => {
    const isImage = file.type.startsWith('image/');
    const isPdf = file.type === 'application/pdf';

    const isWord =
      file.type === 'application/msword' ||
      file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';

    const isExcel =
      file.type === 'application/vnd.ms-excel' ||
      file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

    if (!isImage && !isPdf && !isWord && !isExcel) {
      message.error('Chỉ cho phép ảnh, PDF, Word, Excel!');
      return Upload.LIST_IGNORE;
    }

    return false;
  };

  const handleChange = ({ fileList: newFileList }: { fileList: MyUploadFile[] }) => {
    const updatedList: MyUploadFile[] = newFileList.map(f => {
      if (f.url) return f;

      if (f.originFileObj && f.type?.startsWith('image/')) {
        return { ...f, preview: URL.createObjectURL(f.originFileObj) };
      }

      return { ...f, thumbUrl: getFileIcon(f.name || '') };
    });

    setFileList(updatedList);
  };

  const handleRemove = (file: MyUploadFile) => {
    setFileList(prev => prev.filter(f => f.uid !== file.uid));
  };

  const handlePreview = async (file: UploadFile) => {
    if (!file.url && !file.originFileObj) return;

    const name = file.name?.toLowerCase() || '';
    let src = file.url;

    if (!src && file.originFileObj) {
      src = URL.createObjectURL(file.originFileObj as RcFile);
    }

    if (!src) return;

    if (
      name.endsWith('.pdf') ||
      name.endsWith('.doc') ||
      name.endsWith('.docx') ||
      name.endsWith('.xls') ||
      name.endsWith('.xlsx')
    ) {
      window.open(src, '_blank');
      return;
    }

    const newWindow = window.open();
    if (!newWindow) return;

    newWindow.document.write(`
    <html>
      <head><title>Xem trước ảnh</title></head>
      <body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;">
        <img src="${src}" style="max-width:90%;max-height:90%;object-fit:contain;" />
      </body>
    </html>
  `);
  };

  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      const payload = new FormData();
      payload.append("requestType", values.requestType);
      payload.append("requestContent", values.requestContent || "");
      payload.append("trackingNumber", values.trackingNumber || "");

      fileList.forEach(file => {
        if (file.originFileObj) {
          payload.append("attachments", file.originFileObj as RcFile);
        }
      });

      const oldFileIds = fileList
        .filter(file => file.url && !file.originFileObj)
        .map(file => file.uid);
      payload.append("oldAttachments", JSON.stringify(oldFileIds));

      console.log("payload keys", Array.from(payload.keys()));


      if (mode === 'create') {
        handelCreateShippingRequest(payload);
      } else {
        handelEditShippingRequest(payload);
      }
    } catch (err: any) {
      console.error(err);
      message.error(err.response?.data?.message || "Có lỗi xảy ra!");
      setLoading(false);
    }
  };

  const handelCreateShippingRequest = async (payload: FormData) => {
    setLoading(true);
    await form.validateFields();
    try {
      const result = await shippingRequestApi.createUserShippingRequest(payload);
      if (result.success && result.data) {
        message.success("Tạo yêu cầu thành công!");
        form.resetFields();
        setFileList([]);
        onSuccess();
        onCancel();
      }
      else message.error(result.message || "Có lỗi khi cập nhật");

    } catch (error: any) {
      message.error(error.message || "Có lỗi khi cập nhật");
    } finally {
      setLoading(false);
    }
  };

  const handelEditShippingRequest = async (payload: FormData) => {
    setLoading(true);
    await form.validateFields();
    try {
      const result = await shippingRequestApi.updateUserShippingRequest(request.id!, payload);
      if (result.success) {
        message.success("Sửa yêu cầu thành công!");
        form.resetFields();
        setFileList([]);
        onSuccess();
        onCancel();
      }
      else message.error(result.message || "Có lỗi khi sửa yêu cầu");
      form.resetFields();
    } catch (error: any) {
      message.error(error.message || "Có lỗi khi sửa yêu cầu");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Modal
        open={open}
        onOk={handleSubmit}
        confirmLoading={loading}
        className="modal-hide-scrollbar"
        okButtonProps={{ className: "modal-ok-button", loading }}
        cancelButtonProps={{ className: "modal-cancel-button" }}
        onCancel={onCancel}
        okText={mode === "edit" ? "Cập nhật" : "Tạo mới"}
        title={<span className='modal-title'>{mode === 'edit' ? `Chỉnh sửa yêu cầu #${request.code}` : 'Tạo yêu cầu mới'}</span>}
        zIndex={1100}
        getContainer={false}
        forceRender
      >
        <Form form={form} layout="vertical">
          {selectedType && (
            <Alert
              className='shipping-request-alert'
              type="info"
              message={getShippingRequestMessage(selectedType)}
            />
          )}

          <Form.Item
            label={<span className="modal-lable">Loại yêu cầu</span>}
            name="requestType"
            rules={[{ required: true, message: "Chọn loại yêu cầu!" }]}
          >
            <Select
              placeholder="Chọn loại yêu cầu..."
              className="modal-custom-select"
              disabled={mode === "edit"}
              onChange={setSelectedType}>
              {SHIPPING_REQUEST_TYPES.map(t => (
                <Select.Option key={t} value={t}>{translateShippingRequestType(t)}</Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label={<span className="modal-lable">Mã đơn hàng</span>}
            name="trackingNumber"
            rules={[
              ({ getFieldValue }) => ({
                required: getFieldValue("requestType") && !canEmptyTrackingNumberShippingRequest(getFieldValue("requestType")),
                message: "Nhập mã đơn hàng!",
              }),
            ]}
          >
            <Input
              className="modal-custom-input"
              placeholder="Nhập mã đơn hàng..."
              disabled={mode === "edit"} />
          </Form.Item>

          <Form.Item
            label={<span className="modal-lable">Nội dung yêu cầu</span>}
            name="requestContent"
            rules={[
              ({ getFieldValue }) => ({
                required: getFieldValue("requestType") && !canEmptyRequestContentShippingRequest(getFieldValue("requestType")),
                message: "Nhập nội dung yêu cầu!",
              }),
              { max: 1000, message: "Nội dung tối đa 1000 ký tự!" },
            ]}
            getValueFromEvent={(e) => {
              const value = e.target.value;
              return value === "" ? null : value;
            }}
          >
            <Input.TextArea
              className="modal-custom-input-textarea"
              rows={4}
              placeholder="Nhập nội dung..." />
          </Form.Item>

          <Form.Item
            label={<span className="modal-lable">Đính kèm ảnh minh chứng (nếu có)</span>}>
            <div className="modal-image-upload-container">
              <Upload
                listType="picture-card"
                fileList={fileList}
                beforeUpload={beforeUpload}
                onRemove={handleRemove}
                onPreview={handlePreview}
                onChange={handleChange}
                accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"
                multiple
              >
                <div className="modal-upload-button-content">
                  <UploadOutlined />
                  <div className='modal-upload-button-lable'>Tải file (ảnh / PDF / Word/ Excel)</div>
                </div>
              </Upload>
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default AddEditModal;