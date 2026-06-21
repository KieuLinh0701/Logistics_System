import React, {useCallback, useEffect, useState} from "react";
import {Form, message} from "antd";
import {PlusOutlined} from "@ant-design/icons";
import vehicleApi from "../../../api/vehicleApi";
import axiosClient from "../../../api/axiosClient";
import type {AdminVehicle} from "../../../types/vehicle";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "../../../styles/ListPage.css";
import "../AdminModal.css";
import "./VehiclesPage.css";
import VehiclesToolbar from "./components/VehiclesToolbar";
import VehiclesTable from "./components/VehiclesTable";
import VehicleDetailsDrawer from "./components/VehicleDetailsDrawer";
import VehicleTrackingsModal from "./components/VehicleTrackingsModal";
import AddEditVehicleModal from "./components/AddEditVehicleModal";

type QueryState = { page: number; limit: number; search: string; type?: string; status?: string };

type OfficeOption = { id: number; name: string };
type TrackingPoint = { id: number; latitude: number; longitude: number; recordedAt: string };

const typeOptions = [
  { label: "Xe tải", value: "TRUCK" },
  { label: "Xe van", value: "VAN" },
  { label: "Container", value: "CONTAINER" },
];

const statusOptions = [
  { label: "Sẵn sàng", value: "AVAILABLE" },
  { label: "Đang sử dụng", value: "IN_USE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
  { label: "Lưu trữ", value: "ARCHIVED" },
];

const VehiclesPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminVehicle[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });

  const [searchValue, setSearchValue] = useState("");
  const [filterType, setFilterType] = useState<string | undefined>(undefined);
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);

  const [offices, setOffices] = useState<OfficeOption[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<AdminVehicle | null>(null);

  const [trackingsModalOpen, setTrackingsModalOpen] = useState(false);
  const [trackingPoints, setTrackingPoints] = useState<TrackingPoint[]>([]);
  const [trackingLoading, setTrackingLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingVehicle, setEditingVehicle] = useState<AdminVehicle | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery((prev) => ({ ...prev, page: 1, search: searchValue }));
    }, 300);
    return () => clearTimeout(timer);
  }, [searchValue]);

  useEffect(() => {
    setQuery((prev) => ({ ...prev, page: 1, type: filterType, status: filterStatus }));
  }, [filterStatus, filterType]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await vehicleApi.listAdminVehicles({
        page: query.page,
        limit: query.limit,
        search: query.search,
        type: query.type,
        status: query.status,
      });

      if (res.success && res.data) {
        setRows(res.data.data || []);
        setTotal(res.data.pagination?.total || 0);
      }
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [query.page, query.limit, query.search, query.status, query.type]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    (async () => {
      try {
        const res = await axiosClient.get<any>("/admin/offices");
        if (res && typeof res === "object" && "success" in res) {
          const inner = res.data;
          if (inner && Array.isArray(inner.data)) setOffices(inner.data);
          else if (Array.isArray(inner)) setOffices(inner);
          else setOffices([]);
        } else if (Array.isArray(res)) setOffices(res);
        else setOffices(res?.data || []);
      } catch {
        setOffices([]);
      }
    })();
  }, []);

  const statusText = (status?: string) => {
    if (!status) return "-";
    return statusOptions.find((option) => option.value === status)?.label || status;
  };

  const typeText = (type?: string) => {
    if (!type) return "-";
    return typeOptions.find((option) => option.value === type)?.label || type;
  };

  const onViewDetails = (record: AdminVehicle) => {
    setSelectedVehicle(record);
    setDrawerOpen(true);
  };

  const fetchTrackings = async (vehicleId: number) => {
    try {
      setTrackingLoading(true);
      const res = await axiosClient.get<any>(`/admin/vehicles/${vehicleId}/trackings`);
      let list: any[] = [];

      if (res && typeof res === "object" && "success" in res) list = res.data?.data || [];
      else if (res && res.data) list = res.data?.data || res.data || [];
      else if (Array.isArray(res)) list = res;

      setTrackingPoints(
        (list || []).map((item: any) => ({
          id: item.id,
          latitude: Number(item.latitude),
          longitude: Number(item.longitude),
          recordedAt: item.recordedAt || item.recorded_at,
        }))
      );
    } catch {
      setTrackingPoints([]);
    } finally {
      setTrackingLoading(false);
    }
  };

  const onEdit = (record: AdminVehicle) => {
    setEditingVehicle(record);
    form.setFieldsValue({
      type: record.type,
      capacity: record.capacity,
      status: record.status,
      description: record.description,
      officeId: record.office?.id || record.officeId,
      gpsDeviceId: record.gpsDeviceId,
      nextMaintenanceDue: record.nextMaintenanceDue,
    });
    setModalOpen(true);
  };

  const onAdd = () => {
    setEditingVehicle(null);
    form.resetFields();
    form.setFieldsValue({ status: "AVAILABLE" });
    setModalOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await vehicleApi.deleteAdminVehicle(id);
      message.success("Đã xóa");
      fetchData();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Xóa thất bại");
    }
  };

  const submitForm = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      if (editingVehicle) {
        await vehicleApi.updateAdminVehicle(editingVehicle.id, values);
        message.success("Cập nhật phương tiện thành công");
      } else {
        await vehicleApi.createAdminVehicle({
          licensePlate: values.licensePlate,
          type: values.type,
          capacity: values.capacity,
          status: values.status,
          description: values.description,
          officeId: values.officeId,
        });
        message.success("Thêm phương tiện thành công");
      }

      setModalOpen(false);
      fetchData();
    } catch (error: any) {
      if (!error?.errorFields) {
        message.error(error?.response?.data?.message || "Thao tác thất bại");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleRefreshAll = () => {
    setSearchValue("");
    setFilterType(undefined);
    setFilterStatus(undefined);
    setQuery((prev) => ({ ...prev, page: 1, search: "", type: undefined, status: undefined }));
  };

  return (
    <div className="list-page-layout vehicles-page">
      <div className="list-page-content">
        <VehiclesToolbar
          searchValue={searchValue}
          filterType={filterType}
          filterStatus={filterStatus}
          typeOptions={typeOptions}
          statusOptions={statusOptions}
          onSearchChange={setSearchValue}
          onTypeChange={setFilterType}
          onStatusChange={setFilterStatus}
          onRefresh={handleRefreshAll}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý phương tiện</h3>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Kết quả trả về: {total} phương tiện</div>
            </div>
          </div>
          <div className="list-page-actions">
            <button className="primary-button" onClick={onAdd} style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <PlusOutlined />
              Thêm phương tiện
            </button>
          </div>
        </div>

        <div className="list-page-table">
          <VehiclesTable
            loading={loading}
            rows={rows}
            page={query.page}
            pageSize={query.limit}
            total={total}
            statusText={statusText}
            typeText={typeText}
            onPageChange={(page, pageSize) =>
              setQuery((prev) => ({
                ...prev,
                page,
                limit: pageSize || prev.limit,
              }))
            }
            onView={onViewDetails}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </div>

        <VehicleDetailsDrawer
          open={drawerOpen}
          selectedVehicle={selectedVehicle}
          typeText={typeText}
          statusText={statusText}
          onClose={() => setDrawerOpen(false)}
          onViewTrackings={() => {
            if (!selectedVehicle) return;
            fetchTrackings(selectedVehicle.id);
            setTrackingsModalOpen(true);
          }}
        />

        <VehicleTrackingsModal
          open={trackingsModalOpen}
          loading={trackingLoading}
          points={trackingPoints}
          onClose={() => setTrackingsModalOpen(false)}
        />

        <AddEditVehicleModal
          open={modalOpen}
          editingVehicle={editingVehicle}
          form={form}
          submitting={submitting}
          typeOptions={typeOptions}
          statusOptions={statusOptions}
          offices={offices}
          onCancel={() => setModalOpen(false)}
          onSubmit={submitForm}
          onValuesChange={() => undefined}
        />
      </div>
    </div>
  );
};

export default VehiclesPage;
