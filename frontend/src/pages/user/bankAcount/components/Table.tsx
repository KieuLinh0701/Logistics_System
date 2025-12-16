import React from 'react';
import { Table, Button, Tag, Switch, Dropdown } from 'antd';
import { CheckOutlined, CloseOutlined, EditOutlined, DeleteOutlined, DownOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { BankAccount } from '../../../../types/bankAccount';

interface BankAccountTableProps {
  data: BankAccount[];
  loading?: boolean;
  onEdit: (account: BankAccount) => void;
  onDelete: (accountId: number) => void;
  onSetDefault: (accountId: number) => void;
}

const BankAccountTable: React.FC<BankAccountTableProps> = ({
  data,
  loading = false,
  onEdit,
  onDelete,
  onSetDefault,
}) => {

  const columns: ColumnsType<BankAccount> = [
    { title: 'Tên ngân hàng', dataIndex: 'bankName', key: 'bankName', align: 'left' },
    { title: 'Số tài khoản', dataIndex: 'accountNumber', key: 'accountNumber', align: 'left' },
    { title: 'Tên chủ tài khoản', dataIndex: 'accountName', key: 'accountName', align: 'left' },
    {
      title: 'Mặc định',
      dataIndex: 'isDefault',
      key: 'isDefault',
      align: 'center',
      render: (val: boolean, record: BankAccount) => (
        <Switch
          className={"custom-switch"}
          checked={val}
          disabled={val}
          onChange={() => onSetDefault(record.id)}
          checkedChildren={<CheckOutlined />}
          unCheckedChildren={<CloseOutlined />}
        />
      ),
    },
    {
      title: "Ghi chú",
      dataIndex: "notes",
      key: "notes",
      align: "left",
      render: (value) => {
        if (value) {
          return <span className="custom-table-content-limit">{value}</span>;
        } else {
          return <span className="text-muted">N/A</span>;
        }
      },
    },
    {
      key: 'action',
      align: 'left',
      render: (_: any, record: BankAccount) => {
        const items = [
          {
            key: "edit",
            icon: <EditOutlined />,
            label: "Sửa",
            onClick: () => onEdit(record),
          },
          {
            key: "delete",
            icon: <DeleteOutlined />,
            label: "Xóa",
            onClick: () => onDelete(record.id),
          },
        ];

        return (
          <Dropdown menu={{ items }} trigger={['click']}>
            <Button className="dropdown-trigger-button">
              Hành động <DownOutlined />
            </Button>
          </Dropdown>
        );
      },
    },
  ];

  const tableData = data.map((p) => ({ ...p, key: p.id }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="key"
        loading={loading}
        scroll={{ x: "max-content" }}
        className="list-page-table"
        pagination={false}
      />
    </div>
  );
};

export default BankAccountTable;