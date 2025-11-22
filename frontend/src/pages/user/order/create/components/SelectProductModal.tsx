import React, { useEffect, useState } from "react";
import { Modal, Input, Table, Button } from "antd";
import { SearchOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import type { Product } from "../../../../../types/product";

interface Props {
  open: boolean;
  products: Product[];
  nextCursor: number | null;
  selectedProductIds: number[];
  loading?: boolean;
  onClose: () => void;
  onSearch: (value: string) => void;
  onSelectProducts: (selected: Product[]) => void;
  onLoadMore: () => void;
  setSelectedProductIds: React.Dispatch<React.SetStateAction<number[]>>;
  initialSelectedProducts?: Product[];
}

const SelectProductModal: React.FC<Props> = ({
  open,
  products,
  nextCursor,
  selectedProductIds,
  loading,
  onClose,
  onSearch,
  onSelectProducts,
  onLoadMore,
  setSelectedProductIds,
  initialSelectedProducts = [],
}) => {

  // Đồng bộ tick sẵn khi mở modal
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

  return (
    <Modal
      title="Chọn sản phẩm"
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={800}
      destroyOnClose
    >
      {/* Ô tìm kiếm */}
      <Input.Search
        placeholder="Tìm sản phẩm..."
        allowClear
        onSearch={onSearch}
        style={{ marginBottom: 12 }}
        enterButton={<SearchOutlined />}
      />

      {/* Bảng sản phẩm */}
      <Table
        rowKey="id"
        dataSource={products}
        loading={loading}
        pagination={false}
        rowSelection={{
          type: 'checkbox',
          selectedRowKeys: selectedProductIds,
          onChange: (keys) => setSelectedProductIds(keys as number[]),
        }}
        columns={[
          { 
            title: "Tên sản phẩm", 
            dataIndex: "name", 
            key: "name", 
            align: "center",
          },
          { 
            title: "Trọng lượng (Kg)", 
            dataIndex: "weight", 
            key: "weight", 
            align: "center",
            render: (weight: number) => (Number(weight).toFixed(2) || 0)
          },
          {
            title: "Giá (VNĐ)",
            dataIndex: "price",
            key: "price",
            align: "center",
            render: (p: number) => p?.toLocaleString("vi-VN") || '0',
          },
          { 
            title: "Loại", 
            dataIndex: "type", 
            key: "type", 
            align: "center",
          },
          {
            title: "Tồn kho",
            dataIndex: "stock",
            key: "stock",
            align: "center",
            render: (stock: number) => stock || 0,
          },
        ]}
      />

      {/* Nút hành động */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginTop: 16,
        }}
      >
        <Button
          icon={nextCursor ? <PlusOutlined /> : <ReloadOutlined />}
          onClick={onLoadMore}
          disabled={!nextCursor}
          loading={loading}
          style={{
            background: nextCursor ? "#1C3D90" : "#d9d9d9",
            color: nextCursor ? "#fff" : "#000",
            borderColor: nextCursor ? "#1C3D90" : "#d9d9d9",
          }}
        >
          {nextCursor ? "Xem thêm" : "Hết sản phẩm"}
        </Button>

        <div style={{ display: "flex", gap: 8 }}>
          <Button
            style={{ borderColor: "#1C3D90", color: "#1C3D90" }}
            onClick={handleCancel}
          >
            Hủy
          </Button>
          <Button
            type="primary"
            style={{ background: "#1C3D90", borderColor: "#1C3D90", color: "#ffffff"}}
            onClick={handleConfirm}
            disabled={selectedProductIds.length === 0}
          >
            Chọn ({selectedProductIds.length})
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default SelectProductModal;