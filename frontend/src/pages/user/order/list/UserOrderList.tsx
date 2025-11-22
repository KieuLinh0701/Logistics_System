import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { Modal, message, Tag, Row, Col } from "antd";
import dayjs from "dayjs";
import OrderActions from "./components/Actions";
import * as XLSX from "xlsx";
import SearchFilters from "./components/SearchFilters";
import OrderTable from "./components/Table";
import Title from "antd/es/typography/Title";
import type { Order, UserOrderSearchRequest } from "../../../../types/order";
import orderApi from "../../../../api/orderApi";
import "../../../../styles/ListPage.css";
import type { ServiceType } from "../../../../types/serviceType";
import serviceTypeApi from "../../../../api/serviceTypeApi";
import { ShoppingOutlined } from "@ant-design/icons";

const UserOrderList = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [limit, setLimit] = useState(10);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState<number>(0);

  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);

  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);

  const [search, setSearch] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterSort, setFilterSort] = useState("NEWEST");
  const [filterPayer, setFilterPayer] = useState("ALL");
  const [filterPaymentStatus, setFilterPaymentStatus] = useState("ALL");
  const [filterServiceType, setFilterServiceType] = useState<number | null>(null);
  const [filterCOD, setFilterCOD] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  const updateURL = () => {
    const params: any = {};

    if (search) params.search = search.toLowerCase();
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterPayer !== "ALL") params.payer = filterPayer.toLowerCase();
    if (filterPaymentStatus !== "ALL") params.payment = filterPaymentStatus.toLowerCase();
    if (filterServiceType !== null) params.service = filterServiceType;
    if (filterCOD !== "ALL") params.cod = filterCOD.toLowerCase();
    if (filterSort !== "NEWEST") params.sort = filterSort.toLowerCase();
    if (page) params.page = page;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params);
  };

  // --- Fetch Orders ---
  const fetchOrders = async (currentPage = page) => {
    try {
      setLoading(true);

      const param: UserOrderSearchRequest = {
        page: currentPage,
        limit: limit,
        search: search,
        payer: filterPayer !== "ALL" ? filterPayer : undefined,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        serviceTypeId: filterServiceType !== null ? filterServiceType : undefined,
        paymentStatus: filterPaymentStatus !== "ALL" ? filterPaymentStatus : undefined,
        cod: filterCOD !== "ALL" ? filterCOD : undefined,
        sort: filterSort !== "NEWEST" ? filterSort : undefined,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await orderApi.listUserOrders(param);
      if (result.success && result.data) {
        const orderList = result.data?.list || [];
        setOrders(orderList);
        setTotal(result.data.pagination?.total || result.data.list.length);
        setPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách đơn hàng");
      }
    } catch (error) {
      console.error("Error fetching orders:", error);
    } finally {
      setLoading(false);
    }
  };

  // --- Cancel Order ---
  const handleCancelOrder = (orderId: number) => {
    Modal.confirm({
      title: "Xác nhận hủy đơn hàng",
      content: "Bạn có chắc chắn muốn hủy đơn hàng này không?",
      okText: "Hủy",
      cancelText: "Không",
      centered: true,
      icon: null,
      okButtonProps: {
        className: "modal-ok-button",
      },
      cancelButtonProps: {
        className: "modal-cancel-button",
      },
      onOk: async () => {
        try {
          // const resultAction = await dispatch(cancelUserOrder(orderId)).unwrap();
          // if (resultAction.success) {
          //   message.success(resultAction.message || "Hủy đơn hàng thành công");
          //   fetchOrders(page);
          // } else {
          //   message.error(resultAction.message || "Hủy đơn thất bại");
          // }
        } catch (error: any) {
          message.error(error.message || "Lỗi server khi hủy đơn hàng");
        }
      },
    });
  };

  // --- Excel Import ---
  const handleExcelUpload = (file: File): boolean => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target?.result as ArrayBuffer);
        const workbook = XLSX.read(data, { type: "array" });
        const sheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[sheetName];
        const rows: any[] = XLSX.utils.sheet_to_json(worksheet);

        const newOrders: Partial<Order>[] = rows.map(row => ({
          senderName: row["Tên người gửi"]?.trim() || "",
          senderPhone: row["SĐT người gửi"]?.trim() || "",
          recipientName: row["Tên người nhận"]?.trim() || "",
          recipientPhone: row["SĐT người nhận"]?.trim() || "",
          weight: row["Trọng lượng (kg)"] ?? 0,
          paymentMethod: row["Phương thức thanh toán"]?.trim() || "Cash",
          status: row["Trạng thái"]?.trim() || "pending",
          notes: row["Ghi chú"]?.trim() || "",
        }));

        const invalidRows = newOrders.filter(o => !o.senderName || !o.recipientName || !o.weight);
        if (invalidRows.length > 0) {
          message.error("Có dòng bị thiếu thông tin bắt buộc. Vui lòng kiểm tra lại file!");
          return;
        }

        message.success("Đọc file Excel thành công. Chưa gửi lên server trong demo này.");
      } catch (error) {
        message.error("Có lỗi khi đọc file Excel!");
      }
    };
    reader.readAsArrayBuffer(file);

    return false;
  };

  const handleDownloadTemplate = () => {
    const wb = XLSX.utils.book_new();
    const data = [
      {
        "Tên người gửi": "Nguyen Van A",
        "SĐT người gửi": "0123456789",
        "Tên người nhận": "Tran Thi B",
        "SĐT người nhận": "0987654321",
        "Trọng lượng (kg)": 1.5,
        "Phương thức thanh toán": "Cash",
        "Trạng thái": "pending",
        "Ghi chú": "",
      },
    ];
    const ws = XLSX.utils.json_to_sheet(data);
    const header = Object.keys(data[0]);
    XLSX.utils.sheet_add_aoa(ws, [header], { origin: 0 });
    XLSX.utils.book_append_sheet(wb, ws, "Template");
    XLSX.writeFile(wb, "order_template.xlsx");
  };

  const handleAdd = () => {
    navigate(`/orders/create`);
  };

  const handleClearFilters = () => {
    setSearch("");
    setFilterStatus("ALL");
    setFilterServiceType(null);
    setFilterSort("NEWEST");
    setFilterPayer("ALL");
    setFilterPaymentStatus("ALL");
    setFilterCOD("ALL");
    setDateRange(null);
    setPage(1);
  };

  const handleFilterChange = (filter: string, value: string) => {
    switch (filter) {
      case 'status':
        setFilterStatus(value);
        break;
      case 'payer':
        setFilterPayer(value);
        break;
      case 'paymentStatus':
        setFilterPaymentStatus(value);
        break;
      case 'serviceType':
        setFilterServiceType(Number(value));
        break;
      case 'cod':
        setFilterCOD(value);
        break;
      case 'sort':
        setFilterSort(value);
        break;
    }
  };

  useEffect(() => {
    const fetchServiceTypes = async () => {
      try {
        setLoading(true);
        const response = await serviceTypeApi.getActiveServiceTypes();
        if (response.success && response.data) {
          setServiceTypes(response.data);
        }
      } catch (error) {
        console.error("Error fetching Service types:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
    fetchServiceTypes();
  }, []);

  useEffect(() => {
    updateURL();
    setPage(1);
    fetchOrders(1);

    console.log("search", search);
  }, [search, filterStatus, filterServiceType, filterPayer, filterPaymentStatus, filterCOD, dateRange, filterSort]);

  useEffect(() => {
    const s = searchParams.get("search")?.toLocaleUpperCase();
    const st = searchParams.get("status")?.toLocaleUpperCase();
    const payer = searchParams.get("payer")?.toLocaleUpperCase();
    const pst = searchParams.get("payment")?.toLocaleUpperCase();
    const sid = searchParams.get("service");
    const cod = searchParams.get("cod")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const sd = searchParams.get("start");
    const ed = searchParams.get("end");

    if (s) setSearch(s);
    if (st) setFilterStatus(st);
    if (payer) setFilterPayer(payer);
    if (pst) setFilterPaymentStatus(pst);

    if (sid) setFilterServiceType(Number(sid));

    if (cod) setFilterCOD(cod);
    if (sort) setFilterSort(sort);

    if (sd && ed) {
      setDateRange([
        dayjs(sd, "YYYY-MM-DD"),
        dayjs(ed, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams, serviceTypes]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          search={search}
          setSearch={setSearch}
          dateRange={dateRange}
          setDateRange={setDateRange}
          showAdvancedFilters={showAdvancedFilters}
          setShowAdvancedFilters={setShowAdvancedFilters}
          filters={{ status: filterStatus, payer: filterPayer, paymentStatus: filterPaymentStatus, serviceType: filterServiceType, cod: filterCOD, sort: filterSort }}
          setFilters={handleFilterChange}
          serviceTypes={serviceTypes}
          onReset={handleClearFilters}
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
              <OrderActions
                onAdd={handleAdd}
                onUpload={handleExcelUpload}
                onDownloadTemplate={handleDownloadTemplate}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>

        <OrderTable
          orders={orders}
          onCancel={handleCancelOrder}
          page={page}
          total={total}
          loading={loading}
          limit={limit}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
            fetchOrders(page);
          }}
        />

      </div>
    </div>
  );
};

export default UserOrderList;