import { useEffect, useRef, useState } from "react";
import { Col, message, Row, Tag } from "antd";
import SearchFilters from "./components/SearchFilters";
import EmployeeTable from "./components/Table";
import Actions from "./components/Actions";
import { useNavigate, useSearchParams } from "react-router-dom";
import type { ManagerEmployeePerformanceData } from "../../../../types/employee";
import Title from "antd/es/typography/Title";
import { BarChartOutlined } from "@ant-design/icons";
import employeeApi from "../../../../api/employeeApi";

const ManagerEmployeePerformance = () => {
  const navigate = useNavigate();
  const latestRequestRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterRole, setFilterRole] = useState("ALL");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterShift, setFilterShift] = useState("ALL");
  const [employeePerformances, setEmployeePerformances] = useState<ManagerEmployeePerformanceData[] | []>([]);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterShift !== "ALL") params.shift = filterShift.toLowerCase();
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterRole !== "ALL") params.role = filterRole;
    if (currentPage) params.page = currentPage;

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const pageParam = Number(searchParams.get("page")) || 1;
    const s = searchParams.get("search");
    const shift = searchParams.get("shift")?.toLocaleUpperCase();
    const status = searchParams.get("status")?.toLocaleUpperCase();
    const r = searchParams.get("role");

    setCurrentPage(pageParam);
    if (s) setSearchText(s);
    if (shift) setFilterShift(shift);
    if (status) setFilterStatus(status);
    if (r) setFilterRole(r);

  }, [searchParams]);

  const fetchEmployeePerfomances = async (page = currentPage) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;
      const param: any = {
        page,
        limit: pageSize,
        search: searchText,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        role: filterRole !== "ALL" ? filterRole : undefined,
        shift: filterShift != "ALL" ? filterShift : undefined,
      };

      const result = await employeeApi.listManagerEmployeePerformances(param);
      if (requestId !== latestRequestRef.current) return;
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setEmployeePerformances(list);
        setTotal(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách nhân viên");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách nhân viên");
    } finally {
      setLoading(false);
    }
  };

  const handleViewEmployeeShipmentsDetail = (employeeId: number) => {
    navigate(`/employees/performance/${employeeId}/shipments`);
  };

  const handleExportEmployeesPerformance = async () => {
    try {
      const params: any = {
        search: searchText,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        role: filterRole !== "ALL" ? filterRole : undefined,
        shift: filterShift != "ALL" ? filterShift : undefined,
      };

      const result = await employeeApi.exportManagerEmployeePerformance(params);

      if (!result.success) {
        console.error("Export thất bại:", result.error);
      }

    } catch (error: any) {
      message.error(error.message || "Xuất Excel thất bại!");
    }
  };

  useEffect(() => {
    updateURL();
    fetchEmployeePerfomances(currentPage);
  }, [currentPage, pageSize, searchText, filterRole, filterShift, filterStatus]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          setSearchText={setSearchText}
          filters={{ role: filterRole, status: filterStatus, shift: filterShift }}
          setFilters={(key, val) => {
            if (key === "role") setFilterRole(val);
            if (key === "status") setFilterStatus(val);
            if (key === "shift") setFilterShift(val);
            setCurrentPage(1);
          }}
          onReset={() => {
            setSearchText("");
            setFilterRole("ALL");
            setFilterStatus("ALL");
            setFilterShift("ALL");
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