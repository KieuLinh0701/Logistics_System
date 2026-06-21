import React, {useCallback, useEffect, useState} from "react";
import {Form, message, Modal} from "antd";
import {PlusOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import promotionApi from "../../../api/promotionApi";
import serviceTypeApi from "../../../api/serviceTypeApi";
import userApi from "../../../api/userApi";
import type {CreatePromotionPayload, Promotion} from "../../../types/promotion";
import type {AdminServiceType} from "../../../types/serviceType";
import type {AdminUser} from "../../../types/user";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "../../../styles/ListPage.css";
import "../AdminModal.css";
import "./PromotionsPage.css";
import PromotionsToolbar from "./components/PromotionsToolbar";
import PromotionsTable from "./components/PromotionsTable";
import AddEditPromotionModal from "./components/AddEditPromotionModal";
import PromotionDetailsDrawer from "./components/PromotionDetailsDrawer";

type QueryState = { page: number; limit: number; search: string; status?: string; isGlobal?: boolean };

const statusOptions = [
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Tạm dừng", value: "INACTIVE" },
  { label: "Hết hạn", value: "EXPIRED" },
];

const discountTypeOptions = [
  { label: "Phần trăm (%)", value: "PERCENTAGE" },
  { label: "Số tiền cố định", value: "FIXED" },
];

const PromotionsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Promotion[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });

  const [searchValue, setSearchValue] = useState("");
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);
  const [filterIsGlobal, setFilterIsGlobal] = useState<boolean | undefined>(undefined);

  const [openForm, setOpenForm] = useState(false);
  const [editing, setEditing] = useState<Promotion | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedPromotion, setSelectedPromotion] = useState<Promotion | null>(null);

  const [form] = Form.useForm();
  const [serviceTypes, setServiceTypes] = useState<AdminServiceType[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [isGlobal, setIsGlobal] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery((prev) => ({ ...prev, page: 1, search: searchValue }));
    }, 300);
    return () => clearTimeout(timer);
  }, [searchValue]);

  useEffect(() => {
    setQuery((prev) => ({ ...prev, page: 1, status: filterStatus, isGlobal: filterIsGlobal }));
  }, [filterIsGlobal, filterStatus]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await promotionApi.listAdminPromotions({
        page: query.page,
        limit: query.limit,
        search: query.search,
        status: query.status,
        isGlobal: query.isGlobal,
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
  }, [query.page, query.limit, query.search, query.status, query.isGlobal]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    (async () => {
      try {
        const res = await serviceTypeApi.listAdminServiceTypes({ page: 1, limit: 100 });
        if (res.success && res.data) setServiceTypes(res.data.data || []);
      } catch {
        setServiceTypes([]);
      }
    })();
  }, []);

  useEffect(() => {
    (async () => {
      if (isGlobal) {
        setUsers([]);
        return;
      }

      try {
        const res = await userApi.listAdminUsers({ page: 1, limit: 100 });
        if (res.success && res.data) setUsers(res.data.data || []);
      } catch {
        setUsers([]);
      }
    })();
  }, [isGlobal]);

  const statusText = (status?: string) => {
    if (!status) return "-";
    return statusOptions.find((option) => option.value === status)?.label || status;
  };

  const promotionTypeText = (promotion: Promotion) => {
    if (!promotion.isGlobal) return "Theo user";
    if (promotion.minOrderValue || promotion.minWeight || promotion.minOrdersCount || promotion.firstTimeUser) {
      return "Điều kiện";
    }
    return "Chung";
  };

  const onCreate = () => {
    setEditing(null);
    setIsGlobal(true);
    form.resetFields();
    form.setFieldsValue({
      isGlobal: true,
      discountType: "PERCENTAGE",
      status: "ACTIVE",
      firstTimeUser: false,
    });
    setOpenForm(true);
  };

  const onView = (record: Promotion) => {
    setSelectedPromotion(record);
    setDetailOpen(true);
  };

  const onEdit = (record: Promotion) => {
    setEditing(record);
    setIsGlobal(record.isGlobal);
    form.setFieldsValue({
      code: record.code,
      title: record.title,
      description: record.description,
      discountType: record.discountType,
      discountValue: record.discountValue,
      isGlobal: record.isGlobal,
      maxDiscountAmount: record.maxDiscountAmount,
      startDate: record.startDate ? dayjs(record.startDate) : null,
      endDate: record.endDate ? dayjs(record.endDate) : null,
      minOrderValue: record.minOrderValue,
      minWeight: record.minWeight,
      maxWeight: record.maxWeight,
      minOrdersCount: record.minOrdersCount,
      serviceTypeIds: record.serviceTypes ? record.serviceTypes.map((serviceType) => serviceType.id) : undefined,
      firstTimeUser: record.firstTimeUser,
      validMonthsAfterJoin: record.validMonthsAfterJoin,
      validYearsAfterJoin: record.validYearsAfterJoin,
      usageLimit: record.usageLimit,
      maxUsagePerUser: record.maxUsagePerUser,
      dailyUsageLimitGlobal: record.dailyUsageLimitGlobal,
      dailyUsageLimitPerUser: record.dailyUsageLimitPerUser,
      status: record.status,
      userIds: (record as any).userIds,
    });
    setOpenForm(true);
  };

  const onDelete = (record: Promotion) => {
    if (record.usedCount > 0) {
      message.warning("Khuyến mãi đã được sử dụng, không thể xóa");
      return;
    }

    Modal.confirm({
      title: "Xóa khuyến mãi này?",
      okText: "Xóa",
      cancelText: "Hủy",
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await promotionApi.deleteAdminPromotion(record.id);
          message.success("Đã xóa");
          fetchData();
        } catch (error: any) {
          message.error(error?.response?.data?.message || "Xóa thất bại");
        }
      },
    });
  };

  const submit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      const payload: CreatePromotionPayload = {
        code: values.code?.toUpperCase().trim(),
        title: values.title,
        description: values.description,
        discountType: values.discountType,
        discountValue: values.discountValue,
        isGlobal: values.isGlobal,
        maxDiscountAmount: values.maxDiscountAmount,
        startDate: values.startDate?.toISOString(),
        endDate: values.endDate?.toISOString(),
        minOrderValue: values.minOrderValue,
        minWeight: values.minWeight,
        maxWeight: values.maxWeight,
        minOrdersCount: values.minOrdersCount,
        serviceTypeIds: values.serviceTypeIds,
        firstTimeUser: values.firstTimeUser,
        validMonthsAfterJoin: values.validMonthsAfterJoin,
        validYearsAfterJoin: values.validYearsAfterJoin,
        usageLimit: values.usageLimit,
        maxUsagePerUser: values.maxUsagePerUser,
        dailyUsageLimitGlobal: values.dailyUsageLimitGlobal,
        dailyUsageLimitPerUser: values.dailyUsageLimitPerUser,
        status: values.status || "ACTIVE",
        userIds: !values.isGlobal ? values.userIds : undefined,
      };

      if (editing) {
        await promotionApi.updateAdminPromotion(editing.id, payload);
        message.success("Cập nhật thành công");
      } else {
        await promotionApi.createAdminPromotion(payload);
        message.success("Tạo mới thành công");
      }

      setOpenForm(false);
      fetchData();
    } catch (error: any) {
      if (!error?.errorFields) {
        message.error(error?.response?.data?.message || "Lưu thất bại");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleRefreshAll = () => {
    setSearchValue("");
    setFilterStatus(undefined);
    setFilterIsGlobal(undefined);
    setQuery((prev) => ({ ...prev, page: 1, search: "", status: undefined, isGlobal: undefined }));
  };

  return (
    <div className="list-page-layout promotions-page">
      <div className="list-page-content">
        <PromotionsToolbar
          searchValue={searchValue}
          filterStatus={filterStatus}
          filterIsGlobal={filterIsGlobal}
          statusOptions={statusOptions}
          onSearchChange={setSearchValue}
          onStatusChange={setFilterStatus}
          onIsGlobalChange={setFilterIsGlobal}
          onRefresh={handleRefreshAll}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý khuyến mãi</h3>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Kết quả trả về: {total} khuyến mãi</div>
            </div>
          </div>
          <div className="list-page-actions">
            <button className="primary-button" onClick={onCreate} style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <PlusOutlined />
              Thêm khuyến mãi
            </button>
          </div>
        </div>

        <div className="list-page-table">
          <PromotionsTable
            loading={loading}
            rows={rows}
            page={query.page}
            pageSize={query.limit}
            total={total}
            statusText={statusText}
            promotionTypeText={promotionTypeText}
            onPageChange={(page, pageSize) =>
              setQuery((prev) => ({
                ...prev,
                page,
                limit: pageSize || prev.limit,
              }))
            }
            onView={onView}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </div>

        <PromotionDetailsDrawer
          open={detailOpen}
          promotion={selectedPromotion}
          statusText={statusText}
          promotionTypeText={promotionTypeText}
          onClose={() => setDetailOpen(false)}
        />

        <AddEditPromotionModal
          open={openForm}
          editing={editing}
          form={form}
          submitting={submitting}
          isGlobal={isGlobal}
          statusOptions={statusOptions}
          discountTypeOptions={discountTypeOptions}
          serviceTypes={serviceTypes}
          users={users}
          onCancel={() => setOpenForm(false)}
          onSubmit={submit}
          onIsGlobalChange={setIsGlobal}
          onValuesChange={() => undefined}
        />
      </div>
    </div>
  );
};

export default PromotionsPage;
