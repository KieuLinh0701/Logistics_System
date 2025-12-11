import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Upload, message, Descriptions, Typography, Tooltip } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { RcFile, UploadFile } from 'antd/es/upload/interface';
import type { ShippingRequest } from '../../../../../types/shippingRequest';
import {
  translateShippingRequestType,
  translateShippingRequestStatus,
  getAllowedManagerStatuses
} from '../../../../../utils/shippingRequestUtils';
import shippingRequestApi from '../../../../../api/shippingRequestApi';
import { useNavigate } from 'react-router-dom';

const { Text } = Typography;

interface ProcessingModalProps {
  open: boolean;
  request: Partial<ShippingRequest>;
  onSuccess: () => void;
  onCancel: () => void;
}

type MyUploadFile = UploadFile & {
  isPdf?: boolean;
};

const ProcessingModal: React.FC<ProcessingModalProps> = ({
  open,
  request,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fileList, setFileList] = useState<MyUploadFile[]>([]);
  const [responseFileList, setResponseFileList] = useState<MyUploadFile[]>([]);
  const navigate = useNavigate();

  const address = request.contactDetail || 'N/A';

  useEffect(() => {
    if (!open) return;

    form.resetFields();

    // file người gửi (chỉ xem)
    const requestFiles: MyUploadFile[] =
      request.requestAttachments?.map(att => {
        const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(att.fileName);
        return {
          uid: att.id?.toString() || att.fileName,
          name: att.fileName,
          status: 'done',
          url: att.url,
          preview: isImage ? att.url : undefined,
          thumbUrl: isImage ? att.url : getFileIcon(att.fileName),
        };
      }) ?? [];
    setFileList(requestFiles);

    // file phản hồi (có thể xóa/upload)
    const responseFiles: MyUploadFile[] =
      request.responseAttachments?.map(att => {
        const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(att.fileName);
        return {
          uid: att.id?.toString() || att.fileName,
          name: att.fileName,
          status: 'done',
          url: att.url,
          preview: isImage ? att.url : undefined,
          thumbUrl: isImage ? att.url : getFileIcon(att.fileName),
        };
      }) ?? [];
    setResponseFileList(responseFiles);

    form.setFieldsValue({ managerNote: request.response });
  }, [open, request, form]);

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

  const handleRemoveResponseFile = (file: MyUploadFile) => {
    setResponseFileList(prev => prev.filter(f => f.uid !== file.uid));
  };

  const handleResponseChange = ({ fileList: newFileList }: { fileList: MyUploadFile[] }) => {
    const updatedList: MyUploadFile[] = newFileList.map(f => {
      if (f.url) return f;
      if (f.originFileObj && f.type?.startsWith('image/')) {
        return { ...f, preview: URL.createObjectURL(f.originFileObj) };
      }
      return { ...f, thumbUrl: getFileIcon(f.name || '') };
    });
    setResponseFileList(updatedList);
  };

  const handlePreview = async (file: UploadFile) => {
    if (!file.url && !file.originFileObj) return;

    const name = file.name?.toLowerCase() || '';
    let src = file.url;
    if (!src && file.originFileObj) {
      src = URL.createObjectURL(file.originFileObj as RcFile);
    }
    if (!src) return;

    if (name.match(/\.(pdf|doc|docx|xls|xlsx)$/)) {
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
      payload.append("status", values.status);
      payload.append("response", values.managerNote || "");

      // file mới upload
      responseFileList.forEach(file => {
        if (file.originFileObj) payload.append("attachments", file.originFileObj as RcFile);
      });

      // id file cũ còn giữ
      const oldFileIds = responseFileList.filter(f => !f.originFileObj).map(f => f.uid);
      payload.append("oldAttachments", JSON.stringify(oldFileIds));

      await handelManagerProcessRequest(payload);
    } catch (err: any) {
      console.error(err);
      message.error(err?.response?.data?.message || "Có lỗi xảy ra!");
      setLoading(false);
    }
  };

  const handelManagerProcessRequest = async (payload: FormData) => {
    setLoading(true);
    try {
      const result = await shippingRequestApi.processingManagerShippingRequest(request.id!, payload);
      if (result.success) {
        message.success("Phản hồi yêu cầu thành công!");
        form.resetFields();
        setResponseFileList([]); // reset file phản hồi
        onSuccess();
        onCancel();
      } else {
        message.error(result.message || "Có lỗi khi phản hồi yêu cầu");
      }
    } catch (error) {
      message.error("Có lỗi khi phản hồi yêu cầu");
    } finally {
      setLoading(false);
    }
  };

  const handleViewOrder = () => {
    if (!request.orderTrackingNumber) return;
    navigate(`/orders/tracking/${request.orderTrackingNumber}`);
  };

  return (
    <Modal
      open={open}
      onOk={handleSubmit}
      confirmLoading={loading}
      className="modal-hide-scrollbar"
      okButtonProps={{ className: "modal-ok-button", loading }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      onCancel={onCancel}
      okText="Cập nhật xử lý"
      title={<span className='modal-title'>Xử lý yêu cầu #{request.code}</span>}
      zIndex={1100}
      getContainer={false}
      forceRender
      width={650}
    >
      <Form form={form} layout="vertical">

        {/* THÔNG TIN YÊU CẦU */}
        <Descriptions bordered column={1} size="middle" >

          <Descriptions.Item label="Thời gian yêu cầu">
            <Text>
              {request.createdAt
                ? new Date(request.createdAt).toLocaleString('vi-VN')
                : 'N/A'}
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Loại yêu cầu">
            {request.requestType
              ? translateShippingRequestType(request.requestType)
              : 'N/A'}
          </Descriptions.Item>

          <Descriptions.Item label="Trạng thái">
            {request.status
              ? translateShippingRequestStatus(request.status)
              : 'N/A'}
          </Descriptions.Item>

          <Descriptions.Item label="Người yêu cầu">
            <div>
              <div className="custom-table-content-strong">
                {(request.contactName || 'N/A')} -{" "}
                {(request.userCode ? request.userCode : ' Khách vãng lai')}
              </div>

              <div>
                {(request.contactEmail || 'N/A')} -{" "}
                {(request.contactPhoneNumber || 'N/A')}
              </div>

              <div>
                {address}
              </div>
            </div>
          </Descriptions.Item>

          {request.orderTrackingNumber && (
            <Descriptions.Item label="Đơn hàng liên quan">
              <div onClick={handleViewOrder}>
                <Tooltip title="Click để xem chi tiết đơn hàng">
                  <span className="navigate-link">
                    {request.orderTrackingNumber}
                  </span>
                </Tooltip>
              </div>
            </Descriptions.Item>
          )}

          {request.requestContent?.trim() && (
            <Descriptions.Item label="Nội dung yêu cầu">
              <div className='shipping-request-detail-modal-request-background'>
                {request.requestContent}
              </div>
            </Descriptions.Item>
          )}

          {fileList.length > 0 && (
            <Descriptions.Item label="File người gửi đính kèm">
              <Upload
                listType="picture-card"
                fileList={fileList}
                showUploadList={{ showRemoveIcon: false }}
                onPreview={handlePreview}
              />
            </Descriptions.Item>
          )}
        </Descriptions>

        {/* PHẢN HỒI CỦA QUẢN LÝ */}
        <Form.Item
          label={<span className="modal-lable">Trạng thái xử lý</span>}
          name="status"
          rules={[{ required: true, message: "Chọn trạng thái!" }]}
        >
          <Select
            className="modal-custom-select"
            placeholder="Chọn trạng thái xử lý..."
          >
            {getAllowedManagerStatuses(request.status).map(t => (
              <Select.Option key={t} value={t}>{translateShippingRequestStatus(t)}</Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Nội dung phản hồi</span>}
          name="managerNote"
          rules={[{ max: 1000, message: "Tối đa 1000 ký tự!" }]}
        >
          <Input.TextArea
            className="modal-custom-input-textarea"
            rows={4}
            placeholder="Nhập nội dung phản hồi..."
          />
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">File phản hồi (nếu có)</span>}
        >
          <div className="modal-image-upload-container">
            <Upload
              listType="picture-card"
              fileList={responseFileList}
              beforeUpload={beforeUpload}
              onRemove={handleRemoveResponseFile}
              onPreview={handlePreview}
              onChange={handleResponseChange}
              accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"
              multiple
            >
              <div className="modal-upload-button-content">
                <UploadOutlined />
                <div className='modal-upload-button-lable'>Tải file phản hồi</div>
              </div>
            </Upload>
          </div>
        </Form.Item>

      </Form>
    </Modal>
  );
};

export default ProcessingModal;