import React from 'react';
import { Table, Button, Space, Tooltip, Dropdown } from 'antd';
import { CloseCircleOutlined, DownOutlined, EditOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router-dom';
import type { ShippingRequest } from '../../../../../types/shippingRequest';
import { canCancelUserShippingRequest, canEditUserShippingRequest, translateShippingRequestStatus, translateShippingRequestType } from '../../../../../utils/shippingRequestUtils';

interface TableProps {
  data: ShippingRequest[];
  currentPage: number;
  pageSize: number;
  total: number;
  loading?: boolean;
  onEdit: (requestId: number) => void;
  onCancel: (request: ShippingRequest) => void;
  onDetail: (requestId: number) => void;
  onPageChange: (page: number, pageSize?: number) => void;
}

const RequestTable: React.FC<TableProps> = ({
  data,
  currentPage,
  pageSize,
  total,
  loading = false,
  onEdit,
  onDetail,
  onCancel,
  onPageChange,
}) => {
  const navigate = useNavigate();

  const columns: ColumnsType<ShippingRequest> = [
    {
      title: 'Mã yêu cầu',
      dataIndex: 'code',
      key: 'code',
      align: 'center',
      render: (_, record) => {
        return (
          <Tooltip title="Click để xem chi tiết yêu cầu">
            <span
              className="navigate-link"
              onClick={() => onDetail(record.id)}
            >
              {record.code}
            </span>
          </Tooltip>
        );
      }
    },
    {
      title: 'Loại yêu cầu',
      dataIndex: 'requestType',
      key: 'requestType',
      align: 'center',
      render: (type) => translateShippingRequestType(type)
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      align: 'center',
      render: (status) => translateShippingRequestStatus(status)
    },
    {
      title: 'Nội dung yêu cầu',
      dataIndex: 'contentRequest',
      key: 'contentRequest',
      align: 'left',
      render: (_, record) => {
        const hasTracking = !!record.orderTrackingNumber;
        const hasContent = !!record.requestContent;

        if (!hasTracking && !hasContent) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            {hasTracking && (
              <>
                <span className="custom-table-content-strong">ĐH liên quan:</span><br />
                <Tooltip title="Click để xem chi tiết đơn hàng">
                  <span
                    className="navigate-link-default"
                    onClick={() => navigate(`/orders/tracking/${record.orderTrackingNumber}`)}
                  >
                    {record.orderTrackingNumber}
                  </span>
                </Tooltip>
                <br />
              </>
            )}
            {hasContent && (
              <>
                <span className="custom-table-content-strong">Nội dung:</span><br />
                <Tooltip title={record.requestContent} placement="topLeft">
                  <div className='custom-table-content-limit'>
                    {record.requestContent}
                  </div>
                </Tooltip>
              </>
            )}
          </div>
        );
      },
    },
    {
      title: 'Nội dung phản hồi',
      dataIndex: 'response',
      key: 'response',
      align: 'left',
      render: (_, record) => {
        const response = record.response;
        if (response) {
          return (
            <>
              <Tooltip title={response} placement="topLeft">
                <div className='custom-table-content-limit'>
                  {response}
                </div>
              </Tooltip>
            </>
          );
        }

        return (
          <span className="text-muted">
            Chưa có phản hồi
          </span>
        );
      }
    },
    {
      title: 'Thời gian',
      key: 'time',
      align: 'left',
      render: (_, record) => {
        const createdAt = record.createdAt
          ? dayjs(record.createdAt).format('HH:mm:ss DD/MM/YYYY')
          : null;

        const responseAt = record.responseAt
          ? dayjs(record.responseAt).format('HH:mm:ss DD/MM/YYYY')
          : null;

        return (
          <div>
            {createdAt && (
              <div>
                <span className="custom-table-content-strong">
                  Gửi:{" "}
                </span>
                {createdAt}
              </div>
            )}
            <div>
              <span className="custom-table-content-strong">
                Phản hồi:{" "}
              </span>
              {responseAt ? responseAt : <span className="text-muted">N/A</span>}
            </div>
          </div>
        );
      }
    },
    {
      key: "action",
      align: "left",
      render: (_, record: ShippingRequest) => {
        const canCancel = canCancelUserShippingRequest(record.status);
        const canEdit = canEditUserShippingRequest(record.status);

        const items = [
          ...(canEdit
            ? [
              {
                key: "edit",
                icon: <EditOutlined />,
                label: "Sửa",
                onClick: () => onEdit(record.id),
              },
            ]
            : []),

          ...(canCancel
            ? [
              {
                key: "cancel",
                icon: <CloseCircleOutlined />,
                label: "Hủy",
                danger: true,
                onClick: () => onCancel(record),
              },
            ]
            : []),
        ];

        return (
          <Space>
            <Button
              className="action-button-link"
              type="link"
              onClick={() => onDetail(record.id)}
            >
              Xem
            </Button>

            <Dropdown menu={{ items }} trigger={["click"]} disabled={items.length === 0}>
              <Button className="dropdown-trigger-button">
                Thêm <DownOutlined />
              </Button>
            </Dropdown>
          </Space>
        );
      },
    },
  ];

  const tableData = data.map((p, index) => ({
    ...p,
    key: String(index + 1 + (currentPage - 1) * pageSize),
  }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="id"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        pagination={{
          current: currentPage,
          pageSize,
          total,
          onChange: onPageChange,
        }}
        loading={loading}
      />
    </div>
  );
};

export default RequestTable;