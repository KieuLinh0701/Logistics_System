import React from 'react';
import { Table, Button, Dropdown } from 'antd';
import { DeleteOutlined, DownOutlined, EditOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { Product } from '../../../../types/product';
import defaultImage from "../../../../assets/images/imageDefault.jpg";
import { translateProductStatus, translateProductType } from '../../../../utils/productUtils';

interface ProductTableProps {
  data: Product[];
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onEdit: (product: Product) => void;
  onDelete: (productId: number) => void;
  onPageChange: (page: number, limit?: number) => void;
}

const ProductTable: React.FC<ProductTableProps> = ({
  data,
  page,
  limit,
  total,
  loading,
  onEdit,
  onDelete,
  onPageChange,
}) => {
  const columns: ColumnsType<Product> = [
    { title: 'Mã SP', dataIndex: 'code', key: 'code', align: 'left' },
    {
      title: 'Ảnh',
      key: 'image',
      align: 'left',
      render: (_, record: Product) => (
        <img
          className='table-image'
          src={record.image || defaultImage}
          alt={record.name}
        />
      ),
    },
    { title: 'Tên', dataIndex: 'name', key: 'name', align: 'left' },
    {
      title: 'Loại',
      dataIndex: 'type',
      key: 'type',
      align: 'left',
      render: (type) => translateProductType(type)
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      align: 'left',
      render: (status) => translateProductStatus(status)
    },
    {
      title: 'Thông tin',
      key: 'info',
      align: 'left',
      render: (_, record: Product) => (
        <div>
          {`Trọng lượng: ${record.weight}kg`}
          <br />
          {`Giá: ${record.price.toLocaleString()} VNĐ`}
        </div>
      )
    },
    {
      title: 'Thống kê',
      key: 'stats',
      align: 'left',
      render: (_, record: Product) => (
        <div>
          {`Tồn kho: ${record.stock}`}
          <br />
          {`Tổng bán: ${record.soldQuantity}`}
        </div>
      )
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      align: 'left',
      render: (date) => dayjs(date).format('DD-MM-YYYY')
    },
    {
      key: 'action',
      align: 'left',
      render: (_, record: Product) => {
        if (record.soldQuantity !== 0) {
          return (
            <Button
              className="action-button-link"
              icon={<EditOutlined />}
              onClick={() => onEdit(record)}
            >
              Sửa
            </Button>
          );
        }
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
          <Dropdown
            menu={{ items }}
            trigger={['click']}
          >
            <Button className="dropdown-trigger-button">
              Hành động <DownOutlined />
            </Button>
          </Dropdown>
        );
      },
    },
  ];

  const tableData = data.map((p, index) => ({
    ...p,
    key: String(index + 1 + (page - 1) * limit),
  }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="key"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        loading={loading}
        pagination={{
          current: page,
          pageSize: limit,
          total,
          onChange: (page, pageSize) => onPageChange(page, pageSize)
        }}
      />
    </div>
  );
};

export default ProductTable;