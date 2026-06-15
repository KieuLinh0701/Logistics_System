import React, { useEffect, useState, useMemo } from "react";
import { Alert, Form, message } from "antd";
import { PlusOutlined, UsergroupAddOutlined } from "@ant-design/icons";
import { roleTypeLabelMap } from "../../../common/recruitment/recruitmentHelpers";
import officeApi from "../../../../api/officeApi";
import recruitmentApi from "../../../../api/recruitmentApi";
import type { JobPosting, JobPostingStatus } from "../../../../types/recruitment";
import SearchFilters from "./components/SearchFilters";
import JobPostingTable from "./components/JobPostingTable";
import AddEditModal from "./components/AddEditModal";
import { getUserRole, getUserOfficeId, getUserId } from "../../../../utils/authUtils";
import userApi from "../../../../api/userApi";
import "../components/RecruitmentShared.css";
import "./components/JobPostingComponents.css";
import "../../../manager/order/request/ManagerShippingRequest.css";

interface OfficeOption {
  label: string;
  value: string;
}

interface OfficeItem {
  id: number;
  name: string;
}

interface OfficeListPayload {
  data?: OfficeItem[];
}

const JobPostingManagementPage: React.FC = () => {
  const [form] = Form.useForm();

  const [jobs, setJobs] = useState<JobPosting[]>([]);
  const [jobsPagination, setJobsPagination] = useState({ page: 1, limit: 10, total: 0 });
  const [jobsLoading, setJobsLoading] = useState(false);
  const [jobsError, setJobsError] = useState<string | null>(null);

  const [filters, setFilters] = useState<{ page: number; limit: number; status?: JobPostingStatus }>({ page: 1, limit: 10 });

  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccessMessage, setActionSuccessMessage] = useState<string | null>(null);

  const [searchText, setSearchText] = useState<string>("");
  const [openModal, setOpenModal] = useState(false);
  const [editingJob, setEditingJob] = useState<JobPosting | null>(null);
  const [officeOptions, setOfficeOptions] = useState<OfficeOption[]>([]);
  const [managerOfficeIdState, setManagerOfficeIdState] = useState<number | null>(getUserOfficeId());

  const clearMessages = () => {
    setActionError(null);
    setActionSuccessMessage(null);
    setJobsError(null);
  };

  const fetchJobs = async (opts = filters) => {
    setJobsLoading(true);
    setJobsError(null);
    try {
      const params: any = { page: opts.page, limit: opts.limit, status: opts.status };
      if (getUserRole() === "manager" && managerOfficeIdState) params.officeId = managerOfficeIdState;
      const res = await recruitmentApi.getJobs(params);
      if (res) {
        const payload = (res as any).data ?? res;

        if (payload) {
          if (Array.isArray(payload.list)) {
            setJobs(payload.list);
            setJobsPagination({ page: payload.pagination?.page || opts.page, limit: payload.pagination?.limit || opts.limit, total: payload.pagination?.total || 0 });
          } else if (Array.isArray((payload as any).data)) {
            setJobs((payload as any).data);
            setJobsPagination({ page: (payload as any).page || opts.page, limit: (payload as any).limit || opts.limit, total: (payload as any).total || 0 });
          } else if (Array.isArray(payload)) {
            setJobs(payload as any);
            setJobsPagination({ page: opts.page, limit: opts.limit, total: (payload as any).length });
          }
        }
      }
    } catch (err: any) {
      setJobsError(err?.message || "Lỗi khi tải danh sách");
    } finally {
      setJobsLoading(false);
    }
  };

  const createJob = async (payload: any) => {
    setActionLoading(true);
    setActionError(null);
    try {
      await recruitmentApi.createJob(payload as any);
      setActionSuccessMessage("Tạo tin tuyển dụng thành công");
    } catch (err: any) {
      setActionError(err?.message || "Lỗi khi tạo");
    } finally {
      setActionLoading(false);
    }
  };

  const updateJob = async ({ id, payload }: { id: number; payload: any }) => {
    setActionLoading(true);
    setActionError(null);
    try {
      await recruitmentApi.updateJob(id, payload);
      setActionSuccessMessage("Cập nhật thành công");
    } catch (err: any) {
      setActionError(err?.message || "Lỗi khi cập nhật");
    } finally {
      setActionLoading(false);
    }
  };

  const deleteJob = async (id: number) => {
    setActionLoading(true);
    setActionError(null);
    try {
      await recruitmentApi.deleteJob(id);
      setActionSuccessMessage("Xóa thành công");
    } catch (err: any) {
      setActionError(err?.message || "Lỗi khi xóa");
    } finally {
      setActionLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs(filters);
    const fetchOffices = async () => {
      try {
        const res = await officeApi.searchOffice({ page: 1, limit: 100 });
        let offices: any[] = [];
        if (res && res.success) {
          const payload = (res as any).data;
          if (Array.isArray(payload)) offices = payload;
          else if (payload && Array.isArray(payload.data)) offices = payload.data;
        }
        setOfficeOptions(
          offices.map((office) => ({ value: String(office.id), label: `${office.name} (#${office.id})` }))
        );

        if (getUserRole() === "manager" && !managerOfficeIdState) {
          try {
            const uid = getUserId();
            if (uid) {
              const userRes: any = await userApi.getAdminUserById(uid);
              if (userRes && userRes.success && userRes.data) {
                const userObj = (userRes as any).data;
                if (userObj.officeId) setManagerOfficeIdState(userObj.officeId);
                else if (userObj.office && userObj.office.id) setManagerOfficeIdState(userObj.office.id);
              }
            }
          } catch (e) {
            // ingore 
          }
        }
      } catch {
        setOfficeOptions([]);
      }
    };

    fetchOffices();
    return () => clearMessages();
  }, [filters.page, filters.limit, filters.status]);

  useEffect(() => {
    if (getUserRole() === "manager" && managerOfficeIdState) {
      fetchJobs(filters);
    }
  }, [managerOfficeIdState]);

  useEffect(() => {
    (async () => {
      if (getUserRole() === "manager" && !managerOfficeIdState) {
        try {
          const r: any = await officeApi.getManagerOffice();
          if (r && r.success && r.data && r.data.id) setManagerOfficeIdState(r.data.id);
        } catch (e) {
          // ingore
        }
      }
    })();
  }, []);

  useEffect(() => {
    if (actionError) message.error(actionError);
  }, [actionError]);

  useEffect(() => {
    if (actionSuccessMessage) {
      message.success(actionSuccessMessage);
      setOpenModal(false);
      setEditingJob(null);
      form.resetFields();
      fetchJobs(filters);
      setTimeout(() => setActionSuccessMessage(null), 1500);
    }
  }, [actionSuccessMessage]);

  const handleEdit = (job: JobPosting) => {
    console.log('[JobPostingManagementPage] handleEdit called with job=', job);
    setEditingJob(job);
    try {
      form.setFieldsValue({ title: job.title, description: job.description, roleType: job.roleType, officeId: String(job.officeId), status: job.status, quantityNeeded: job.quantityNeeded, shift: job.shift });
      console.log('[JobPostingManagementPage] form.getFieldsValue()=', form.getFieldsValue());
      console.log('[JobPostingManagementPage] individual fields:', {
        title: form.getFieldValue('title'),
        description: form.getFieldValue('description'),
        roleType: form.getFieldValue('roleType'),
        officeId: form.getFieldValue('officeId'),
        status: form.getFieldValue('status'),
        quantityNeeded: form.getFieldValue('quantityNeeded'),
        shift: form.getFieldValue('shift'),
      });
    } catch (e) {
      console.warn('[JobPostingManagementPage] setFieldsValue failed', e);
    }
    setOpenModal(true);
  };

  const handleCreate = () => {
    setEditingJob(null);
    form.resetFields();
    form.setFieldValue("status", "OPEN");
    form.setFieldValue("quantityNeeded", 1);
    if (getUserRole() === "manager" && managerOfficeIdState) {
      try {
        form.setFieldsValue({ officeId: String(managerOfficeIdState) });
      } catch (e) {
        // ingore
      }
    }
    console.log('[JobPostingManagementPage] handleCreate: managerOfficeIdState=', managerOfficeIdState, 'form.officeId=', form.getFieldValue('officeId'));
    setOpenModal(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const payload = { ...values, officeId: values.officeId !== undefined ? Number(values.officeId) : undefined };
    if (editingJob) await updateJob({ id: editingJob.id, payload });
    else await createJob(payload);
  };

  const handleRefresh = () => fetchJobs(filters);

  const onToggleStatus = (job: JobPosting, nextStatus: JobPostingStatus) => updateJob({ id: job.id, payload: { status: nextStatus } });

  const filteredData = useMemo(() => {
    if (!searchText) return jobs;
    const q = searchText.toLowerCase();
    return jobs.filter((r) => {
      const title = (r.title || "").toString().toLowerCase();
      const role = (roleTypeLabelMap[r.roleType] || "").toString().toLowerCase();
      const office = (r.officeName || (`#${r.officeId}`) || "").toString().toLowerCase();
      return title.includes(q) || role.includes(q) || office.includes(q);
    });
  }, [jobs, searchText]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <style>{'.hr-recruitment-filter-actions .ant-btn-primary{display:none !important}'} </style>
        <SearchFilters
          status={filters.status}
          onStatusChange={(status) => setFilters((s) => ({ ...s, page: 1, status }))}
          onRefresh={handleRefresh}
          onCreate={handleCreate}
          onSearchChange={(v) => setSearchText(v || "")}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <UsergroupAddOutlined className="title-icon" />
              <h3 className="list-page-title-main">Quản lý tin tuyển dụng</h3>
            </div>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Tổng tin tuyển dụng: {jobsPagination.total}</div>
            </div>
          </div>
          <div className="list-page-actions">
            <button className="primary-button" onClick={handleCreate} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              Tạo tin tuyển dụng
            </button>
          </div>
        </div>

        {jobsError && <Alert type="error" message={jobsError} showIcon />}

        <div className="list-page-table">
          <JobPostingTable
            data={filteredData}
            loading={jobsLoading}
            actionLoading={actionLoading}
            currentPage={jobsPagination.page}
            pageSize={jobsPagination.limit}
            total={jobsPagination.total}
            onPageChange={(page, limit) => setFilters((s) => ({ ...s, page, limit }))}
            onEdit={handleEdit}
            onToggleStatus={onToggleStatus}
            onDelete={(id) => deleteJob(id)}
          />
        </div>
      </div>

      <AddEditModal
        open={openModal}
        editingJob={editingJob}
        actionLoading={actionLoading}
        form={form}
        officeOptions={officeOptions}
        isManager={getUserRole() === "manager"}
        managerOfficeId={managerOfficeIdState}
        onCancel={() => setOpenModal(false)}
        onSubmit={handleSubmit}
      />
    </div>
  );
};

export default JobPostingManagementPage;
