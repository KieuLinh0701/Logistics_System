import { useEffect, useRef, useState } from "react";
import { Col, message, Row, Tag } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import ShipmentTable from "./components/Table";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import Actions from "./components/Actions";
import Title from "antd/es/typography/Title";
import { CarOutlined } from "@ant-design/icons";
import type { ManagerShipment } from "../../../../types/shipment";
import employeeApi from "../../../../api/employeeApi";
import type { SearchRequest } from "../../../../types/request";

const ManagerEmployeePerfomanceShipment = () => {
  const navigate = useNavigate();
  const latestRequestRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);

  const { employeeId } = useParams<{ employeeId: string }>();

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState("NEWEST");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [shipments, setShipments] = useState<ManagerShipment[] | []>([]);
  const [exportShipments, setExportShipments] = useState<ManagerShipment[] | []>([]);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (currentPage) params.page = currentPage;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const pageParam = Number(searchParams.get("page")) || 1;
    const s = searchParams.get("search");
    const status = searchParams.get("status")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");


    setCurrentPage(pageParam);
    if (s) setSearchText(s);
    if (status) setFilterStatus(status);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }

  }, [searchParams]);

  const fetchShipments = async (page = currentPage) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;
      if (!employeeId) return;

      const param: SearchRequest = {
        page,
        search: searchText,
        limit: pageSize,
        sort: filterSort,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await employeeApi.listManagerShipmentsByEmployeeId(
        Number(employeeId),
        param);

      if (requestId !== latestRequestRef.current) return;
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setShipments(list);
        setTotal(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách chuyến hàng của nhân viên");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách chuyến hàng của nhân viên");
    } finally {
      setLoading(false);
    }
  };

  const handleViewOrderShipmentsDetail = (id: number) => {
    navigate(`/employees/performance/${employeeId}/shipments/${id}/orders`);
  };

  const handleExportEmployeeShipments = async () => {
    try {
      const params: SearchRequest = {
        sort: filterSort !== "NONE" ? filterSort : undefined,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
      };

      if (dateRange) {
        params.startDate = dateRange[0].startOf("day").toISOString();
        params.endDate = dateRange[1].endOf("day").toISOString();
      }

      // const resultAction = await dispatch(exportEmployeeShipments(params));
      // const payload = resultAction.payload as any;
      // const data = Array.isArray(payload) ? payload : payload?.exportShipments ?? [];

      // if (data.length === 0) {
      //   return message.info("Không có dữ liệu để xuất Excel");
      // }

      // // Map dữ liệu xuất Excel theo bảng hiển thị
      // const exportData = data.map((t: any) => ({
      //   "Mã chuyến": t.id,
      //   "Trạng thái": translateShipmentStatus(t.status) || "N/A",
      //   "Biển số phương tiện": t.vehicle?.licensePlate || "N/A",
      //   "Tải trọng xe (kg)": t.vehicle?.capacity || "N/A",
      //   "Tổng số đơn": t.orderCount ?? 0,
      //   "Tổng trọng lượng (kg)": Number(t.totalWeight || 0).toFixed(2),
      //   "Thời gian bắt đầu": t.startTime
      //     ? dayjs(t.startTime).locale("vi").format("DD/MM/YYYY HH:mm")
      //     : "N/A",
      //   "Thời gian kết thúc": t.endTime
      //     ? dayjs(t.endTime).locale("vi").format("DD/MM/YYYY HH:mm")
      //     : "N/A",
      // }));

      // const worksheet = XLSX.utils.json_to_sheet(exportData);

      // // Căn chỉnh độ rộng cột (dựa trên độ dài tên cột và dữ liệu)
      // worksheet["!cols"] = [
      //   { wch: 12 }, // Mã chuyến
      //   { wch: 15 }, // Trạng thái
      //   { wch: 18 }, // Biển số phương tiện
      //   { wch: 20 }, // Tên phương tiện
      //   { wch: 18 }, // Tổng số đơn
      //   { wch: 22 }, // Tổng trọng lượng (kg)
      //   { wch: 22 }, // Thời gian bắt đầu
      //   { wch: 22 }, // Thời gian kết thúc
      // ];

      // const workbook = XLSX.utils.book_new();
      // XLSX.utils.book_append_sheet(workbook, worksheet, "Danh sách chuyến giao");

      // const excelBuffer = XLSX.write(workbook, { bookType: "xlsx", type: "array" });
      // const blob = new Blob([excelBuffer], { type: "application/octet-stream" });

      // saveAs(blob, `DanhSachChuyenGiao_${dayjs().format("YYYYMMDD_HHmm")}.xlsx`);
    } catch (error) {
      console.error(error);
      message.error("Xuất Excel thất bại!");
    }
  };

  useEffect(() => {
    updateURL();
    fetchShipments(currentPage);
  }, [currentPage, pageSize, searchText, dateRange, filterSort, filterStatus]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          setSearchText={setSearchText}
          dateRange={dateRange}
          setDateRange={setDateRange}
          filters={{ sort: filterSort, status: filterStatus }}
          setFilters={(key, val) => {
            if (key === "sort") setFilterSort(val);
            if (key === "status") setFilterStatus(val);
            setCurrentPage(1)
          }}
          onReset={() => {
            setSearchText("");
            setFilterSort("NEWEST");
            setFilterStatus("ALL");
            setDateRange(null);
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <CarOutlined className="title-icon" />
              Danh sách chuyến hàng của nhân viên
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions onExport={handleExportEmployeeShipments} />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} chuyến hàng</Tag>

        <ShipmentTable
          shipments={shipments}
          onDetail={handleViewOrderShipmentsDetail}
          currentPage={currentPage}
          pageSize={pageSize}
          total={total}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetchShipments(page);
          }}
        />
      </div>
    </div>
  );
};

export default ManagerEmployeePerfomanceShipment;