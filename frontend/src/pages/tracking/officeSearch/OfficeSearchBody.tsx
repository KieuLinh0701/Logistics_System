import React, { useEffect, useState } from "react";
import { Form, Select, Input, Button, Typography, Card, Row, Col, message, Spin, Pagination } from "antd";
import { SearchOutlined, EnvironmentOutlined, PhoneOutlined, MailOutlined, ClockCircleOutlined } from "@ant-design/icons";
import "./OfficeSearch.css";
import type { City, Ward } from "../../../types/location";
import locationApi from "../../../api/locationApi";
import type { Office } from "../../../types/office";
import officeApi from "../../../api/officeApi";
import { translateOfficeType } from "../../../utils/officeUtils";

const { Option } = Select;
const { Title, Text } = Typography;

const OfficeSearchBody: React.FC = () => {
  const [form] = Form.useForm();
  const [cities, setCities] = useState<City[]>([]);
  const [wards, setWards] = useState<Ward[]>([]);
  const [offices, setOffices] = useState<Office[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedCity, setSelectedCity] = useState<number | null>(null);
  const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);
  const [total, setTotal] = useState(0);

  const fetchCities = async () => {
    try {
      setLoading(true);
      const response = await locationApi.getCities();
      if (response) {
        setCities(response);
      }
    } catch (error) {
      console.error("Error fetching Cities:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCities();
  }, []);

  const fetchWards = async (codeCity: number) => {
    try {
      setLoading(true);
      const response = await locationApi.getWardsByCity(codeCity);
      if (response) {
        setWards(response);
      }
    } catch (error) {
      console.error("Error fetching Wards:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleCityChange = (codeCity: number) => {
    setSelectedCity(codeCity);
    setWards([]);
    form.setFieldsValue({ ward: undefined });
    fetchWards(codeCity);
  };

  const searchOffices = async (values: any) => {
    setLoading(true);
    try {
      const response = await officeApi.searchOffice({
        search: values.search,
        city: values.city,
        ward: values.ward
      });
      if (response.success && response.data) {
        setOffices(response.data);
        if (response.data.length > 0) {
          setSelectedOffice(response.data[0]);
          setTotal(response.data.length);
        } else {
          setSelectedOffice(null);
        }
      } else {
        message.error(response.message || "Có lỗi xảy ra khi tìm kiếm bưu cục");
      }
    } catch (error) {
      message.error("Có lỗi xảy ra khi tìm kiếm bưu cục");
    } finally {
      setLoading(false);
    }
  };

  const handleOfficeSelect = (office: Office) => {
    setSelectedOffice(office);
  };

  const getVietnamMapUrl = () => {
    return "https://www.google.com/maps?q=Vietnam&output=embed";
  };

  const getOfficeMapUrl = (office: Office) => {
    return `https://www.google.com/maps?q=${office.latitude},${office.longitude}&z=15&output=embed`;
  };

  const getAddressOffice = (office: Office) => {
    const city = cities.find(c => c.code === office.cityCode)?.name || "";
    const ward = wards.find(w => w.code === office.wardCode)?.name || "";
    return `${office.detail}, ${ward}, ${city}`;
  };

  return (
    <div className="office-search-container">
      <div className="office-search-layout">
        {/* Left Side - Search Form and Results */}
        <div className="office-search-left">
          <div className="office-search-section">
            <Title level={2} className="office-search-title">
              Tra cứu bưu cục
            </Title>

            <Card className="office-search-card">
              <Form
                form={form}
                layout="vertical"
                onFinish={(values) => searchOffices(values)}
                className="office-search-form"
              >
                <div className="office-search-form-vertical">
                  <Form.Item
                    name="city"
                    label={<span className="office-search-form-label">Tỉnh/Thành phố</span>}
                    rules={[{ required: true, message: "Chọn tỉnh/thành phố!" }]}
                  >
                    <Select
                      placeholder="Chọn tỉnh/thành phố"
                      size="large"
                      onChange={handleCityChange}
                      showSearch
                      filterOption={(input, option) =>
                        (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
                      }
                      optionFilterProp="label"
                    >
                      {cities.map((c) => (
                        <Option key={c.code} value={c.code} label={c.name}>
                          {c.name}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>

                  <Form.Item
                    name="ward"
                    label={<span className="office-search-form-label">Xã/Phường</span>}
                  >
                    <Select
                      placeholder="Chọn xã/phường"
                      size="large"
                      disabled={!selectedCity}
                      showSearch
                      filterOption={(input, option) =>
                        (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
                      }
                      optionFilterProp="label"
                    >
                      {wards.map((w) => (
                        <Option key={w.code} value={w.code} label={w.name}>
                          {w.name}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>

                  <Form.Item
                    name="search"
                    label={<span className="office-search-form-label">Tìm kiếm</span>}
                  >
                    <Input
                      placeholder="Tên bưu cục, địa chỉ..."
                      size="large"
                      prefix={<SearchOutlined />}
                      allowClear
                    />
                  </Form.Item>
                </div>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    className="office-search-btn"
                  >
                    Tìm kiếm
                  </Button>
                </Form.Item>
              </Form>
            </Card>

            {loading && (
              <div className="office-search-loading">
                <Spin size="large" />
                <div className="office-search-loading-text">Đang tìm kiếm bưu cục...</div>
              </div>
            )}

            {offices.length > 0 && (
              <div className="office-search-results">
                <Title level={4} className="office-search-results-title">
                  Kết quả tìm kiếm ({total} bưu cục)
                </Title>
                <div className="office-cards-scroll-container">
                  <div className="office-cards-container">
                    {offices.map((office) => (
                      <Card
                        key={office.id}
                        className={`office-service-card ${selectedOffice?.id === office.id ? 'office-service-card-selected' : ''}`}
                        onClick={() => handleOfficeSelect(office)}
                      >
                        <div className="office-service-content">
                          <div className="office-service-header">
                            <Title level={5} className="office-service-name">
                              {office.name}
                            </Title>
                            <Text className="office-service-type">
                              {translateOfficeType(office.type)}
                            </Text>
                          </div>

                          <div className="office-service-details">
                            <div className="office-service-detail">
                              <EnvironmentOutlined className="office-service-icon" />
                              <Text className="office-service-text">{getAddressOffice(office)}</Text>
                            </div>

                            <div className="office-service-detail">
                              <MailOutlined className="office-service-icon" />
                              <Text className="office-service-text">{office.email}</Text>
                            </div>
                          </div>

                          <div className="office-service-footer">
                            <div className="office-service-time">
                              <ClockCircleOutlined className="office-service-icon" />
                              <Text className="office-service-text">
                                {office.openingTime} - {office.closingTime}
                              </Text>
                            </div>
                            <Button
                              className="office-service-call-btn"
                              onClick={(e) => {
                                e.stopPropagation();
                                window.open(`tel:${office.phoneNumber}`);
                              }}
                            >
                              <PhoneOutlined />
                              {office.phoneNumber}
                            </Button>
                          </div>
                        </div>
                      </Card>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Side - Map */}
        <div className="office-search-right">
          <Card className="office-map-card">
            <Title level={4} className="office-map-title">
              {selectedOffice ? selectedOffice.name : "Bản đồ Việt Nam"}
            </Title>
            <div className="office-map-container">
              {selectedOffice ? (
                <iframe
                  className="office-map-iframe"
                  src={getOfficeMapUrl(selectedOffice)}
                  allowFullScreen
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
              ) : (
                <iframe
                  className="office-map-iframe"
                  src={getVietnamMapUrl()}
                  allowFullScreen
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default OfficeSearchBody;