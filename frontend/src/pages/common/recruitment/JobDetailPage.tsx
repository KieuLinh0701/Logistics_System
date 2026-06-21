import {useEffect, useState} from "react";
import {Alert, Button, Descriptions, Modal, Space, Spin, Tag, Typography} from "antd";
import {LeftOutlined} from "@ant-design/icons";
import {useNavigate, useParams} from "react-router-dom";
import HeaderHome from "../../../components/common/HeaderHome";
import "../../hr/recruitment/styles/recruitment.css";
import companyInfoImage from "../../../assets/images/companyInfo.jpg";
import joblistImage from "../../../assets/images/joblist.png";
import FooterHome from "../../../components/common/FooterHome";
import recruitmentApi from "../../../api/recruitmentApi";
import type {JobPosting} from "../../../types/recruitment";
import {postingStatusColorMap, postingStatusLabelMap, roleTypeLabelMap} from "./recruitmentHelpers";
import {shiftLabel} from "../../../utils/recruitmentHelpers";

const { Title, Paragraph } = Typography;

const JobDetailModal: React.FC<{
  visible: boolean;
  jobId?: number | null;
  onClose: () => void;
}> = ({ visible, jobId, onClose }) => {
  const [job, setJob] = useState<JobPosting | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDetail = async () => {
      if (!visible) return;
      if (!jobId || Number.isNaN(jobId) || jobId <= 0) {
        setError("ID công việc không hợp lệ");
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const res = await recruitmentApi.getJobById(jobId);
        if (res.success && res.data) {
          setJob(res.data);
          return;
        }
        setError(res.message || "Không tải được chi tiết tin tuyển dụng");
      } catch (e: any) {
        console.log(e?.response?.data);
        setError(e?.response?.data?.message || "Không tải được chi tiết tin tuyển dụng");
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [visible, jobId]);

  return (
    <Modal
      open={visible}
      onCancel={onClose}
      onOk={onClose}
      width={820}
      footer={null}
      title={<div className="modal-title">{job ? job.title : "Chi tiết tuyển dụng"}</div>}
    >
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}
      <Spin spinning={loading}>
        {job ? (
          <div>
            <Tag color={postingStatusColorMap[job.status]} style={{ marginBottom: 12 }}>
              {postingStatusLabelMap[job.status]}
            </Tag>

            <Descriptions bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Vị trí tuyển dụng">{roleTypeLabelMap[job.roleType]}</Descriptions.Item>
              <Descriptions.Item label="Bưu cục">{job.officeName || `#${job.officeId}`}</Descriptions.Item>
              <Descriptions.Item label="Số lượng tuyển">{job.quantityNeeded ?? 'Đang cập nhật'}</Descriptions.Item>
              <Descriptions.Item label="Ca làm việc">{shiftLabel(job.shift)}</Descriptions.Item>
              <Descriptions.Item label="Ngày đăng">{new Date(job.createdAt).toLocaleDateString("vi-VN")}</Descriptions.Item>
              <Descriptions.Item label="Cập nhật gần nhất">{new Date(job.updatedAt).toLocaleString("vi-VN")}</Descriptions.Item>
            </Descriptions>

            <Typography.Title level={5}>Mô tả công việc</Typography.Title>
            <Typography.Paragraph style={{ whiteSpace: "pre-wrap" }}>{job.description}</Typography.Paragraph>

            <div className="public-job-actions" style={{ marginTop: 12, display: "flex", gap: 8, justifyContent: "flex-end" }}>
              <Button type="primary" onClick={() => (window.location.href = `/jobs/${job.id}/apply`)}>
                Ứng tuyển ngay
              </Button>
            </div>
          </div>
        ) : (
          !loading && <Alert type="warning" showIcon message="Không tìm thấy tin tuyển dụng" />
        )}
      </Spin>
    </Modal>
  );
};

const JobDetailPage = () => {
  const { id } = useParams();
  const jobId = Number(id);
  const navigate = useNavigate();

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
            Chi tiết tuyển dụng
          </Title>
          <Paragraph className="public-hero-subtitle">Xem đầy đủ thông tin vị trí trước khi ứng tuyển</Paragraph>
        </div>
      </div>

      <main className="recruitment-page min-h-screen py-12">
        <div className="recruitment-container max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Button icon={<LeftOutlined />} onClick={() => navigate(-1)}>
              Quay lại
            </Button>
            <JobDetailModal visible={true} jobId={jobId} onClose={() => navigate(-1)} />
          </Space>
        </div>
      </main>

      <FooterHome />
    </>
  );
};

export { JobDetailModal };
export default JobDetailPage;
