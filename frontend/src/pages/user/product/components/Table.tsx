import React, { useState } from 'react';
import { Table, Button, Dropdown, Modal } from 'antd';
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
  const [previewImage, setPreviewImage] = useState<string | null>(null);

  const handleImageClick = (src: string) => {
    setPreviewImage(src);
  };

  const handleModalClose = () => {
    setPreviewImage(null);
  };

  const columns: ColumnsType<Product> = [
    {
      title: 'Mã SP',
      dataIndex: 'code',
      key: 'code',
      align: 'left',
      render: (code, _) => {
        return (
          <span className="custom-table-content-strong">
            {code}
          </span>
        );
      }
    },
    {
      title: 'Ảnh',
      key: 'image',
      align: 'left',
      render: (_, record: Product) => (
        <img
          className='table-image'
          src={record.image || defaultImage}
          alt={record.name}
          onClick={() => handleImageClick(record.image || defaultImage)}
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
        <>
          <span className="custom-table-content-strong">Khối lượng quy đổi: </span>{record.weight} Kg<br />
          <span className="custom-table-content-strong">Giá SP: </span>{record.price.toLocaleString()} VNĐ
        </>
      )
    },
    {
      title: 'Thống kê',
      key: 'stats',
      align: 'left',
      render: (_, record: Product) => (
        <>
          <span className="custom-table-content-strong">Tồn kho: </span>{record.stock} <br />
          <span className="custom-table-content-strong">Tổng bán: </span>{record.soldQuantity}
        </>
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
        const items = [];

        items.push({
          key: "edit",
          icon: <EditOutlined />,
          label: "Sửa",
          onClick: () => onEdit(record),
        });

        if (record.soldQuantity === 0) {
          items.push({
            key: "delete",
            icon: <DeleteOutlined />,
            label: "Xóa",
            onClick: () => onDelete(record.id),
          });
        }

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
      }
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
      <Modal
        open={!!previewImage}
        footer={null}
        onCancel={handleModalClose}
      >
        <img src={previewImage || undefined} alt="Preview" className="preview-image" />
      </Modal>
    </div>
  );
};

export default ProductTable;