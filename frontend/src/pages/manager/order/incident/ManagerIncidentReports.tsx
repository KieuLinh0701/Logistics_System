import { useEffect, useState } from "react";
import { Col, Row, Tag, message } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import Title from "antd/es/typography/Title";
import IncidentDetailModal from "./components/IncidentDetailModal";
import IncidentTable from "./components/Table";
import type { Incident } from "../../../../types/incidentReport";
import { HistoryOutlined } from "@ant-design/icons";

const ManagerIncidentReports = () => {
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
  const [mode, setMode] = useState<'view' | 'edit'>();

  const fetchIncidents = (page = currentPage, search?: string) => {
    const payload: any = {
      page,
      limit: pageSize,
      searchText: search ?? searchText,
      priority: filterPriority != "ALL" ? filterPriority : undefined,
      type: filterType !== "ALL" ? filterType : undefined,
      status: filterStatus !== "ALL" ? filterStatus : undefined,
      sort: filterSort !== "NEWEST" ? filterSort : undefined,
    };
    if (dateRange) {
      payload.startDate = dateRange[0].startOf("day").toISOString();
      payload.endDate = dateRange[1].endOf("day").toISOString();
    }
    // dispatch(listOfficeIncidents(payload));
  };

  const handleEditIncident = async (resolution: string, status: string) => {
    // try {
    //   if (!selectedIncident?.id) return;

    //   const resultAction = await dispatch(
    //     handleIncident({
    //       incidentId: selectedIncident.id,
    //       status,
    //       resolution,
    //     })
    //   ).unwrap();

    //   if (resultAction.success) {
    //     message.success(resultAction.message || "Xử lý báo cáo thành công");
    //     fetchIncidents(currentPage);
    //   } else {
    //     message.error(resultAction.message || "Xử lý báo cáo thất bại");
    //   }
    // } catch (error: any) {
    //   message.error(error.message || "Lỗi server khi xử lý báo cáo");
    // }
  };

  const handleViewIncident = (incident: Incident) => {
    setMode("view");
    setSelectedIncident(incident);
    setIsModalVisible(true);
  };

  const handleEditIncidentClick = (incident: Incident) => {
    setMode("edit");
    setSelectedIncident(incident);
    setIsModalVisible(true);
  };


  const handleModalClose = () => {
    setSelectedIncident(null);
    setIsModalVisible(false);
  };

  useEffect(() => {
    fetchIncidents();
  }, []);

  useEffect(() => {
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
          onEdit={handleEditIncidentClick}
        />

        <IncidentDetailModal
          incident={selectedIncident}
          visible={isModalVisible}
          onClose={handleModalClose}
          onUpdate={handleEditIncident}
          initialMode={mode}
        />
      </div>
    </div>
  );
};

export default ManagerIncidentReports;