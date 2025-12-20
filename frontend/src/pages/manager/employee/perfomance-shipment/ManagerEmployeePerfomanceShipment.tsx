import { useEffect, useState } from "react";
import { Col, message, Row, Tag } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import ShipmentTable from "./components/Table";
import { useNavigate, useParams } from "react-router-dom";
import Actions from "./components/Actions";
import Title from "antd/es/typography/Title";
import { CarOutlined } from "@ant-design/icons";
import type { ManagerShipment } from "../../../../types/shipment";

const ManagerEmployeePerfomanceShipment = () => {
  const navigate = useNavigate();

  const { employeeCode } = useParams<{ employeeCode: string }>();

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState("NONE");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [shipments, setShipments] = useState<ManagerShipment[] | []>([]);
  const [exportShipments, setExportShipments] = useState<ManagerShipment[] | []>([]);

  const fetchShipments = (page = currentPage, search?: string) => {
    if (!employeeCode) return;

    const payload: any = {
      employeeCode: employeeCode,
      page,
      limit: pageSize,
      sort: filterSort !== "none" ? filterSort : undefined,
      status: filterStatus !== "ALL" ? filterStatus : undefined,
    };
    if (dateRange) {
      payload.startDate = dateRange[0].startOf("day").toISOString();
      payload.endDate = dateRange[1].endOf("day").toISOString();
    }

    // dispatch(listEmployeeShipments(payload));
  };

  const handleViewOrderShipmentsDetail = (shipmentCode: string) => {
    navigate(`/manager/employees/performance/${employeeCode}/shipments/${shipmentCode}/orders`);
  };

  const handleExportEmployeeShipments = async () => {
    try {
      const params: any = {
        employeeCode: employeeCode,
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
    setCurrentPage(1)
    fetchShipments(currentPage);
  }, []);

  useEffect(() => {
    setCurrentPage(1)
    fetchShipments(currentPage);
  }, [searchText, dateRange, filterSort, filterStatus]);

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
          }}
          onReset={() => {
            setSearchText("");
            setFilterSort("NONE");
            setFilterStatus("ALL");
            setDateRange(null);
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <CarOutlined className="title-icon" />
              Danh sách chuyến đi
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions onExport={handleExportEmployeeShipments} />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} chuyến xe</Tag>

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