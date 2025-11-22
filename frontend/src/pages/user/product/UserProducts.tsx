import React, { useEffect, useState } from 'react';
import * as XLSX from "xlsx";
import { Col, Form, message, Row, Tag } from 'antd';
import dayjs from 'dayjs';
import SearchFilters from './components/SearchFilters';
import Actions from './components/Actions';
import ProductTable from './components/Table';
import AddEditModal from './components/AddEditModal';
import Title from 'antd/es/typography/Title';
import type { Product, UserProductForm } from '../../../types/product';
import productApi from '../../../api/productApi';
import { DropboxOutlined } from '@ant-design/icons';
import "../../../styles/ListPage.css";
import { PRODUCT_STATUS, PRODUCT_TYPES, translateProductStatus, translateProductType } from '../../../utils/productUtils';
import type { BulkResponse } from '../../../types/response';
import BulkResult from './components/BulkResult';

const UserProducts: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);

  const [loading, setLoading] = useState(false);

  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [total, setTotal] = useState(0);

  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [sort, setSort] = useState('NEWEST');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [newProduct, setNewProduct] = useState<Partial<UserProductForm>>({});

  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkResult, setBulkResult] = useState<BulkResponse<Product>>();

  const [hover, setHover] = useState(false);
  const [filterStock, setFilterStock] = useState<string>('ALL');
  const [form] = Form.useForm();

  // Thêm sản phẩm
  const handleAddProduct = async () => {
    setLoading(true);
    await form.validateFields();
    try {
      const formData = new FormData();
      Object.entries(newProduct).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          if (value instanceof File) {
            formData.append(key, value);
          } else {
            formData.append(key, value.toString());
          }
        }
      });

      const result = await productApi.createUserProduct(formData);
      if (result.success && result.data) {
        message.success("Thêm sản phẩm thành công");
        setIsModalOpen(false);
        setNewProduct({});
        form.resetFields();
        fetchProducts(1);
      } else {
        message.error(result.message || "Lỗi khi thêm sản phẩm");
      }
    } catch (error: any) {
      const errorMessage = error?.message || error?.response?.data?.message || 'Có lỗi khi thêm sản phẩm!';
      console.log(errorMessage);
      message.error("Lỗi khi thêm sản phẩm");
    } finally {
      setLoading(false);
    }
  };

  // Sửa sản phẩm
  const handleEditProduct = async () => {
    setLoading(true);
    await form.validateFields();
    try {
      const formData = new FormData();
      Object.entries(newProduct).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          if (value instanceof File) {
            formData.append(key, value);
          } else {
            formData.append(key, value.toString());
          }
        }
      });

      const result = await productApi.updateUserProduct(formData);
      if (result.success && result.data) {
        message.success("Sửa sản phẩm thành công");
        setIsModalOpen(false);
        setNewProduct({});
        form.resetFields();
        fetchProducts(1);
      } else {
        message.error(result.message || "Lỗi khi sửa sản phẩm");
      }
    } catch (error: any) {
      const errorMessage = error?.message || error?.response?.data?.message || 'Có lỗi khi sửa sản phẩm!';
      console.log(errorMessage);
      message.error("Lỗi khi sửa sản phẩm");
    } finally {
      setLoading(false);
    }
  };

  // Thêm hàng loạt sản phẩm
  const handleExcelUpload = async (file: File) => {
    setLoading(true);
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const data = new Uint8Array(e.target?.result as ArrayBuffer);
        const workbook = XLSX.read(data, { type: "array" });
        const sheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[sheetName];

        if (!worksheet) {
          message.error("Không tìm thấy sheet trong file Excel!");
          return;
        }

        const rows: any[] = XLSX.utils.sheet_to_json(worksheet);

        console.log("Dữ liệu thô từ Excel:", rows);

        const newProducts: Partial<UserProductForm>[] = rows.map((row, index) => {
          console.log(`Dòng ${index + 1}:`, row);

          return {
            name: row["Tên sản phẩm"]?.toString().trim() || "",
            weight: parseFloat(row["Trọng lượng (kg)"]) || 0,
            price: parseInt(row["Giá sản phẩm (VNĐ)"]) || 0,
            type: row["Loại"]?.toString().trim() || "",
            status: (row["Trạng thái"]?.toString().trim() || "ACTIVE") as "ACTIVE" | "INACTIVE",
            stock: parseInt(row["Tồn kho"]) || 0,
          };
        });

        if (newProducts.length === 0) {
          message.error("Không tìm thấy dữ liệu sản phẩm trong file Excel!");
          return;
        }

        const invalidRows = newProducts.filter(
          (p) => !p.name || !p.type || p.weight === undefined || p.price === undefined
        );

        if (invalidRows.length > 0) {
          message.error(`Có ${invalidRows.length} dòng bị thiếu thông tin bắt buộc. Vui lòng kiểm tra lại file!`);
          console.log("Các dòng lỗi:", invalidRows);
          return;
        }

        const formData = new FormData();

        newProducts.forEach((p, i) => {
          if (p.id !== undefined) formData.append(`products[${i}].id`, p.id.toString());
          if (p.name) formData.append(`products[${i}].name`, p.name);
          if (p.price !== undefined) formData.append(`products[${i}].price`, p.price.toString());
          if (p.weight !== undefined) formData.append(`products[${i}].weight`, p.weight.toString());
          if (p.stock !== undefined) formData.append(`products[${i}].stock`, p.stock.toString());
          if (p.type) formData.append(`products[${i}].type`, p.type);
          if (p.status) formData.append(`products[${i}].status`, p.status);
        });

        const result = await productApi.createBulkUserProduct(formData);

        if (result?.success) {
          message.success(result?.message || "Import sản phẩm thành công!");

          setBulkResult(result);
          setBulkModalOpen(true);
          setPage(1);
          handleClearFilters();
          fetchProducts(1);
        } else {
          message.error(result?.message || "Thêm các sản phẩm thất bại");
        }

      } catch (err: any) {
        console.error("Chi tiết lỗi đọc Excel:", err);
        message.error(`Có lỗi khi đọc file Excel: ${err.message}`);
      } finally {
        setLoading(false);
      }
    };
    reader.readAsArrayBuffer(file);
    return false;
  };

  // Download template
  const handleDownloadTemplate = () => {
    const wb = XLSX.utils.book_new();

    const data = [
      {
        "Tên sản phẩm": "Áo tay dài",
        "Trọng lượng (kg)": 0.5,
        "Giá sản phẩm (VNĐ)": 200000,
        "Loại": "GOODS",
        "Trạng thái": "ACTIVE",
        "Tồn kho": "10",
      },
    ];

    const ws = XLSX.utils.json_to_sheet(data);

    const header = ["Tên sản phẩm", "Trọng lượng (kg)", "Giá sản phẩm (VNĐ)", "Loại", "Trạng thái", "Tồn kho"];
    XLSX.utils.sheet_add_aoa(ws, [header], { origin: "A1" });

    ws["!cols"] = [
      { wch: 25 },
      { wch: 15 },
      { wch: 20 },
      { wch: 20 },
      { wch: 15 },
      { wch: 12 },
    ];

    XLSX.utils.book_append_sheet(wb, ws, "Template");

    const productTypeNote = PRODUCT_TYPES
      .map((key) => `${key} (${translateProductType(key)})`)
      .join(" / ");

    const productStatusNote = PRODUCT_STATUS
      .map((key) => `${key} (${translateProductStatus(key)})`)
      .join(" / ");

    const noteData = [
      ["HƯỚNG DẪN NHẬP LIỆU"],
      [""],
      ["• Tên sản phẩm (bắt buộc): không trùng với những sản phẩm đã có"],
      ["• Trọng lượng (kg) (bắt buộc): Nhập số, ví dụ 0.5"],
      ["• Giá sản phẩm (bắt buộc): Số nguyên, không có dấu phẩy"],
      [`• Loại (bắt buộc): ${productTypeNote}`],
      [`• Trạng thái: ${productStatusNote}`],
      ["• Tồn kho: Số nguyên >= 0"],
    ];

    const wsNote = XLSX.utils.aoa_to_sheet(noteData);

    wsNote["!cols"] = [{ wch: 50 }];

    XLSX.utils.book_append_sheet(wb, wsNote, "Ghi chú");

    XLSX.writeFile(wb, "product_template.xlsx");
  };

  const fetchProducts = async (currentPage = page) => {
    try {
      setLoading(true);

      const param: any = {
        page: currentPage,
        limit: limit,
        search: search,
        type: filterType !== 'ALL' ? filterType : undefined,
        status: filterStatus !== 'ALL' ? filterStatus : undefined,
        sort: sort !== 'NEWEST' ? sort : undefined,
        stock: filterStock != 'ALL' ? filterStock : undefined,
      };

      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await productApi.getUserProducts(param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setProducts(list);
        setTotal(result.data.pagination?.total || result.data.list.length);
        setPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách sản phẩm");
      }
    } catch (error) {
      console.error("Error fetching products:", error);
    } finally {
      setLoading(false);
    }
  };

  // Xóa sản phẩm
  const handleDeleteProduct = async (productId: number) => {
    try {
      const result = await productApi.deleteUserProduct(productId);
      if (result.success) {
        message.success("Xóa sản phẩm thành công");
        fetchProducts(1);
      } else {
        message.error(result.message || "Lỗi khi xóa sản phẩm");
      }
    } catch (error: any) {
      const errorMessage = error?.message || error?.response?.data?.message || 'Có lỗi khi xóa sản phẩm!';
      console.log(errorMessage);
      message.error("Lỗi khi xóa sản phẩm")
    }
  }

  // Lọc sản phẩm
  const handleFilterChange = (filter: string, value: string) => {
    switch (filter) {
      case 'type':
        setFilterType(value);
        break;
      case 'status':
        setFilterStatus(value);
        break;
      case 'stock':
        setFilterStock(value);
        break;
    }
    setPage(1);
  };

  const handleClearFilters = () => {
    setSearch('');
    setFilterType('ALL');
    setFilterStatus('ALL');
    setSort('NEWEST');
    setDateRange(null);
    setPage(1);
    setFilterStock('ALL');
  };

  // Effects
  useEffect(() => {
    fetchProducts();
  }, []);


  useEffect(() => {
    setPage(1);
    fetchProducts(1);
  }, [search, filterType, filterStatus, sort, dateRange, filterStock]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          search={search}
          filterType={filterType}
          filterStatus={filterStatus}
          filterStock={filterStock}
          sort={sort}
          dateRange={dateRange}
          hover={hover}
          onSearchChange={setSearch}
          onFilterChange={handleFilterChange}
          onSortChange={setSort}
          onDateRangeChange={setDateRange}
          onClearFilters={handleClearFilters}
          onHoverChange={setHover}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <DropboxOutlined className="title-icon" />
              Danh sách sản phẩm
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                onAdd={() => {
                  setIsModalOpen(true);
                  setModalMode('create');
                  setNewProduct({});
                  form.resetFields();
                }}
                onImportExcel={handleExcelUpload}
                onDownloadTemplate={handleDownloadTemplate}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} sản phẩm</Tag>

        <ProductTable
          data={products}
          page={page}
          limit={limit}
          total={total}
          loading={loading}
          onEdit={(product) => {
            setModalMode('edit');
            setNewProduct(product);
            setIsModalOpen(true);
            form.setFieldsValue(product);
          }}
          onDelete={handleDeleteProduct}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
            fetchProducts(page);
          }}
        />

        <AddEditModal
          open={isModalOpen}
          mode={modalMode}
          product={newProduct}
          loading={loading}
          onOk={modalMode === 'edit' ? handleEditProduct : handleAddProduct}
          onCancel={() => {
            setIsModalOpen(false);
            setNewProduct({});
            form.resetFields();
          }}
          onProductChange={(data) => {
            setNewProduct(prev => ({ ...prev, ...data }));
          }}
          form={form}
        />

        {bulkResult &&
          <BulkResult
            open={bulkModalOpen}
            results={bulkResult}
            onClose={() => setBulkModalOpen(false)}
          />
        }
      </div>
    </div>
  );
};

export default UserProducts;