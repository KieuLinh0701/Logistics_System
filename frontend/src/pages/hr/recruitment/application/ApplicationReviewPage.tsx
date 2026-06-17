import React, {useEffect, useState} from "react";
import {Alert, message} from "antd";
import recruitmentApi from "../../../../api/recruitmentApi";
import type {JobApplication, JobApplicationStatus} from "../../../../types/recruitment";
import {getUserOfficeId, getUserRole} from "../../../../utils/authUtils";
import officeApi from "../../../../api/officeApi";
import {UsergroupAddOutlined} from "@ant-design/icons";
import SearchFilters from "./components/SearchFilters";
import ApplicationTable from "./components/ApplicationTable";
import ApplicationDetailModal from "./components/ApplicationDetailModal";
import "../components/RecruitmentShared.css";
import "./components/ApplicationComponents.css";

const ApplicationReviewPage: React.FC = () => {
  const [applications, setApplications] = useState<JobApplication[]>([]);
  const [applicationsPagination, setApplicationsPagination] = useState({ page: 1, limit: 10, total: 0 });
  const [applicationsLoading, setApplicationsLoading] = useState(false);
  const [applicationsError, setApplicationsError] = useState<string | null>(null);

  const [applicationFilters, setApplicationFilters] = useState<{ page: number; limit: number; status?: JobApplicationStatus }>({ page: 1, limit: 10 });

  const [managerOfficeIdState, setManagerOfficeIdState] = useState<number | null>(getUserOfficeId());

  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccessMessage, setActionSuccessMessage] = useState<string | null>(null);

  const [searchText, setSearchText] = useState<string>("");

  const [selected, setSelected] = useState<JobApplication | null>(null);

  const clearMessages = () => {
    setActionError(null);
    setActionSuccessMessage(null);
    setApplicationsError(null);
  };

  const fetchApplications = async (opts = applicationFilters) => {
    setApplicationsLoading(true);
    setApplicationsError(null);
    try {
      const res = await recruitmentApi.getApplications({ page: opts.page, limit: opts.limit, status: opts.status });
      if (res) {
        const payload = (res as any).data ?? res;
        let list: any[] = [];
        if (payload) {
          if (Array.isArray(payload.list)) list = payload.list;
          else if (Array.isArray((payload as any).data)) list = (payload as any).data;
          else if (Array.isArray(payload)) list = payload;
        }

        if (getUserRole() === "manager") {
          if (!managerOfficeIdState) {
            try {
              const r: any = await officeApi.getManagerOffice();
              if (r && r.success && r.data && r.data.id) setManagerOfficeIdState(r.data.id);
            } catch (e) {
              // ignore
            }
          }

          if (managerOfficeIdState) {
            list = list.filter((a: any) => Number(a.officeId) === Number(managerOfficeIdState));
          }
        }

        setApplications(list);
        const totalForDisplay = (getUserRole() === "manager" && managerOfficeIdState) ? list.length : (payload?.pagination?.total || list.length);
        setApplicationsPagination({ page: payload?.pagination?.page || opts.page, limit: payload?.pagination?.limit || opts.limit, total: totalForDisplay });
      }
    } catch (err: any) {
      setApplicationsError(err?.message || "Lỗi khi tải hồ sơ");
    } finally {
      setApplicationsLoading(false);
    }
  };

  const updateApplicationStatus = async ({ id, status }: { id: number; status: JobApplicationStatus }) => {
    setActionLoading(true);
    setActionError(null);
    try {
      await recruitmentApi.updateApplicationStatus(id, { status });
      setActionSuccessMessage("Cập nhật trạng thái thành công");
    } catch (err: any) {
      setActionError(err?.message || "Lỗi khi cập nhật");
    } finally {
      setActionLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications(applicationFilters);
    return () => clearMessages();
  }, [applicationFilters.page, applicationFilters.limit, applicationFilters.status]);

  useEffect(() => {
    if (getUserRole() === "manager" && managerOfficeIdState) {
      fetchApplications(applicationFilters);
    }
  }, [managerOfficeIdState]);

  useEffect(() => {
    (async () => {
      if (getUserRole() === "manager" && !managerOfficeIdState) {
        try {
          const r: any = await officeApi.getManagerOffice();
          if (r && r.success && r.data && r.data.id) setManagerOfficeIdState(r.data.id);
        } catch (e) {
          // ignore
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
      fetchApplications(applicationFilters);
      setTimeout(() => setActionSuccessMessage(null), 1500);
    }
  }, [actionSuccessMessage]);

  const handleStatusFilterChange = (status?: JobApplicationStatus) => {
    setApplicationFilters((s) => ({ ...s, page: 1, status }));
  };

  const handleSearchChange = (value: string) => {
    setSearchText(value || "");
  };

  const handleRefresh = () => {
    fetchApplications(applicationFilters);
  };

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <style>{'.hr-recruitment-filter-actions .ant-btn-primary{display:none !important}'} </style>
        <SearchFilters
          status={applicationFilters.status}
          onStatusChange={handleStatusFilterChange}
          onRefresh={handleRefresh}
          onSearchChange={handleSearchChange}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <UsergroupAddOutlined className="title-icon" />
              <h3 className="list-page-title-main">Duyệt hồ sơ ứng tuyển</h3>
            </div>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Tổng hồ sơ: {applicationsPagination.total}</div>
            </div>
          </div>
          <div className="list-page-actions">
            {/* no create button for applications */}
          </div>
        </div>

        {applicationsError && <Alert type="error" message={applicationsError} showIcon />}

        <div className="list-page-table">
          <ApplicationTable
            data={(() => {
              if (!searchText) return applications;
              const q = searchText.toLowerCase();
              return applications.filter((a: any) => {
                const name = (a.fullName || "").toString().toLowerCase();
                const job = (a.jobTitle || "").toString().toLowerCase();
                const office = (a.officeName || (`#${a.officeId}`) || "").toString().toLowerCase();
                return name.includes(q) || job.includes(q) || office.includes(q);
              });
            })()}
            loading={applicationsLoading}
            actionLoading={actionLoading}
            currentPage={applicationsPagination.page}
            pageSize={applicationsPagination.limit}
            total={applicationsPagination.total}
            onPageChange={(page, limit) => setApplicationFilters((s) => ({ ...s, page, limit: limit || s.limit }))}
            onView={setSelected}
            onReviewing={(id) => updateApplicationStatus({ id, status: "REVIEWING" })}
            onApprove={(id) => updateApplicationStatus({ id, status: "APPROVED" })}
            onReject={(id) => updateApplicationStatus({ id, status: "REJECTED" })}
          />
        </div>
      </div>

      <ApplicationDetailModal selected={selected} onClose={() => setSelected(null)} />
    </div>
  );
};

export default ApplicationReviewPage;
