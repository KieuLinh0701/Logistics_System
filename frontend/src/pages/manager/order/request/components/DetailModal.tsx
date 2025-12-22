import type { UploadFile } from 'antd/es/upload/interface';
import { Modal, Descriptions, Button, Typography, Space, Tooltip, Upload } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import type { ShippingRequest } from '../../../../../types/shippingRequest';
import { translateShippingRequestStatus, translateShippingRequestType } from '../../../../../utils/shippingRequestUtils';
import { useEffect, useState } from 'react';
import { formatAddress } from '../../../../../utils/locationUtils';

const { Text } = Typography;

interface DetailModalProps {
    open: boolean;
    request: ShippingRequest | null;
    loading: boolean;
    onClose: () => void;
    onEdit: (request: ShippingRequest) => void;
    onViewOrderDetail?: (trackingNumber: string) => void;
}

const DetailModal: React.FC<DetailModalProps> = ({
    open,
    request,
    loading,
    onClose,
    onEdit,
    onViewOrderDetail
}) => {
    const [address, setAddress] = useState<string>('');

    useEffect(() => {
        if (!request) {
            setAddress('');
            return;
        }

        const loadAddress = async () => {
            try {
                const full = await formatAddress(
                    request.contactDetail || '',
                    request.contactWardCode,
                    request.contactCityCode
                );
                setAddress(full);
            } catch {
                setAddress(request.contactDetail || '');
            }
        };

        loadAddress();
    }, [request]);

    if (!request) return null;

    const handleViewOrder = () => {
        if (request.orderTrackingNumber) {
            onViewOrderDetail?.(request.orderTrackingNumber);
        }
    };

    const handleEdit = () => {
        onEdit(request);
    };

    const getFileIcon = (fileName: string): string | undefined => {
        if (/\.(jpg|jpeg|png|gif|webp)$/i.test(fileName)) return undefined;
        if (/\.pdf$/i.test(fileName)) return 'https://cdn-icons-png.flaticon.com/512/337/337946.png';
        if (/\.docx?$/i.test(fileName)) return 'https://cdn-icons-png.flaticon.com/512/281/281760.png';
        if (/\.xlsx?$/i.test(fileName)) return 'https://cdn-icons-png.flaticon.com/512/732/732220.png';
        return 'https://cdn-icons-png.flaticon.com/512/1091/1091007.png';
    };

    const handlePreview = (file: UploadFile) => {
        if (!file.url) return;

        const name = file.name?.toLowerCase() || '';

        const isPdf = name.endsWith('.pdf');
        const isWord = name.endsWith('.doc') || name.endsWith('.docx');
        const isExcel = name.endsWith('.xls') || name.endsWith('.xlsx');

        if (isPdf || isWord || isExcel) {
            window.open(file.url, '_blank');
            return;
        }

        const newWindow = window.open();
        if (!newWindow) return;

        newWindow.document.write(`
    <html>
      <head><title>Xem trước ảnh</title></head>
      <body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;">
        <img src="${file.url}" style="max-width:90%;max-height:90%;object-fit:contain;" />
      </body>
    </html>
  `);
    };

    const requestFiles: UploadFile[] = (request.requestAttachments || []).map(att => {
        const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(att.fileName);

        return {
            uid: att.id?.toString() || att.fileName,
            name: att.fileName,
            status: 'done',
            url: att.url,
            thumbUrl: isImage ? att.url : getFileIcon(att.fileName)
        };
    });

    const responseFiles: UploadFile[] = (request.responseAttachments || []).map(att => {
        const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(att.fileName);

        return {
            uid: att.id?.toString() || att.fileName,
            name: att.fileName,
            status: 'done',
            url: att.url,
            thumbUrl: isImage ? att.url : getFileIcon(att.fileName)
        };
    });

    return (
        <Modal
            title={
                <span className="modal-title">
                    Chi tiết yêu cầu {" "}
                    <span className="custom-table-content-strong">
                        #{request.code}
                    </span>
                </span>
            }
            loading={loading}
            open={open}
            onCancel={onClose}
            className="modal-hide-scrollbar"
            footer={[
                <Space key={`space-${request.id}`}>
                    {(request.status === 'PENDING' || request.status === 'PROCESSING') && (
                        <Button
                            key={`handleRequest-${request.id}`}
                            type="primary"
                            icon={<EditOutlined />}
                            onClick={handleEdit}
                            className='modal-ok-button'
                        >
                            {request.status === 'PENDING'
                                ? 'Xử lý yêu cầu'
                                : 'Cập nhật trạng thái'}
                        </Button>
                    )}
                </Space>
            ].filter(Boolean) as React.ReactNode[]}
            width={900}
            centered
            zIndex={1000}
            getContainer={false}
        >
            <Descriptions bordered column={1} size="middle" >
                <Descriptions.Item label="Thời gian yêu cầu">
                    <Text>{new Date(request.createdAt).toLocaleString('vi-VN')}</Text>
                </Descriptions.Item>

                {request.responseAt &&
                    <Descriptions.Item label="Thời gian phản hồi">
                        <Text>{new Date(request.responseAt).toLocaleString('vi-VN')}</Text>
                    </Descriptions.Item>
                }

                <Descriptions.Item label="Loại yêu cầu">
                    {translateShippingRequestType(request.requestType)}
                </Descriptions.Item>

                <Descriptions.Item label="Trạng thái">
                    {translateShippingRequestStatus(request.status)}
                </Descriptions.Item>

                <Descriptions.Item label="Người yêu cầu">
                    <div>
                        <div className="custom-table-content-strong">
                            {(request.contactName || 'N/A')} - {" "}
                            {(request.userCode ? request.userCode : ' Khách vãng lai')}
                        </div>

                        <div>
                            {(request.contactEmail || 'N/A')} - {" "}
                            {(request.contactPhoneNumber || 'N/A')}
                        </div>

                        <div>
                            {address || 'N/A'}
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

                {request.requestContent?.trim() ? (
                    <Descriptions.Item label="Nội dung yêu cầu">
                        <div className='shipping-request-detail-modal-request-background'>
                            {request.requestContent}
                        </div>
                    </Descriptions.Item>
                ) : null}

                {request.requestAttachments?.length > 0 && (
                    <Descriptions.Item label="File người gửi đính kèm">
                        <Upload
                            listType="picture-card"
                            fileList={requestFiles}
                            showUploadList={{ showRemoveIcon: false }}
                            onPreview={handlePreview}
                        />
                    </Descriptions.Item>
                )}

                 {request.handlerName && (
                    <Descriptions.Item label="Người phản hồi">
                        <span className="custom-table-content-strong">
                            {request.handlerName}
                        </span><br />
                        <span>
                            {request.handlerPhone}
                        </span><br />
                        <span>
                            {request.handlerEmail}
                        </span>
                    </Descriptions.Item>
                )}

                <Descriptions.Item label="Phản hồi">
                    <div
                        className='shipping-request-detail-modal-response-background'
                    >
                        {request.response && request.response.trim() !== ''
                            ? request.response
                            : <i>Chưa phản hồi</i>}
                    </div>
                </Descriptions.Item>

                {request.responseAttachments?.length > 0 && (
                    <Descriptions.Item label="File đính kèm từ bộ phận xử lý">
                        <Upload
                            listType="picture-card"
                            fileList={responseFiles}
                            showUploadList={{ showRemoveIcon: false }}
                            onPreview={handlePreview}
                        />
                    </Descriptions.Item>
                )}
            </Descriptions>
        </Modal>
    );
};

export default DetailModal;