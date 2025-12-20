import { useEffect, useState } from "react";
import { Col, Row, Tag, message } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import Title from "antd/es/typography/Title";
import IncidentDetailModal from "./components/IncidentDetailModal";
import IncidentTable from "./components/Table";
import type { Incident } from "../../../../types/incidentReport";
import { HistoryOutlined } from "@ant-design/icons";
import type { SearchRequest } from "../../../../types/request";
import incidentReportApi from "../../../../api/incidentReportApi";
import { useNavigate, useSearchParams } from "react-router-dom";
import "./ManagerIncidentReport.css"
import ProcessingModal from "./components/ProcessingModal";

const ManagerIncidentReports = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [loading, setLoading] = useState(false);
  const [incidents, setIncidents] = useState<Incident[] | []>([]);

  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [searchText, setSearchText] = useState("");
  const [filterType, setFilterType] = useState("ALL");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterSort, setFilterSort] = useState("NEWEST");
  const [filterPriority, setFilterPriority] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [selectedIncident, setSelectedIncident] = useState<Incident | null>(null);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterPriority !== "ALL") params.priority = filterPriority.toLowerCase();
    if (filterType !== "ALL") params.type = filterType.toLowerCase();
    params.sort = filterSort.toLowerCase();
    if (currentPage) params.page = currentPage;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const s = searchParams.get("search");
    const st = searchParams.get("status")?.toLocaleUpperCase();
    const type = searchParams.get("type")?.toLocaleUpperCase();
    const pr = searchParams.get("priority")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    if (s) setSearchText(s);
    if (st) setFilterStatus(st);
    if (type) setFilterType(type);
    if (pr) setFilterPriority(pr);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetchIncidents = async (page = currentPage) => {
    const param: SearchRequest = {
      page,
      limit: pageSize,
      search: searchText,
      priority: filterPriority != "ALL" ? filterPriority : undefined,
      type: filterType !== "ALL" ? filterType : undefined,
      status: filterStatus !== "ALL" ? filterStatus : undefined,
      sort: filterSort,
    };
    if (dateRange) {
      param.startDate = dateRange[0].startOf("day").toISOString();
      param.endDate = dateRange[1].endOf("day").toISOString();
    }
    try {
      const result = await incidentReportApi.listManagerIncidentReports(param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setIncidents(list);
        setTotal(result.data.pagination?.total || 0);
        setCurrentPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách yêu cầu");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách yêu cầu");
    } finally {
      setLoading(false);
    }
  };

  const handleViewIncident = async (id: number) => {
    try {
      setLoading(true);

      const result = await incidentReportApi.getManagerIncidentReportById(id);
      if (result.success && result.data) {
        setSelectedIncident(result.data);
        setIsModalVisible(true);
        // form.setFieldsValue(result.data);
      } else {
        message.error(result.message || "Lỗi khi lấy báo cáo");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy báo cáo");
      setSelectedIncident(null);
    } finally {
      setLoading(false);
    }
  };

  const handleEditIncidentFromTable = async (id: number) => {
    try {
      setLoading(true);

      const result = await incidentReportApi.getManagerIncidentReportById(id);
      if (result.success && result.data) {
        setSelectedIncident(result.data);
        setIsModalOpen(true);
      } else {
        message.error(result.message || "Lỗi khi lấy báo cáo");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy báo cáo");
      setSelectedIncident(null);
    } finally {
      setLoading(false);
    }
  };


  const handleModalClose = () => {
    setSelectedIncident(null);
    setIsModalVisible(false);
  };

  const handleViewOrderDetail = (trackingNumber: string) => {
    navigate(`/orders/tracking/${trackingNumber}`);
  };

  const handleEditFromDetail = () => {
    setIsModalOpen(true);
  };

  const handleEditSuccess = async () => {
    await fetchIncidents(currentPage);

    if (isModalOpen && selectedIncident) {
      try {
        const result = await incidentReportApi.getManagerIncidentReportById(selectedIncident.id);
        if (result.success && result.data) {
          setSelectedIncident(result.data);
        } else {
          message.error(result.message || "Cập nhật chi tiết thất bại");
        }
      } catch (error: any) {
        message.error(error.message || "Cập nhật chi tiết thất bại");
      }
    }

    setIsModalOpen(false);
  };

  useEffect(() => {
    fetchIncidents();
  }, []);

  useEffect(() => {
    updateURL();
    setCurrentPage(1);
    fetchIncidents(1);
  }, [searchText, filterType, dateRange, filterSort, filterStatus]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          setSearchText={setSearchText}
          dateRange={dateRange}
          setDateRange={setDateRange}
          filters={{ type: filterType, sort: filterSort, status: filterStatus, priority: filterPriority }}
          setFilters={(key, val) => {
            if (key === "type") setFilterType(val);
            if (key === "status") setFilterStatus(val);
            if (key === "sort") setFilterSort(val);
            if (key === "priority") setFilterPriority(val);
          }}
          onReset={() => {
            setSearchText("");
            setFilterType("ALL");
            setFilterStatus("ALL");
            setFilterSort("NEWEST");
            setFilterPriority("ALL");
            setDateRange(null);
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <HistoryOutlined className="title-icon" />
              Danh sách sự cố
            </Title>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} sự cố</Tag>

        <IncidentTable
          incidents={incidents}
          onViewIncident={handleViewIncident}
          currentPage={currentPage}
          pageSize={pageSize}
          total={total}
          loading={loading}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetchIncidents(page);
          }}
          onEdit={handleEditIncidentFromTable}
        />

        <IncidentDetailModal
          incident={selectedIncident}
          visible={isModalVisible}
          onClose={handleModalClose}
          loading={loading}
          onEdit={handleEditFromDetail}
          onViewOrderDetail={handleViewOrderDetail}
        />

        <ProcessingModal
          open={!!selectedIncident && isModalOpen}
          data={selectedIncident!}
          onSuccess={handleEditSuccess}
          onCancel={() => setIsModalOpen(false)}
        />
      </div>
    </div>
  );
};

export default ManagerIncidentReports;