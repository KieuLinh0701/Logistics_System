import React, {useCallback, useEffect, useMemo, useState} from "react";
import {Form, message} from "antd";
import {PlusOutlined} from "@ant-design/icons";
import officeApi from "../../../api/officeApi";
import locationApi from "../../../api/locationApi";
import {formatAddress as formatAddressUtil} from "../../../utils/locationUtils";
import type {AdminOffice, CreateOfficePayload} from "../../../types/office";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "../../../styles/ListPage.css";
import "../AdminModal.css";
import "./PostOfficesPage.css";
import PostOfficesToolbar from "./components/PostOfficesToolbar";
import PostOfficesTable from "./components/PostOfficesTable";
import OfficeDetailsDrawer from "./components/OfficeDetailsDrawer";
import AddEditPostOfficeModal from "./components/AddEditPostOfficeModal";

type QueryState = { page: number; limit: number; search: string };

const officeTypeOptions = [
  { label: "Trụ sở chính", value: "HEAD_OFFICE" },
  { label: "Bưu cục", value: "POST_OFFICE" },
];

const officeStatusOptions = [
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Tạm ngừng", value: "INACTIVE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
];

const PostOfficesPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<(AdminOffice & { displayAddress?: string })[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });

  const [openDrawer, setOpenDrawer] = useState(false);
  const [selectedOffice, setSelectedOffice] = useState<(AdminOffice & { displayAddress?: string }) | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingOffice, setEditingOffice] = useState<AdminOffice | null>(null);

  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [cities, setCities] = useState<Array<{ code: number; name: string }>>([]);
  const [wards, setWards] = useState<Array<{ code: number; name: string }>>([]);

  const [searchValue, setSearchValue] = useState("");
  const [filterType, setFilterType] = useState<string | undefined>(undefined);
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
      const res = await officeApi.listAdminOffices({
        page: query.page,
        limit: query.limit,
        search: query.search,
      });
      if (res.success && res.data) {
        const items: AdminOffice[] = res.data.data || [];
        const mapped = await Promise.all(
          items.map(async (it) => {
            try {
              const displayAddress = await formatAddressUtil(it.detail || "", Number(it.wardCode) || 0, Number(it.cityCode) || 0);
              return { ...it, displayAddress };
            } catch {
              return {
                ...it,
                displayAddress:
                  (it.detail ? it.detail + " - " : "") +
                  (it.wardCode ? `Phường ${it.wardCode}` : "") +
                  (it.cityCode ? ` - TP ${it.cityCode}` : ""),
              };
            }
          })
        );
        setRows(mapped);
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

  useEffect(() => {
    if (modalOpen) {
      locationApi
        .getCities()
        .then((cs) => setCities((cs || []).map((c: any) => ({ code: c.code, name: c.name }))))
        .catch(() => setCities([]));
    }
  }, [modalOpen]);

  const onViewDetails = (record: AdminOffice & { displayAddress?: string }) => {
    setSelectedOffice(record);
    setOpenDrawer(true);
  };

  const onAdd = () => {
    setEditingOffice(null);
    form.resetFields();
    setModalOpen(true);
  };

  const onEdit = (record: AdminOffice) => {
    setEditingOffice(record);
    form.setFieldsValue({
      code: record.code,
      postalCode: record.postalCode,
      name: record.name,
      latitude: record.latitude,
      longitude: record.longitude,
      email: record.email,
      phoneNumber: record.phoneNumber,
      openingTime: record.openingTime,
      closingTime: record.closingTime,
      type: record.type,
      status: record.status,
      capacity: record.capacity,
      notes: record.notes,
      wardCode: record.wardCode,
      cityCode: record.cityCode,
      detailAddress: record.detail,
    });

    if (record.cityCode) {
      locationApi
        .getWardsByCity(Number(record.cityCode))
        .then((ws) => setWards((ws || []).map((w: any) => ({ code: w.code, name: w.name }))))
        .catch(() => setWards([]));
    }

    setModalOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await officeApi.deleteAdminOffice(id);
      message.success("Đã xóa");
      fetchData();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Xóa thất bại");
    }
  };

  const normalizePayload = (values: any): CreateOfficePayload => ({
    code: values.code,
    postalCode: values.postalCode,
    name: values.name,
    latitude: Number(values.latitude),
    longitude: Number(values.longitude),
    email: values.email,
    phoneNumber: values.phoneNumber,
    openingTime: values.openingTime,
    closingTime: values.closingTime,
    type: values.type,
    status: values.status,
    capacity: values.capacity != null ? Number(values.capacity) : undefined,
    notes: values.notes,
    wardCode: Number(values.wardCode),
    cityCode: Number(values.cityCode),
    detailAddress: values.detailAddress,
  });

  const submitForm = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();
      const payload = normalizePayload(values);

      if (editingOffice) {
        await officeApi.updateAdminOffice(editingOffice.id, payload);
        message.success("Cập nhật bưu cục thành công");
      } else {
        await officeApi.createAdminOffice(payload);
        message.success("Thêm bưu cục thành công");
      }

      setModalOpen(false);
      fetchData();
    } catch (e: any) {
      if (!e?.errorFields) {
        message.error(e?.response?.data?.message || "Thao tác thất bại");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const displayedRows = useMemo(() => {
    return rows.filter((row) => {
      if (filterType && row.type !== filterType) return false;
      if (filterStatus && row.status !== filterStatus) return false;
      return true;
    });
  }, [rows, filterStatus, filterType]);

  const hasLocalFilters = Boolean(filterType || filterStatus);

  const handleRefreshAll = () => {
    setSearchValue("");
    setFilterType(undefined);
    setFilterStatus(undefined);
    setQuery((prev) => ({ ...prev, page: 1, search: "" }));
  };

  const handleCityChange = (val?: number) => {
    if (val) {
      locationApi
        .getWardsByCity(Number(val))
        .then((ws) => setWards((ws || []).map((w: any) => ({ code: w.code, name: w.name }))))
        .catch(() => setWards([]));
      form.setFieldsValue({ wardCode: undefined });
    } else {
      setWards([]);
    }
  };

  return (
    <div className="list-page-layout postoffices-page">
      <div className="list-page-content">
        <PostOfficesToolbar
          searchValue={searchValue}
          filterType={filterType}
          filterStatus={filterStatus}
          officeTypeOptions={officeTypeOptions}
          officeStatusOptions={officeStatusOptions}
          onSearchChange={setSearchValue}
          onTypeChange={(v) => {
            setFilterType(v);
            setQuery((prev) => ({ ...prev, page: 1 }));
          }}
          onStatusChange={(v) => {
            setFilterStatus(v);
            setQuery((prev) => ({ ...prev, page: 1 }));
          }}
          onRefresh={handleRefreshAll}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý bưu cục</h3>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Kết quả trả về: {hasLocalFilters ? displayedRows.length : total} bưu cục</div>
            </div>
          </div>
          <div className="list-page-actions">
            <button className="primary-button" onClick={onAdd} style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <PlusOutlined />
              Thêm bưu cục
            </button>
          </div>
        </div>

        <div className="list-page-table">
          <PostOfficesTable
            loading={loading}
            rows={displayedRows}
            page={query.page}
            pageSize={query.limit}
            total={hasLocalFilters ? displayedRows.length : total}
            officeTypeOptions={officeTypeOptions}
            officeStatusOptions={officeStatusOptions}
            onPageChange={(p, ps) => setQuery({ ...query, page: p, limit: ps || query.limit })}
            onView={onViewDetails}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </div>

        <OfficeDetailsDrawer
          open={openDrawer}
          selectedOffice={selectedOffice}
          officeTypeOptions={officeTypeOptions}
          officeStatusOptions={officeStatusOptions}
          onClose={() => setOpenDrawer(false)}
        />

        <AddEditPostOfficeModal
          open={modalOpen}
          editingOffice={editingOffice}
          form={form}
          submitting={submitting}
          officeTypeOptions={officeTypeOptions}
          officeStatusOptions={officeStatusOptions}
          cities={cities}
          wards={wards}
          onCancel={() => setModalOpen(false)}
          onSubmit={submitForm}
          onValuesChange={() => undefined}
          onCityChange={handleCityChange}
        />
      </div>
    </div>
  );
};

export default PostOfficesPage;
