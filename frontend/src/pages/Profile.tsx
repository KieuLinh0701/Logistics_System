import React, { useEffect, useState } from 'react';
import { Card, Button, Avatar, Typography, Row, Col, Tag, message, Modal, Form, Input, Upload, Divider } from 'antd';
import { UserOutlined, EditOutlined, MailOutlined, PhoneOutlined, IdcardOutlined, CheckCircleOutlined, CloseCircleOutlined, LoginOutlined, CalendarOutlined, EnvironmentOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { Select } from 'antd';
import axios from 'axios';
import type { City, Ward } from '../types/location';

const { Option } = Select;
const { Title, Text } = Typography;

// Dữ liệu mẫu để test
const MOCK_USER_DATA = {
  id: 1,
  email: 'nguyenvana@example.com',
  firstName: 'An',
  lastName: 'Nguyễn Văn',
  phoneNumber: '0912345678',
  role: 'user',
  isVerified: true,
  isActive: true,
  images: null,
  lastLoginAt: '2024-01-15T10:30:00.000Z',
  createdAt: '2024-01-01T00:00:00.000Z',
  detailAddress: '123 Đường Lê Lợi',
  codeCity: 79, // TP HCM
  codeWard: 27436 // Phường Bến Nghé
};

const MOCK_PROVINCES = [
  { code: 79, name: 'Thành phố Hồ Chí Minh' },
  { code: 1, name: 'Hà Nội' },
  { code: 48, name: 'Đà Nẵng' },
  { code: 89, name: 'An Giang' },
  { code: 77, name: 'Bà Rịa - Vũng Tàu' }
];

const MOCK_WARDS = [
  { code: 27436, name: 'Phường Bến Nghé' },
  { code: 27439, name: 'Phường Bến Thành' },
  { code: 27442, name: 'Phường Nguyễn Thái Bình' },
  { code: 27445, name: 'Phường Phạm Ngũ Lão' },
  { code: 27448, name: 'Phường Cầu Ông Lãnh' }
];

const Profile: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // Sử dụng dữ liệu mẫu nếu redux user chưa có
  const user = MOCK_USER_DATA;

  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();
  const [avatarPreview, setAvatarPreview] = useState<string | undefined>(undefined);
  const [avatarSrc, setAvatarSrc] = useState<string | undefined>(undefined);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);

  const [provinces, setProvinces] = useState<City[]>([]);
  const [wards, setWards] = useState<Ward[]>([]);
  const [selectedProvince, setSelectedProvince] = useState<number | null>(null);
  const [loadingWards, setLoadingWards] = useState(false);
  const [useMockData, setUseMockData] = useState(false);

  useEffect(() => {
    if (useMockData) {
      setProvinces(MOCK_PROVINCES);
      setWards(MOCK_WARDS);
      setSelectedProvince(MOCK_USER_DATA.codeCity);
    } else {
      axios.get<City[]>('https://provinces.open-api.vn/api/v2/p/')
        .then(res => setProvinces(res.data))
        .catch(err => {
          console.error(err);
          // Fallback to mock data if API fails
          setProvinces(MOCK_PROVINCES);
          setUseMockData(true);
        });
    }
  }, [useMockData]);

  const handleProvinceChange = async (provinceCode: number) => {
    setSelectedProvince(provinceCode);
    form.setFieldsValue({ codeWard: undefined });
    setWards([]);

    if (useMockData) {
      // Sử dụng mock wards
      setWards(MOCK_WARDS);
    } else {
      setLoadingWards(true);
      try {
        const res = await axios.get<City>(`https://provinces.open-api.vn/api/v2/p/${provinceCode}?depth=2`);
        setWards(res.data.wards || []);
      } catch (err) {
        console.error(err);
        // Fallback to mock data
        setWards(MOCK_WARDS);
      } finally {
        setLoadingWards(false);
      }
    }
  };

  const isFromOrderCreate = location.pathname.includes('/orders/create/edit-profile');

  useEffect(() => {
    if (user?.images) {
      // const fileName = user.images.split('/').pop();
      // setAvatarSrc(fileName ? `/uploads/${fileName}` : undefined);
      // setAvatarPreview(fileName ? `/uploads/${fileName}` : undefined);
    } else {
      setAvatarSrc(undefined);
      setAvatarPreview(undefined);
    }
  }, [user?.images]);

  useEffect(() => {
    if (open && form.getFieldValue('codeCity')) {
      const cityCode = form.getFieldValue('codeCity');
      setSelectedProvince(cityCode);

      if (useMockData) {
        setWards(MOCK_WARDS);
      } else {
        const fetchWards = async () => {
          setLoadingWards(true);
          try {
            const res = await axios.get<City>(`https://provinces.open-api.vn/api/v2/p/${cityCode}?depth=2`);
            setWards(res.data.wards || []);
          } catch (err) {
            console.error(err);
            setWards(MOCK_WARDS);
          } finally {
            setLoadingWards(false);
          }
        };
        fetchWards();
      }
    }
  }, [open, useMockData]);

  const handleUpdateSuccess = () => {
    message.success('Cập nhật thành công (Demo)');
    setOpen(false);

    if (isFromOrderCreate && user) {
      navigate(`/${user.role}/orders/create`);
    }
  };

  const handleUpdateProfile = async () => {
    try {
      const values = await form.validateFields();

      if (useMockData) {
        // Demo update với mock data
        console.log('Updated data:', values);
        message.info('Đây là bản demo. Dữ liệu thật sẽ được cập nhật khi kết nối backend.');
      } else {
        // await (dispatch(updateProfile({
        //   firstName: values.firstName,
        //   lastName: values.lastName,
        //   phoneNumber: values.phoneNumber,
        //   detailAddress: values.detailAddress,
        //   codeWard: values.codeWard ? Number(values.codeWard) : undefined,
        //   codeCity: values.codeCity ? Number(values.codeCity) : undefined,
        // }) as any)).unwrap();

        // if (avatarFile) {
        //   await (dispatch(updateAvatar(avatarFile) as any)).unwrap();
        //   setAvatarFile(null);
        // }
      }

      handleUpdateSuccess();
    } catch (e: any) {
      if (!e?.errorFields) {
        if (useMockData) {
          message.error('Cập nhật thất bại (Demo)');
        } else {
          message.error(e || 'Cập nhật thất bại');
        }
      }
    }
  };

  const openEditModal = async () => {
    form.setFieldsValue({
      firstName: user?.firstName,
      lastName: user?.lastName,
      phoneNumber: user?.phoneNumber,
      detailAddress: user?.detailAddress,
      codeWard: user?.codeWard,
      codeCity: user?.codeCity,
    });

    // setAvatarPreview(user?.images ? `/uploads/${user.images.split('/').pop()}` : undefined);

    if (user?.codeCity) {
      if (useMockData) {
        setSelectedProvince(user.codeCity);
        setWards(MOCK_WARDS);
      } else {
        setLoadingWards(true);
        setSelectedProvince(user.codeCity);
        try {
          const res = await axios.get<City>(`https://provinces.open-api.vn/api/v2/p/${user.codeCity}?depth=2`);
          setWards(res.data.wards || []);
        } catch (err) {
          console.error(err);
          setWards(MOCK_WARDS);
        } finally {
          setLoadingWards(false);
        }
      }
    }

    setOpen(true);
  };

  // Thêm indicator cho mock data
  const MockDataIndicator = () => (
    <Card
      style={{
        marginBottom: 16,
        backgroundColor: '#fff7e6',
        border: '1px solid #ffd591',
        borderRadius: '8px'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, justifyContent: 'center' }}>
        <Text style={{ color: '#d46b08', fontSize: '14px' }}>
          🚀 Đang sử dụng dữ liệu mẫu để demo
        </Text>
      </div>
    </Card>
  );

  // if (loading && !useMockData) {
  //   return (
  //     <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
  //       <Spin size="large" />
  //     </div>
  //   );
  // }

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
      {/* Indicator cho mock data */}
      {useMockData && <MockDataIndicator />}

      {/* Thông báo từ trang tạo order */}
      {isFromOrderCreate && (
        <Card
          style={{
            marginBottom: 32,
            backgroundColor: '#f0f7ff',
            border: '1px solid #91d5ff',
            borderRadius: '12px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              background: '#1890ff',
              borderRadius: '50%',
              width: 24,
              height: 24,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white',
              fontSize: '14px'
            }}>
              ⚠️
            </div>
            <Text style={{ color: '#0050b3', fontSize: '14px' }}>
              Vui lòng cập nhật địa chỉ trong hồ sơ cá nhân để sử dụng thông tin này.
              Sau khi cập nhật xong, bạn sẽ được chuyển về trang tạo đơn hàng.
            </Text>
          </div>
        </Card>
      )}

      {/* Card chính */}
      <Card
        style={{
          borderRadius: '16px',
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
          border: 'none',
          overflow: 'hidden'
        }}
      >
        {/* Header với avatar và thông tin cơ bản */}
        <div style={{ textAlign: 'center', padding: '32px 0' }}>
          <div style={{ position: 'relative', display: 'inline-block' }}>
            <Avatar
              size={120}
              src={avatarSrc}
              icon={<UserOutlined />}
              style={{
                border: '4px solid #f0f0f0',
                marginBottom: 16
              }}
            />
            <div
              style={{
                position: 'absolute',
                bottom: 20,
                right: 0,
                background: '#52c41a',
                borderRadius: '50%',
                width: 24,
                height: 24,
                border: '2px solid white',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <CheckCircleOutlined style={{ color: 'white', fontSize: '12px' }} />
            </div>
          </div>

          <Title level={2} style={{ marginBottom: 8, color: '#1f2937' }}>
            {user?.lastName || ""} {user?.firstName || ""}
          </Title>

          <Tag
            color={'#1d3090'}
            style={{
              fontSize: '14px',
              padding: '6px 16px',
              borderRadius: '20px',
              border: 'none'
            }}
          >
            {user?.role}
          </Tag>
        </div>

        <Divider style={{ margin: '32px 0' }} />

        {/* Thông tin chi tiết */}
        <div style={{ padding: '0 24px' }}>
          <Title level={4} style={{ marginBottom: 24, color: '#1f2937' }}>
            Thông tin cá nhân
          </Title>

          <Row gutter={[24, 16]}>
            <Col xs={24} sm={12}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <div style={{
                  background: '#f0f7ff',
                  borderRadius: '8px',
                  padding: '8px',
                  color: '#1890ff'
                }}>
                  <MailOutlined />
                </div>
                <div>
                  <Text strong style={{ display: 'block', color: '#6b7280', fontSize: '12px' }}>Email</Text>
                  <Text style={{ color: '#1f2937' }}>{user?.email}</Text>
                </div>
              </div>
            </Col>

            <Col xs={24} sm={12}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <div style={{
                  background: '#f6ffed',
                  borderRadius: '8px',
                  padding: '8px',
                  color: '#52c41a'
                }}>
                  <PhoneOutlined />
                </div>
                <div>
                  <Text strong style={{ display: 'block', color: '#6b7280', fontSize: '12px' }}>Số điện thoại</Text>
                  <Text style={{ color: '#1f2937' }}>{user?.phoneNumber || 'Chưa cập nhật'}</Text>
                </div>
              </div>
            </Col>

            <Col xs={24} sm={12}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <div style={{
                  background: '#fff7e6',
                  borderRadius: '8px',
                  padding: '8px',
                  color: '#fa8c16'
                }}>
                  <IdcardOutlined />
                </div>
                <div>
                  <Text strong style={{ display: 'block', color: '#6b7280', fontSize: '12px' }}>Trạng thái</Text>
                  <Tag color={user?.isVerified ? 'green' : 'red'} style={{ border: 'none', margin: 0 }}>
                    {user?.isVerified ? 'Đã xác thực' : 'Chưa xác thực'}
                  </Tag>
                </div>
              </div>
            </Col>

            <Col xs={24} sm={12}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <div style={{
                  background: user?.isActive ? '#f6ffed' : '#fff1f0',
                  borderRadius: '8px',
                  padding: '8px',
                  color: user?.isActive ? '#52c41a' : '#ff4d4f'
                }}>
                  {user?.isActive ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                </div>
                <div>
                  <Text strong style={{ display: 'block', color: '#6b7280', fontSize: '12px' }}>Hoạt động</Text>
                  <Text style={{ color: '#1f2937' }}>
                    {user?.isActive ? 'Đang hoạt động' : 'Đã khóa'}
                  </Text>
                </div>
              </div>
            </Col>

            {user?.lastLoginAt && (
              <Col xs={24} sm={12}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                  <div style={{
                    background: '#f9f0ff',
                    borderRadius: '8px',
                    padding: '8px',
                    color: '#722ed1'
                  }}>
                    <LoginOutlined />
                  </div>
                  <div>
                    <Text strong style={{ display: 'block', color: '#6b7280', fontSize: '12px' }}>Đăng nhập cuối</Text>
                    <Text style={{ color: '#1f2937' }}>
                      {new Date(user.lastLoginAt).toLocaleString('vi-VN')}
                    </Text>
                  </div>
                </div>
              </Col>
            )}

            <Col xs={24} sm={12}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <div style={{
                  background: '#f0f0f0',
                  borderRadius: '8px',
                  padding: '8px',
                  color: '#8c8c8c'
                }}>
                  <CalendarOutlined />
                </div>
                <div>
                  <Text strong style={{ display: 'block', color: '#6b7280', fontSize: '12px' }}>Ngày tạo</Text>
                  <Text style={{ color: '#1f2937' }}>
                    {new Date(user?.createdAt || '').toLocaleDateString('vi-VN')}
                  </Text>
                </div>
              </div>
            </Col>

            {/* Thêm địa chỉ nếu có */}
            {(user?.detailAddress || user?.codeCity) && (
              <Col xs={24}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 16 }}>
                  <div style={{
                    background: '#f0f7ff',
                    borderRadius: '8px',
                    padding: '8px',
                    color: '#1890ff',
                    marginTop: 4
                  }}>
                    <EnvironmentOutlined />
                  </div>
                  <div style={{ flex: 1 }}>
                    <Text strong style={{ display: 'block', color: '#6b7280', fontSize: '12px', marginBottom: 4 }}>Địa chỉ</Text>
                    <Text style={{ color: '#1f2937', display: 'block' }}>
                      {user?.detailAddress || 'Chưa cập nhật'}
                    </Text>
                    <Text style={{ color: '#6b7280', fontSize: '12px' }}>
                      {user?.codeCity ? `TP HCM - Quận 1 - ${wards.find(w => w.code === user.codeWard)?.name || 'Phường Bến Nghé'}` : 'Chưa cập nhật tỉnh/thành'}
                    </Text>
                  </div>
                </div>
              </Col>
            )}
          </Row>
        </div>

        {/* Nút chỉnh sửa */}
        <div style={{ textAlign: 'center', padding: '32px 0 16px' }}>
          <Button
            type="primary"
            icon={<EditOutlined />}
            size="large"
            style={{
              backgroundColor: "#1C3D90",
              borderColor: "#1C3D90",
              borderRadius: '8px',
              padding: '0 32px',
              height: '48px',
              fontSize: '16px',
              fontWeight: 600
            }}
            onClick={openEditModal}
          >
            Chỉnh sửa thông tin
          </Button>
        </div>
      </Card>

      {/* Modal chỉnh sửa */}
      <Modal
        centered
        title={
          <div style={{ textAlign: 'center', fontSize: '20px', fontWeight: 600, color: '#1f2937' }}>
            Chỉnh sửa thông tin {useMockData && '(Demo)'}
          </div>
        }
        open={open}
        onCancel={() => {
          // setOpen(false);
          // if (user?.images) {
          //   const fileName = user.images.split('/').pop();
          //   setAvatarPreview(fileName ? `/uploads/${fileName}` : undefined);
          // } else {
          //   setAvatarPreview(undefined);
          // }
          // setAvatarFile(null);
        }}
        onOk={handleUpdateProfile}
        okText={isFromOrderCreate ? "Cập nhật và quay lại" : "Cập nhật"}
        okButtonProps={{
          style: {
            backgroundColor: '#1C3D90',
            borderColor: '#1C3D90',
            color: '#fff',
            borderRadius: '8px',
            height: '40px',
            padding: '0 24px'
          },
        }}
        cancelButtonProps={{
          style: {
            borderColor: '#d1d5db',
            color: '#6b7280',
            borderRadius: '8px',
            height: '40px',
            padding: '0 24px'
          },
        }}
        width={480}
        styles={{
          body: { padding: '24px' }
        }}
      >
        <Form form={form} layout="vertical">
          {/* Avatar upload */}
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}>
            <div style={{ textAlign: 'center' }}>
              <Upload
                accept="image/*"
                showUploadList={false}
                beforeUpload={() => false}
                onChange={async (info) => {
                  const raw = info.file as any;
                  const blob: Blob | undefined = (raw?.originFileObj as File) || (raw as Blob);
                  if (blob && blob instanceof Blob) {
                    const url = URL.createObjectURL(blob);
                    setAvatarPreview(url);
                    const file: File = blob instanceof File ? blob : new File([blob], 'avatar.jpg', { type: blob.type || 'image/jpeg' });
                    setAvatarFile(file);
                  }
                }}
              >
                <div style={{ position: 'relative', display: 'inline-block' }}>
                  <Avatar
                    size={80}
                    src={avatarPreview}
                    icon={<UserOutlined />}
                    style={{ cursor: 'pointer' }}
                  />
                  <div style={{
                    position: 'absolute',
                    bottom: 0,
                    right: 0,
                    background: '#1C3D90',
                    borderRadius: '50%',
                    width: 24,
                    height: 24,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    cursor: 'pointer'
                  }}>
                    <EditOutlined style={{ color: 'white', fontSize: '12px' }} />
                  </div>
                </div>
              </Upload>
              <Text style={{ display: 'block', marginTop: 8, color: '#6b7280', fontSize: '12px' }}>
                Click để thay đổi ảnh đại diện
              </Text>
            </div>
          </div>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="firstName"
                label="Tên"
                rules={[{ required: true, message: 'Vui lòng nhập tên' }]}
              >
                <Input style={{ borderRadius: '6px' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="lastName"
                label="Họ"
                rules={[{ required: true, message: 'Vui lòng nhập họ' }]}
              >
                <Input style={{ borderRadius: '6px' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="phoneNumber"
            label="Số điện thoại"
            rules={[{ required: true, message: 'Vui lòng nhập số điện thoại' }]}
          >
            <Input style={{ borderRadius: '6px' }} />
          </Form.Item>

          <Form.Item name="detailAddress" label="Địa chỉ chi tiết">
            <Input.TextArea
              rows={2}
              style={{ borderRadius: '6px' }}
              placeholder="Nhập số nhà, tên đường..."
            />
          </Form.Item>

          <Form.Item
            name="codeCity"
            label="Tỉnh/Thành phố"
            rules={[{ required: true, message: 'Chọn tỉnh/thành phố' }]}
          >
            <Select
              placeholder="Chọn tỉnh/thành phố"
              showSearch
              optionFilterProp="label"
              onChange={handleProvinceChange}
              style={{ borderRadius: '6px' }}
            >
              {provinces.map(p => (
                <Option key={p.code} value={p.code} label={p.name}>
                  {p.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="codeWard"
            label="Phường/Xã"
            rules={[{ required: true, message: 'Chọn phường/xã' }]}
          >
            <Select
              placeholder="Chọn phường/xã"
              disabled={!selectedProvince}
              loading={loadingWards}
              showSearch
              optionFilterProp="label"
              style={{ borderRadius: '6px' }}
            >
              {wards.map(w => (
                <Option key={w.code} value={w.code} label={w.name}>
                  {w.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          {/* Demo notice */}
          {useMockData && (
            <div style={{
              background: '#fff7e6',
              border: '1px solid #ffd591',
              borderRadius: '6px',
              padding: '12px',
              marginTop: '16px'
            }}>
              <Text style={{ color: '#d46b08', fontSize: '12px' }}>
                💡 Đây là bản demo. Dữ liệu sẽ không được lưu thật.
              </Text>
            </div>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default Profile;