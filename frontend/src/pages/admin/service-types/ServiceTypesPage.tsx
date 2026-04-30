import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Form, message } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import serviceTypeApi from "../../../api/serviceTypeApi";
import type { AdminServiceType } from "../../../types/serviceType";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "../../../styles/ListPage.css";
import "../AdminModal.css";
import "./ServiceTypesPage.css";
import ServiceTypesToolbar from "./components/ServiceTypesToolbar";
import ServiceTypesTable from "./components/ServiceTypesTable";
import AddEditServiceTypeModal from "./components/AddEditServiceTypeModal";

type QueryState = { page: number; limit: number; search: string };

const statusOptions = [
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Tạm ngưng", value: "INACTIVE" },
];

const timeUnitOptions = [
  { label: "Ngày", value: "ngày" },
  { label: "Giờ", value: "giờ" },
];

const ServiceTypesPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminServiceType[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AdminServiceType | null>(null);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const [searchValue, setSearchValue] = useState("");
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);

  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery((prev) => ({ ...prev, page: 1, search: searchValue }));
    }, 300);
    return () => clearTimeout(timer);
  }, [searchValue]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await serviceTypeApi.listAdminServiceTypes({
        page: query.page,
        limit: query.limit,
        search: query.search,
      });
      if (res.success && res.data) {
        setRows(res.data.data || []);
        setTotal(res.data.pagination?.total || 0);
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [query.page, query.limit, query.search]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const onCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const onEdit = (record: AdminServiceType) => {
    setEditing(record);
    const parsed = parseDeliveryTime(record.deliveryTime);
    form.setFieldsValue({
      name: record.name,
      description: record.description,
      status: record.status,
      timeFrom: parsed?.from,
      timeTo: parsed?.to,
      timeUnit: parsed?.unit,
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await serviceTypeApi.deleteAdminServiceType(id);
      message.success("Đã xóa");
      fetchData();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Xóa thất bại");
    }
  };

  const submit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();
      const payload = {
        name: values.name,
        description: values.description,
        status: values.status,
        deliveryTimeFrom: values.timeFrom,
        deliveryTimeTo: values.timeTo,
        deliveryTimeUnit: values.timeUnit,
      };

      if (editing) {
        await serviceTypeApi.updateAdminServiceType(editing.id, payload);
        message.success("Cập nhật thành công");
      } else {
        await serviceTypeApi.createAdminServiceType(payload);
        message.success("Tạo mới thành công");
      }
      setOpen(false);
      fetchData();
    } catch (e: any) {
      if (!e?.errorFields) {
        message.error(e?.response?.data?.message || "Lưu thất bại");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const displayedRows = useMemo(() => {
    return rows.filter((row) => {
      if (filterStatus && row.status !== filterStatus) return false;
      return true;
    });
  }, [rows, filterStatus]);

  const hasLocalFilters = Boolean(filterStatus);

  const handleRefreshAll = () => {
    setSearchValue("");
    setFilterStatus(undefined);
    setQuery((prev) => ({ ...prev, page: 1, search: "" }));
  };

  return (
    <div className="list-page-layout service-types-page">
      <div className="list-page-content">
        <ServiceTypesToolbar
          searchValue={searchValue}
          filterStatus={filterStatus}
          statusOptions={statusOptions}
          onSearchChange={setSearchValue}
          onStatusChange={(v) => {
            setFilterStatus(v);
            setQuery((prev) => ({ ...prev, page: 1 }));
          }}
          onRefresh={handleRefreshAll}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý loại dịch vụ</h3>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Kết quả trả về: {hasLocalFilters ? displayedRows.length : total} loại dịch vụ</div>
            </div>
          </div>
          <div className="list-page-actions">
            <button className="primary-button" onClick={onCreate} style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <PlusOutlined />
              Thêm loại dịch vụ
            </button>
          </div>
        </div>

        <div className="list-page-table">
          <ServiceTypesTable
            loading={loading}
            rows={displayedRows}
            page={query.page}
            pageSize={query.limit}
            total={hasLocalFilters ? displayedRows.length : total}
            onPageChange={(p, ps) => setQuery({ ...query, page: p, limit: ps || query.limit })}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </div>

        <AddEditServiceTypeModal
          open={open}
          editing={editing}
          form={form}
          submitting={submitting}
          statusOptions={statusOptions}
          timeUnitOptions={timeUnitOptions}
          onCancel={() => setOpen(false)}
          onSubmit={submit}
        />
      </div>
    </div>
  );
};

function parseDeliveryTime(value?: string) {
  if (!value) return null;
  const match = value.match(/(\d+)\s*-\s*(\d+)\s*(.+)/);
  if (match) {
    return {
      from: Number(match[1]),
      to: Number(match[2]),
      unit: match[3].trim(),
    };
  }
  return null;
}

export default ServiceTypesPage;
