import { useEffect, useState } from "react";
import { Col, message, Row, Tag } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import EmployeeTable from "./components/Table";
import Actions from "./components/Actions";
import * as XLSX from "xlsx";
import { saveAs } from "file-saver";
import { useNavigate } from "react-router-dom";
import type { ManagerEmployeePerformanceData } from "../../../../types/employee";
import Title from "antd/es/typography/Title";
import { BarChartOutlined, TeamOutlined } from "@ant-design/icons";

const ManagerEmployeePerformance = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState("NONE");
  const [filterRole, setFilterRole] = useState("ALL");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterShift, setFilterShift] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [employeePerformances, setEmployeePerformances] = useState<ManagerEmployeePerformanceData[] | []>([]);

  const fetchEmployeePerfomances = (page = currentPage, search?: string) => {
    const payload: any = {
      page,
      limit: pageSize,
      searchText: search ?? searchText,
      sort: filterSort !== "NONE" ? filterSort : undefined,
      role: filterRole !== "ALL" ? filterRole : undefined,
    };
    if (dateRange) {
      payload.startDate = dateRange[0].startOf("day").toISOString();
      payload.endDate = dateRange[1].endOf("day").toISOString();
    }

    // dispatch(getEmployeePerformance(payload));
  };

  const handleViewEmployeeShipmentsDetail = (employeeId: number) => {
    navigate(`/employees/performance/${employeeId}/shipments`);
  };

  const handleExportEmployeesPerformance = async () => {
    try {
      const params: any = {
        searchText: searchText || undefined,
        sort: filterSort !== "NONE" ? filterSort : undefined,
        role: filterRole !== "ALL" ? filterRole : undefined,
      };

      if (dateRange) {
        params.startDate = dateRange[0].startOf("day").toISOString();
        params.endDate = dateRange[1].endOf("day").toISOString();
      }

      // const resultAction = await dispatch(exportEmployeePerformance(params));
      // const payload = resultAction.payload as any;
      // const data = Array.isArray(payload) ? payload : payload?.exportData ?? [];

      // if (data.length === 0) {
      //   return message.info("Không có dữ liệu để xuất Excel");
      // }

      // const exportData = data.map((t: any) => ({
      //   "Mã nhân viên": t.employeeId,
      //   "Tên nhân viên": t.name,
      //   "Chức vụ": t.role,
      //   "Tổng số chuyến giao": t.totalShipments,
      //   "Tổng số đơn giao": t.totalOrders,
      //   "Số đơn giao thành công": t.completedOrders,
      //   "Tỉ lệ đơn thành công (%)": t.completionRate?.toFixed(2),
      //   "Thời gian trung bình/đơn (phút)": t.avgTimePerOrder?.toFixed(2),
      // }));

      // const worksheet = XLSX.utils.json_to_sheet(exportData);

      // Chỉnh độ rộng cột
      // worksheet['!cols'] = [
      //   { wch: 15 }, // "Mã nhân viên"
      //   { wch: 20 }, // "Tên nhân viên"
      //   { wch: 15 }, // "Chức vụ"
      //   { wch: 20 }, // "Tổng số chuyến giao"
      //   { wch: 20 }, // "Tổng số đơn giao"
      //   { wch: 25 }, // "Số đơn giao thành công"
      //   { wch: 25 }, // "Tỉ lệ đơn thành công (%)"
      //   { wch: 30 }, // "Thời gian trung bình/đơn (phút)"
      // ];

      // const workbook = XLSX.utils.book_new();
      // XLSX.utils.book_append_sheet(workbook, worksheet, "Hiệu suất nhân viên");

      // const excelBuffer = XLSX.write(workbook, { bookType: "xlsx", type: "array" });
      // const blob = new Blob([excelBuffer], { type: "application/octet-stream" });

      // saveAs(blob, `HieuSuatNhanVien${dayjs().format("YYYYMMDD_HHmm")}.xlsx`);
    } catch (error) {
      console.error(error);
      message.error("Xuất Excel thất bại!");
    }
  };

  useEffect(() => {
    setCurrentPage(1);
    fetchEmployeePerfomances(currentPage);
  }, []);

  useEffect(() => {
    setCurrentPage(1);
    fetchEmployeePerfomances(1);
  }, [searchText, dateRange, filterSort, filterRole]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          setSearchText={setSearchText}
          dateRange={dateRange}
          setDateRange={setDateRange}
          filters={{ sort: filterSort, role: filterRole, status: filterStatus, shift: filterShift }}
          setFilters={(key, val) => {
            if (key === "sort") setFilterSort(val);
            if (key === "role") setFilterRole(val);
            if (key === "status") setFilterStatus(val);
            if (key === "shift") setFilterShift(val);
          }}
          onReset={() => {
            setSearchText("");
            setFilterSort("NONE");
            setFilterRole("ALL");
            setFilterStatus("ALL");
            setFilterShift("ALL");
            setDateRange(null);
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <BarChartOutlined className="title-icon" />
              Hiệu suất nhân viên
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions onExport={handleExportEmployeesPerformance} />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} nhân viên</Tag>

        <EmployeeTable
          data={employeePerformances}
          onDetail={handleViewEmployeeShipmentsDetail}
          currentPage={currentPage}
          pageSize={pageSize}
          total={total}
          loading={loading}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetchEmployeePerfomances(page);
          }}
        />
      </div>
    </div >
  );
};

export default ManagerEmployeePerformance;