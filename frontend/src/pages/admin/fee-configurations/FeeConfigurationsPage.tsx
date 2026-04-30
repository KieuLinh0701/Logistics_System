import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Form, Pagination, message } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import feeConfigurationApi from "../../../api/feeConfigurationApi";
import serviceTypeApi from "../../../api/serviceTypeApi";
import type {
  CreateFeeConfigurationPayload,
  FeeConfiguration,
} from "../../../types/feeConfiguration";
import type { AdminServiceType } from "../../../types/serviceType";
import FeeConfigurationFormModal from "./components/FeeConfigurationFormModal";
import FeeConfigurationDetailsModal from "./components/FeeConfigurationDetailsModal";
import FeeConfigurationsTable from "./components/FeeConfigurationsTable";
import FeeConfigurationsToolbar from "./components/FeeConfigurationsToolbar";
import type { Option, QueryState } from "../../../types/feeConfiguration";
import "../../../styles/ListPage.css";
import "../../../pages/hr/recruitment/components/RecruitmentShared.css";
import "./FeeConfigurationsPage.css";

const feeTypeOptions: Option[] = [
  { label: "Phi thu ho (COD)", value: "COD" },
  { label: "Phi dong goi", value: "PACKAGING" },
  { label: "Phi bao hiem", value: "INSURANCE" },
  { label: "Thue VAT", value: "VAT" },
];

const calculationTypeOptions: Option[] = [
  { label: "Co dinh", value: "FIXED" },
  { label: "Phan tram (%)", value: "PERCENTAGE" },
];

const FeeConfigurationsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<FeeConfiguration[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selected, setSelected] = useState<FeeConfiguration | null>(null);
  const [editing, setEditing] = useState<FeeConfiguration | null>(null);
  const [serviceTypes, setServiceTypes] = useState<AdminServiceType[]>([]);
  const [canSubmit, setCanSubmit] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await feeConfigurationApi.listAdminFeeConfigurations(query);
      if (res.success && res.data) {
        const data: any = res.data;
        const list = Array.isArray(data.list) ? data.list : Array.isArray(data.data) ? data.data : [];
        setRows(list);
        setTotal(data.pagination?.total || 0);
      }
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Tai du lieu that bai");
    } finally {
      setLoading(false);
    }
  }, [query]);

  const fetchServiceTypes = useCallback(async () => {
    try {
      const res = await serviceTypeApi.listAdminServiceTypes({ page: 1, limit: 100 });
      if (res.success && res.data) {
        setServiceTypes(res.data.data || []);
      }
    } catch {
      setServiceTypes([]);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    fetchServiceTypes();
  }, [fetchServiceTypes]);

  const feeTypeLabel = (value: string) => {
    return feeTypeOptions.find((item) => item.value === value)?.label || value;
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ active: true, calculationType: "PERCENTAGE" });
    setCanSubmit(false);
    setOpen(true);
  };

  const openEdit = (record: FeeConfiguration) => {
    setEditing(record);
    form.setFieldsValue({
      serviceTypeId: record.serviceTypeId,
      feeType: record.feeType,
      calculationType: record.calculationType,
      feeValue: record.feeValue,
      minOrderFee: record.minOrderFee,
      maxOrderFee: record.maxOrderFee,
      active: record.active,
      notes: record.notes,
    });
    setCanSubmit(true);
    setOpen(true);
  };

  const deleteRecord = async (id: number) => {
    try {
      await feeConfigurationApi.deleteAdminFeeConfiguration(id);
      message.success("Da xoa cau hinh");
      fetchData();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Xoa that bai");
    }
  };

  const submitForm = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();
      const payload: CreateFeeConfigurationPayload = {
        serviceTypeId: values.serviceTypeId,
        feeType: values.feeType,
        calculationType: values.calculationType,
        feeValue: values.feeValue,
        minOrderFee: values.minOrderFee,
        maxOrderFee: values.maxOrderFee,
        active: values.active,
        notes: values.notes,
      };
      if (editing) {
        await feeConfigurationApi.updateAdminFeeConfiguration(editing.id, payload);
        message.success("Cap nhat thanh cong");
      } else {
        await feeConfigurationApi.createAdminFeeConfiguration(payload);
        message.success("Tao moi thanh cong");
      }
      setOpen(false);
      fetchData();
    } catch (error: any) {
      if (!error?.errorFields) {
        message.error(error?.response?.data?.message || "Luu that bai");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const onFormChange = async () => {
    try {
      await form.validateFields();
      setCanSubmit(true);
    } catch {
      setCanSubmit(false);
    }
  };

  const toolbarState = useMemo(
    () => ({
      search: query.search,
      feeType: query.feeType,
      active: query.active,
    }),
    [query]
  );

  return (
    <div className="list-page-layout fee-configurations-page">
      <div className="list-page-content">
        <FeeConfigurationsToolbar
          search={toolbarState.search}
          feeType={toolbarState.feeType}
          active={toolbarState.active}
          feeTypeOptions={feeTypeOptions}
          onSearchChange={(value) => setQuery((prev) => ({ ...prev, page: 1, search: value }))}
          onFeeTypeChange={(value) => setQuery((prev) => ({ ...prev, page: 1, feeType: value }))}
          onActiveChange={(value) => setQuery((prev) => ({ ...prev, page: 1, active: value }))}
          onRefresh={fetchData}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý cấu hình phí</h3>
            <div style={{ marginTop: 12 }}>
              <div className="list-page-tag">Kết quả trả về: {total} cấu hình phí</div>
            </div>
          </div>
          <div className="list-page-actions">
            <button className="primary-button" onClick={openCreate} style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <PlusOutlined />
              Thêm cấu hình
            </button>
          </div>
        </div>

        <div className="list-page-table">
          <FeeConfigurationsTable
            loading={loading}
            rows={rows}
              onView={(record: FeeConfiguration) => {
                setSelected(record);
                setDetailOpen(true);
              }}
            onEdit={openEdit}
            onDelete={deleteRecord}
            feeTypeLabel={feeTypeLabel}
          />
          <div className="list-page__pagination">
            <Pagination
              current={query.page}
              pageSize={query.limit}
              total={total}
              showSizeChanger
              showTotal={(count) => `Tong ${count} ban ghi`}
              onChange={(page, pageSize) =>
                setQuery((prev) => ({ ...prev, page, limit: pageSize }))
              }
            />
          </div>
        </div>

        <FeeConfigurationFormModal
          open={open}
          editing={Boolean(editing)}
          form={form}
          serviceTypes={serviceTypes}
          feeTypeOptions={feeTypeOptions}
          calculationTypeOptions={calculationTypeOptions}
          canSubmit={canSubmit}
          submitting={submitting}
          onCancel={() => setOpen(false)}
          onSubmit={submitForm}
          onFormChange={onFormChange}
        />

        <FeeConfigurationDetailsModal
          open={detailOpen}
          record={selected}
          feeTypeLabel={feeTypeLabel}
          onClose={() => {
            setDetailOpen(false);
            setSelected(null);
          }}
        />
      </div>
    </div>
  );
};

export default FeeConfigurationsPage;
