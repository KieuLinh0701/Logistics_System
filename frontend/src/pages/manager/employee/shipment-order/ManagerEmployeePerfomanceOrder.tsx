import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Tag, Row, Col, message } from "antd";
import SearchFilters from "./components/SearchFilters";
import OrderTable from "./components/Table";
import Actions from "./components/Actions";
import type { Order } from "../../../../types/order";
import Title from "antd/es/typography/Title";
import { ShoppingOutlined } from "@ant-design/icons";

const ManagerEmployeePerfomanceOrder = () => {
  const navigate = useNavigate();

  const { employeeCode } = useParams<{ employeeCode: string }>();
  const { shipmentCode } = useParams<{ shipmentCode: string }>();

  const [orders, setOrders] = useState<Order[] | []>([]);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState("NONE");
  const [filterPayer, setFilterPayer] = useState("ALL");
  const [filterCOD, setFilterCOD] = useState("ALL");

  // --- Fetch Orders ---
  const fetchOrders = (page = currentPage) => {

    const payload: any = {
      shipmentCode: shipmentCode,
      page,
      limit: pageSize,
      searchText: searchText,
      payer: filterPayer !== "ALL" ? filterPayer : undefined,
      cod: filterCOD !== "ALL" ? filterCOD : undefined,
      sort: filterSort !== "NONE" ? filterSort : undefined,
    };
    // dispatch(getShipmentOrders(payload));
  };

  const handleExportShipmentOrders = async () => {
    try {
      const payload: any = {
        shipmentCode: shipmentCode,
        searchText: searchText,
        payer: filterPayer !== "ALL" ? filterPayer : undefined,
        cod: filterCOD !== "ALL" ? filterCOD : undefined,
        sort: filterSort !== "NONE" ? filterSort : undefined,
      };

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

  const handleViewOrderDetail = (trackingNumber: string) => {
    navigate(`/orders/tracking/${trackingNumber}`);
  };

  useEffect(() => {
    setCurrentPage(1);
    fetchOrders(1);
  }, [searchText, filterPayer, filterCOD, filterSort]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          setSearchText={setSearchText}
          filters={{ payer: filterPayer, cod: filterCOD, sort: filterSort }}
          setFilters={(key, val) => {
            if (key === "payer") setFilterPayer(val);
            if (key === "cod") setFilterCOD(val);
            if (key === "sort") setFilterSort(val);
          }}
          onReset={() => {
            setSearchText("");
            setFilterSort("NONE");
            setFilterPayer("ALL");
            setFilterCOD("ALL");
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <ShoppingOutlined className="title-icon" />
              Danh sách đơn hàng
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions onExport={handleExportShipmentOrders} />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>

        <OrderTable
          orders={orders}
          onDetail={handleViewOrderDetail}
          currentPage={currentPage}
          pageSize={pageSize}
          total={total}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetchOrders(page);
          }}
        />
      </div>
    </div>
  );
};

export default ManagerEmployeePerfomanceOrder;