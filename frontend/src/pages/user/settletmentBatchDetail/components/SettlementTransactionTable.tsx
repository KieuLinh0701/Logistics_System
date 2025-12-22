import React from "react";
import dayjs from 'dayjs';
import { Popover, Table } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { SettlementTransaction } from "../../../../types/settlementTransaction";
import { translateSettlementTransactionStatus, translateSettlementTransactionType } from "../../../../utils/settlementTransactionUtils";

interface Props {
  datas: SettlementTransaction[];
  loading: boolean;
}

const SettlementTransactionTable: React.FC<Props> = ({
  datas,
  loading,
}) => {
  const tableData = datas.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<SettlementTransaction> = [
    {
      title: "Mã giao dịch",
      key: "code",
      align: "center",
      render: (_, record) => {
        return (
          <span className="custom-table-content-strong">
            {record.code}
          </span>
        );
      }
    },
    {
      title: "Loại",
      dataIndex: "type",
      key: "type",
      align: "center",
      render: (_, record) => (
        <>
          <div>{translateSettlementTransactionType(record.type)}</div>
        </>
      ),
    },
    {
      title: "Số tiền",
      key: "amount",
      dataIndex: "amount",
      align: "center",
      render: (amount) => amount?.toLocaleString('vi-VN')
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      align: "center",
      render: (_, record) => (
        <span className={
          record.status === 'SUCCESS' ? 'custom-table-content-strong' :
            record.status === 'FAILED' ? 'custom-table-content-error' :
              'custom-table-content-pending'
        }>
          {translateSettlementTransactionStatus(record.status)}
        </span>
      )
    },
    {
      title: 'Ngân hàng nhận',
      key: 'info',
      align: 'left',
      render: (_, record) => {
        if (!record.bankName) return <span className="text-muted">N/A</span>;

        return (
          <Popover
            content={
              <div>
                <div>{record.bankName}</div>
                <div className="text-muted">{record.accountName}</div>
                <div className="text-muted">{record.accountNumber}</div>
              </div>
            }
            trigger="click"
          >
            <span className="navigate-link">Hiển thị</span>
          </Popover>
        );
      }
    },
    {
      title: "Ngày thanh toán",
      dataIndex: "paidAt",
      key: "paidAt",
      align: "center",
      render: (value) =>
        value ? dayjs(value).format("DD/MM/YYYY hh:mm:ss") : <span className="text-muted">N/A</span>,
    },
  ];

  return (
    <div className="table-container">
      {datas.length > 0 ? (
        <Table
          columns={columns}
          dataSource={tableData}
          rowKey="key"
          scroll={{ x: "max-content" }}
          className="list-page-table"
          loading={loading}
        />
      ) :
        <div className="no-data-message" style={{ textAlign: 'center', padding: '20px', color: '#888' }}>
          Không có lịch sử thanh toán
        </div>
      }
    </div>
  );
};

export default SettlementTransactionTable;