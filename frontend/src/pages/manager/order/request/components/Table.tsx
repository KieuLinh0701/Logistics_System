import React, { useEffect } from 'react';
import { Table, Button, Tooltip, Dropdown } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import type { ShippingRequest } from '../../../../../types/shippingRequest';
import { canProcessingManagerShippingRequest, translateShippingRequestStatus, translateShippingRequestType } from '../../../../../utils/shippingRequestUtils';
import { formatAddress } from '../../../../../utils/locationUtils';
import dayjs from 'dayjs';
import { DownOutlined } from '@ant-design/icons';

interface TableProps {
  data: ShippingRequest[];
  currentPage: number;
  pageSize: number;
  total: number;
  loading?: boolean;
  onProcess: (requestId: number) => void;
  onDetail: (requestId: number) => void;
  onPageChange: (page: number, pageSize?: number) => void;
}

const RequestTable: React.FC<TableProps> = ({
  data,
  currentPage,
  pageSize,
  total,
  onProcess,
  loading = false,
  onDetail,
  onPageChange,
}) => {
  const navigate = useNavigate();
  const [addressMap, setAddressMap] = React.useState<Record<number, string>>({});

  const loadAddresses = async (list: ShippingRequest[]) => {
    const result: Record<number, string> = {};

    await Promise.all(
      list.map(async (item) => {
        try {
          const address = await formatAddress(
            item.contactDetail,
            item.contactWardCode,
            item.contactCityCode
          );

          result[item.id] = address;
        } catch {
          result[item.id] = item.contactDetail || '';
        }
      })
    );

    setAddressMap(result);
  };

  useEffect(() => {
    if (data?.length) {
      loadAddresses(data);
    }
  }, [data]);


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
      title: 'Người gửi',
      dataIndex: 'requester',
      key: 'requester',
      align: 'left',
      render: (_, record) => {
        const name = record.contactName || 'N/A';
        const userCode = record.userCode || 'Khách vãng lai';
        const email = record.contactEmail || 'N/A';
        const phone = record.contactPhoneNumber || 'N/A';

        const address = addressMap[record.id] || 'N/A';

        return (
          <div>
            <div className="custom-table-content-strong">
              {name} - {userCode}
            </div>

            <div>
              {email} - {phone}
            </div>

            <div className='custom-table-content-limit'>
              {address}
            </div>
          </div>
        );
      },
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
        const respondedAt = record.responseAt
          ? dayjs(record.responseAt).format('HH:mm:ss DD/MM/YYYY')
          : null;

        if (!response && !respondedAt) {
          return <span className="text-muted">Chưa phản hồi</span>;
        }

        return (
          <div>
            {response && (
              <Tooltip title={response} placement="topLeft">
                <div className='custom-table-content-limit'>
                  {response}
                </div>
              </Tooltip>
            )}
          </div>
        );
      },
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
        const canProcess = canProcessingManagerShippingRequest(record.status);

        const items = [
          {
            key: "detail",
            label: "Xem",
            onClick: () => onDetail(record.id),
          },
          {
            key: "process",
            label: "Xử lý",
            disabled: !canProcess,
            onClick: () => onProcess(record.id),
          },
        ];

        return (
          <Dropdown menu={{ items }} trigger={["click"]}>
            <Button className="dropdown-trigger-button">
              Thao tác <DownOutlined />
            </Button>
          </Dropdown>
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