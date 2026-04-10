import { useEffect, useMemo, useState } from "react";
import { Alert, Empty, Spin } from "antd";
import { useNavigate } from "react-router-dom";
import HeaderHome from "../../../components/common/HeaderHome";
import { Typography } from "antd";
import "../../hr/recruitment/styles/recruitment.css";
import companyInfoImage from "../../../assets/images/companyInfo.jpg";
import joblistImage from "../../../assets/images/joblist.png";
import JobCard from "../../hr/recruitment/components/JobCard";
import { JobDetailModal } from "./JobDetailPage";
import FooterHome from "../../../components/common/FooterHome";
import "./RecruitmentPage.css";
import recruitmentApi from "../../../api/recruitmentApi";
import type { JobPosting, RecruitmentRoleType } from "../../../types/recruitment";
import { roleTypeLabelMap } from "./recruitmentHelpers";

const { Title, Paragraph } = Typography;

export default function RecruitmentPage(): JSX.Element {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("Tất cả");
  const [jobs, setJobs] = useState<JobPosting[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const pageSize = 6;

  const roleToLabel = (roleType: RecruitmentRoleType) => roleTypeLabelMap[roleType] || roleType;

  const roles = useMemo(
    () => ["Tất cả", ...Array.from(new Set(jobs.map((j) => roleToLabel(j.roleType))))],
    [jobs]
  );

  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await recruitmentApi.getJobs({ page: 1, limit: 100 });
        if (res.success && res.data?.list) {
          setJobs(res.data.list);
          return;
        }

        setError(res.message || "Không tải được danh sách tuyển dụng");
      } catch (e: unknown) {
        const err = e as { response?: { data?: { message?: string } } };
        console.log(err?.response?.data);
        setError(err?.response?.data?.message || "Không tải được danh sách tuyển dụng");
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    let list = jobs.filter((j) =>
      (j.title + " " + (j.officeName || "")).toLowerCase().includes(q)
    );
    if (roleFilter !== "Tất cả") {
      list = list.filter((j) => roleToLabel(j.roleType) === roleFilter);
    }
    return list;
  }, [query, roleFilter, jobs]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const pageItems = filtered.slice((page - 1) * pageSize, page * pageSize);
  const [detailJobId, setDetailJobId] = useState<number | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);

  return (
    <>
      <HeaderHome />
      <div
        className="public-hero-banner"
        style={{
          backgroundImage: `url(${joblistImage}), url(${companyInfoImage})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      >
        <div className="public-hero-overlay" />
        <div className="public-hero-content">
          <Title className="public-hero-title" level={2}>
            Cơ hội nghề nghiệp Logistics
          </Title>
          <Paragraph className="public-hero-subtitle">
            Chọn vị trí phù hợp và nộp hồ sơ trực tuyến ngay trên hệ thống.
          </Paragraph>
        </div>
      </div>

      <main className="recruitment-page min-h-screen py-12">
        <div className="recruitment-container max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <header className="recruitment-header text-center mb-10">
            <h1 className="recruitment-title text-2xl sm:text-3xl lg:text-4xl font-extrabold leading-tight">
            Cơ hội nghề nghiệp Logistics
          </h1>
          <p className="recruitment-subtitle mt-2 max-w-2xl mx-auto text-sm sm:text-base">
            Chọn vị trí phù hợp và nộp hồ sơ trực tuyến ngay trên hệ thống.
          </p>
        </header>

        {/* Filters (centered card) - refactored below */}
          <div className="recruitment-filter-card-wrapper">
            <div className="recruitment-filter-card">
              <div className="recruitment-filter-row">
                <div className="recruitment-filter-left">
                  <label className="recruitment-filter-label" htmlFor="role-select">Lọc theo vị trí</label>
                  <select
                    id="role-select"
                    value={roleFilter}
                    onChange={(e) => { setRoleFilter(e.target.value); setPage(1); }}
                    className="recruitment-select"
                  >
                    {roles.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>

                  <span className="recruitment-filter-dot" aria-hidden>•</span>

                  <div className="recruitment-filter-count">
                    {loading ? "Đang tải..." : `${filtered.length} vị trí`}
                  </div>
                </div>

                <div className="recruitment-filter-right">
                  <div className="recruitment-search-group">
                    <label htmlFor="search" className="recruitment-search-label">Tìm kiếm</label>
                    <div className="recruitment-search-wrapper">
                      <input
                        id="search"
                        value={query}
                        onChange={(e) => { setQuery(e.target.value); setPage(1); }}
                        placeholder="Tìm theo tên vị trí, địa điểm..."
                        className="recruitment-search-input"
                      />
                      <svg
                        className="recruitment-search-icon"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        width={16}
                        height={16}
                        style={{ width: 16, height: 16 }}
                        aria-hidden="true"
                      >
                        <path strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35" />
                        <circle cx="11" cy="11" r="6" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

        {/* Job grid */}
        <section>
          {error && <Alert type="error" message={error} showIcon className="mb-6" />}

          <Spin spinning={loading}>
            <div className="recruitment-job-grid-wrapper">
              {pageItems.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-2 gap-8">
                  {pageItems.map((job) => (
                    <JobCard
                      key={job.id}
                      job={job}
                      onView={(id) => { setDetailJobId(id); setDetailVisible(true); }}
                      onApply={(id) => navigate(`/jobs/${id}/apply`)}
                    />
                  ))}
                </div>
              ) : (
                !loading && <Empty description="Không có tin tuyển dụng phù hợp" />
              )}
            </div>
          </Spin>

          <JobDetailModal visible={detailVisible} jobId={detailJobId ?? undefined} onClose={() => setDetailVisible(false)} />

          <div className="recruitment-pagination mt-10 flex items-center justify-center gap-3">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              className="recruitment-page-btn px-3 py-2 rounded-md border border-gray-200 bg-white text-sm shadow-sm disabled:opacity-50"
            >Trước</button>

            <div className="flex items-center gap-2">
              {Array.from({ length: totalPages }).map((_, i) => {
                const n = i + 1;
                return (
                  <button
                    key={n}
                    onClick={() => setPage(n)}
                    className={`recruitment-page-btn px-3 py-2 rounded-md text-sm ${page === n ? 'bg-[#1e3a8a] text-white' : 'bg-white border border-gray-200'}`}
                  >{n}</button>
                );
              })}
            </div>

            <button
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages || totalPages === 0}
              className="recruitment-page-btn px-3 py-2 rounded-md border border-gray-200 bg-white text-sm shadow-sm disabled:opacity-50"
            >Sau</button>
          </div>
        </section>
        </div>
      </main>

      <FooterHome />
    </>
  );
}
