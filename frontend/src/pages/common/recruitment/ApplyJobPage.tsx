import React, {useEffect, useState} from "react";
import {Alert, Button, Card, Form, Input, message, Space, Spin, Typography, Upload} from "antd";
import {LeftOutlined, SendOutlined} from "@ant-design/icons";
import {useNavigate, useParams} from "react-router-dom";
import HeaderHome from "../../../components/common/HeaderHome";
import "./ApplyJobPage.css";
import "../../hr/recruitment/styles/recruitment.css";
import companyInfoImage from "../../../assets/images/companyInfo.jpg";
import joblistImage from "../../../assets/images/joblist.png";
import FooterHome from "../../../components/common/FooterHome";
import recruitmentApi from "../../../api/recruitmentApi";
import type {JobPosting} from "../../../types/recruitment";
import type {RcFile} from "antd/es/upload/interface";

const {Title, Paragraph} = Typography;

const ApplyJobPage: React.FC = () => {
    const [form] = Form.useForm();
    const {id} = useParams();
    const jobId = Number(id);
    const navigate = useNavigate();
    const [job, setJob] = useState<JobPosting | null>(null);
    const [loading, setLoading] = useState(false);
    const [submitLoading, setSubmitLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [cvFile, setCvFile] = useState<RcFile | null>(null);

    useEffect(() => {
        const fetchDetail = async () => {
            if (Number.isNaN(jobId) || jobId <= 0) {
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
                setError(res.message || "Không tải được thông tin công việc");
            } catch (e: unknown) {
                const err = e as { response?: { data?: { message?: string } } };
                console.log(err?.response?.data);
                setError(err?.response?.data?.message || "Không tải được thông tin công việc");
            } finally {
                setLoading(false);
            }
        };

        fetchDetail();
    }, [jobId]);

    const onFinish = async (values: {
        fullName: string;
        phone: string;
        email: string;
        address: string;
        coverLetter?: string;
    }) => {
        if (!cvFile) {
            message.error("Vui lòng upload file CV của bạn");
            return;
        }

        try {
            setSubmitLoading(true);

            const formData = new FormData();

            formData.append("fullName", values.fullName);
            formData.append("phone", values.phone);
            formData.append("email", values.email);
            formData.append("address", values.address);
            formData.append("coverLetter", values.coverLetter || "");
            formData.append("jobPostingId", jobId.toString());
            formData.append("cvFile", cvFile);

            await recruitmentApi.createApplication(formData);
            message.success("Nộp hồ sơ thành công");
            form.resetFields();
            setCvFile(null);
            navigate("/jobs", {replace: true});
        } catch (e: unknown) {
            const err = e as { response?: { data?: { message?: string } } };
            console.log(err?.response?.data);
            message.error(err?.response?.data?.message || "Nộp hồ sơ thất bại");
        } finally {
            setSubmitLoading(false);
        }
    };

    return (
        <>
            <HeaderHome/>
            <div
                className="public-hero-banner"
                style={{
                    backgroundImage: `url(${joblistImage}), url(${companyInfoImage})`,
                    backgroundSize: "cover",
                    backgroundPosition: "center",
                }}
            >
                <div className="public-hero-overlay"/>
                <div className="public-hero-content">
                    <Title className="public-hero-title" level={2}>
                        Ứng tuyển vị trí
                    </Title>
                    <Paragraph className="public-hero-subtitle">Hoàn thiện hồ sơ để ứng tuyển nhanh chóng</Paragraph>
                </div>
            </div>

            <main className="apply-job-page min-h-screen py-12">
                <div className="recruitment-container apply-job-container max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                    <Space direction="vertical" size={16} style={{width: "100%"}}>
                        <Button icon={<LeftOutlined/>} onClick={() => navigate(-1)}>
                            Quay lại
                        </Button>

                        {error && <Alert type="error" message={error} showIcon/>}

                        <Spin spinning={loading}>
                            <Card>
                                <Title level={3} className="apply-page-title">
                                    Nộp hồ sơ ứng tuyển
                                </Title>
                                <Paragraph type="secondary" style={{marginBottom: 0}}>
                                    {job ? `Vị trí: ${job.title}` : "Vui lòng điền đầy đủ thông tin ứng tuyển"}
                                </Paragraph>
                            </Card>

                            <Card>
                                <Form layout="vertical" form={form} onFinish={onFinish}>
                                    <Form.Item
                                        name="fullName"
                                        label="Họ và tên"
                                        rules={[{required: true, message: "Vui lòng nhập họ tên"}, {max: 150}]}
                                    >
                                        <Input placeholder="Nguyễn Văn A"/>
                                    </Form.Item>

                                    <Form.Item
                                        name="phone"
                                        label="Số điện thoại"
                                        rules={[
                                            {required: true, message: "Vui lòng nhập số điện thoại"},
                                            {
                                                pattern: /^[0-9+()\-\s]{8,20}$/,
                                                message: "Số điện thoại không hợp lệ",
                                            },
                                        ]}
                                    >
                                        <Input placeholder="0900000000"/>
                                    </Form.Item>

                                    <Form.Item
                                        name="email"
                                        label="Email"
                                        rules={[
                                            {required: true, message: "Vui lòng nhập email"},
                                            {type: "email", message: "Email không hợp lệ"},
                                        ]}
                                    >
                                        <Input placeholder="example@email.com"/>
                                    </Form.Item>

                                    <Form.Item
                                        name="address"
                                        label="Địa chỉ"
                                        rules={[{required: true, message: "Vui lòng nhập địa chỉ"}, {max: 255}]}
                                    >
                                        <Input.TextArea rows={3}
                                                        placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"/>
                                    </Form.Item>

                                    <Form.Item
                                        name="cvUrl"
                                        label="CV URL"
                                        rules={[
                                            {max: 500},
                                        ]}
                                    >
                                        <Input
                                            placeholder="https://.../cv.pdf (tuỳ chọn nếu đã upload file CV bên dưới)"/>
                                    </Form.Item>

                                    <Form.Item label="CV upload" required>
                                        <Upload.Dragger
                                            accept=".pdf"
                                            maxCount={1}
                                            beforeUpload={(file) => {
                                              if (file.type !== 'application/pdf') {
                                                message.error('Chỉ chấp nhận file định dạng PDF!');
                                                return Upload.LIST_IGNORE;
                                              }
                                                setCvFile(file);
                                                return false;
                                            }}
                                            onRemove={() => {
                                                setCvFile(null);
                                            }}
                                        >
                                            <p>Kéo/thả file CV PDF hoặc bấm để chọn file</p>
                                        </Upload.Dragger>
                                        {cvFile ? (
                                            <p style={{marginTop: 8, color: "#64748b"}}>Đã chọn: {cvFile.name}</p>
                                        ) : (
                                            <p style={{marginTop: 8, color: "#64748b"}}>* Vui lòng chọn file CV</p>
                                        )}
                                    </Form.Item>

                                    <Form.Item
                                        name="coverLetter"
                                        label="Cover letter"
                                        rules={[{max: 2000, message: "Cover letter không vượt quá 2000 ký tự"}]}
                                    >
                                        <Input.TextArea rows={4}
                                                        placeholder="Giới thiệu ngắn gọn về kinh nghiệm và điểm mạnh của bạn"/>
                                    </Form.Item>

                                    <Button
                                        loading={submitLoading}
                                        type="primary"
                                        htmlType="submit"
                                        icon={<SendOutlined/>}
                                    >
                                        Gửi hồ sơ
                                    </Button>
                                </Form>
                            </Card>
                        </Spin>
                    </Space>
                </div>
            </main>

            <FooterHome/>
        </>
    );
};

export default ApplyJobPage;
