import React, { useEffect } from "react";
import { Modal, Input, Table, Select, Tag, Row, Col } from "antd";
import { SearchOutlined } from "@ant-design/icons";
import type { Product } from "../../../../../types/product";
import { PRODUCT_TYPES, translateProductType } from "../../../../../utils/productUtils";
import type { ColumnsType } from "antd/es/table";

interface Props {
  open: boolean;
  products: Product[];
  page: number;
  limit: number;
  total: number;
  selectedProductIds: number[];
  loading?: boolean;
  onClose: () => void;
  onSearch: (value: string) => void;
  filterType: string;
  onFilterTypeChange: (value: string) => void;
  onSelectProducts: (selected: Product[]) => void;
  setSelectedProductIds: React.Dispatch<React.SetStateAction<number[]>>;
  initialSelectedProducts?: Product[];
  onPageChange: (page: number, limit?: number) => void;
}

const SelectProductModal: React.FC<Props> = ({
  open,
  products,
  page,
  limit,
  total,
  selectedProductIds,
  loading,
  onClose,
  onSearch,
  filterType,
  onFilterTypeChange,
  onSelectProducts,
  setSelectedProductIds,
  initialSelectedProducts = [],
  onPageChange,
}) => {

  useEffect(() => {
    if (open && initialSelectedProducts.length > 0) {
      const initialIds = initialSelectedProducts.map(p => p.id);
      setSelectedProductIds(initialIds);
    }
  }, [open, initialSelectedProducts, setSelectedProductIds]);

  const handleConfirm = () => {
    const selectedProducts = products.filter((p) =>
      selectedProductIds.includes(p.id)
    );
    onSelectProducts(selectedProducts);
    onClose();
  };

  const handleCancel = () => {
    setSelectedProductIds([]);
    onClose();
  };

  const columns: ColumnsType<any> = [
    {
      title: "Mã",
      dataIndex: "code",
      key: "code",
      align: "center"
    },
    {
      title: "Tên sản phẩm",
      dataIndex: "name",
      key: "name",
      align: "left"
    },
    {
      title: "Trọng lượng (Kg)",
      dataIndex: "weight",
      key: "weight",
      align: "center",
      render: (w: number) => Number(w).toFixed(2)
    },
    {
      title: "Giá (VNĐ)",
      dataIndex: "price",
      key: "price",
      align: "center",
      render: (p: number) => p?.toLocaleString("vi-VN") || '0'
    },
    {
      title: "Loại",
      dataIndex: "type",
      key: "type",
      align: "center",
      render: (p: string) => translateProductType(p)
    },
    {
      title: "Tồn kho",
      dataIndex: "stock",
      key: "stock",
      align: "center",
      render: (s: number) => s || 0
    },
  ]

  return (
    <Modal
      title={<span className='modal-title'>Chọn sản phẩm</span>}
      open={open}
      onCancel={handleCancel}
      onOk={handleConfirm}
      okText={`Chọn (${selectedProductIds.length})`}
      okButtonProps={{
        className: "modal-ok-button",
        loading,
        disabled: selectedProductIds.length === 0
      }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      cancelText="Hủy"
      width={800}
      className="modal-hide-scrollbar"
    >

      <div className="search-filters-container">
        <Row className="search-filters-row" gutter={16}>
          <Col span={24}>
           <div className="list-page-actions">
            {/* Ô tìm kiếm */}
            <Input
              className="search-input"
              placeholder="Tìm theo mã hoặc tên sản phẩm..."
              allowClear
              onChange={(e) => onSearch(e.target.value)}
              prefix={<SearchOutlined />}
            />

            <Select
              value={filterType}
              onChange={(val) => onFilterTypeChange(val)}
              className="filter-select"
              listHeight={PRODUCT_TYPES.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả loại</Select.Option>
              {PRODUCT_TYPES.map((t) => (
                <Select.Option key={t} value={t}>
                  {translateProductType(t)}
                </Select.Option>
              ))}
            </Select>
            </div>
          </Col>
        </Row>
      </div>

      <div className="list-page-header" />

      <Tag className="list-page-tag">Kết quả trả về: {products.length} sản phẩm</Tag>

      {/* Bảng sản phẩm */}
      <div className="table-container">
        <Table
          rowKey="id"
          dataSource={products}
          loading={loading}
          scroll={{ x: "max-content" }}
          className="list-page-table"
          pagination={{
            current: page,
            pageSize: limit,
            total: total,
            onChange: (newPage, newLimit) => onPageChange(newPage, newLimit),
          }}
          rowSelection={{
            type: 'checkbox',
            selectedRowKeys: selectedProductIds,
            onChange: (keys) => setSelectedProductIds(keys as number[]),
          }}
          rowClassName={(record) =>
            selectedProductIds.includes(record.id) ? "create-order-modal-product-row-selected" : ""
          }
          columns={columns}
        />
      </div>
    </Modal>
  );
};

export default SelectProductModal;