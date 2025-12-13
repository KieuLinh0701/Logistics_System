import { useEffect, useState } from "react";
import {
  Table,
  Button,
  Modal,
  message,
  Row,
  Col,
  Tag,
  Form,
} from "antd";
import {
  TeamOutlined,
} from "@ant-design/icons";
import * as XLSX from "xlsx";
import dayjs from "dayjs";
import Title from "antd/es/typography/Title";
import SearchFilters from "./components/SearchFilters";
import Actions from "./components/Actions";
import EmployeeTable from "./components/Table";
import type { ManagerEmployee, ManagerEmployeeSearchRequest } from "../../../../types/employee";
import AddEditModal from "./components/AddEditModal";
import type { BulkResponse } from "../../../../types/response";
import BulkResult from "./components/BulkResult";
import employeeApi from "../../../../api/employeeApi";

const ManagerEmployeeList = () => {
  const [loading, setLoading] = useState(false);
  const [employees, setEmployees] = useState<ManagerEmployee[] | []>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<"create" | "edit">("create");
  const [newEmployee, setNewEmployee] = useState<Partial<ManagerEmployee>>({});
  const [searchText, setSearchText] = useState("");
  const [filterShift, setFilterShift] = useState<string>("ALL");
  const [filterStatus, setFilterStatus] = useState<string>("ALL");
  const [filterSort, setFilterSort] = useState<string>("NEWEST");
  const [filterRole, setFilterRole] = useState<string>("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(
    null
  );
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importResults, setImportResults] = useState<any[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [hover, setHover] = useState(false);
  const [form] = Form.useForm();

  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkResult, setBulkResult] = useState<BulkResponse<ManagerEmployee>>();

  const fetchEmployees = async (page = currentPage) => {
    try {
      setLoading(true);
      const param: ManagerEmployeeSearchRequest = {
        page,
        limit: pageSize,
        search: searchText || undefined,
        shift: filterShift !== "ALL" ? filterShift : undefined,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        role: filterRole !== "ALL" ? filterRole : undefined,
        sort: filterSort,
      };

      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await employeeApi.listManagerEmployees(param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setEmployees(list);
        setTotal(result.data.pagination?.total || 0);
        setCurrentPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách nhân viên");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách nhân viên");
    } finally {
      setLoading(false);
    }
  };

  // Sửa nhân viên
  const handleEditEmployee = async () => {
    const values = form.getFieldsValue();

    // Chuẩn bị payload gửi API
    const payload = {
      userRole: values.role,
      shift: values.shift,
      status: values.status,
      hireDate: values.hireDate?.toISOString(),
    };

    try {
      setLoading(true);
      const result = await employeeApi.updateManagerEmployee(newEmployee.id!, payload);

      if (result.success && result.data) {
        message.success(result.message || "Thêm nhân viên thành công!");
        setIsModalOpen(false);
        form.resetFields();
        fetchEmployees(currentPage);
      } else {
        message.error(result.message || "Có lỗi khi thêm nhân viên!");
      }

    } catch (error: any) {
      message.error(error.message || "Có lỗi khi thêm nhân viên!");
    } finally {
      setLoading(false);
    }
  };

  // Thêm nhân viên
  const handleAddEmployee = async () => {
    const values = form.getFieldsValue();

    // Chuẩn bị payload gửi API
    const payload = {
      userFirstName: values.firstName,
      userLastName: values.lastName,
      userEmail: values.email,
      userPhoneNumber: values.phoneNumber,
      userRole: values.role,
      shift: values.shift,
      status: values.status,
      hireDate: values.hireDate?.toISOString(),
    };

    try {
      setLoading(true);
      const result = await employeeApi.createManagerEmployee(payload);

      if (result.success && result.data) {
        message.success(result.message || "Thêm nhân viên thành công!");
        setIsModalOpen(false);
        form.resetFields();
        fetchEmployees(currentPage);
      } else {
        message.error(result.message || "Có lỗi khi thêm nhân viên!");
      }

    } catch (error: any) {
      message.error(error.message || "Có lỗi khi thêm nhân viên!");
    } finally {
      setLoading(false);
    }
  };

  // Nhập Excel
  const handleExcelUpload = async (file: File) => {
    const reader = new FileReader();
    // reader.onload = async (e) => {
    //   const data = new Uint8Array(e.target?.result as ArrayBuffer);
    //   const workbook = XLSX.read(data, { type: "array" });
    //   const sheetName = workbook.SheetNames[0];
    //   const worksheet = workbook.Sheets[sheetName];
    //   const rows: any[] = XLSX.utils.sheet_to_json(worksheet);

    //   // const newEmployees: Partial<Employee>[] = rows.map((row) => ({
    //   const newEmployees: Partial<any>[] = rows.map((row) => ({
    //     shift: row["Ca làm"] || "Full Day",
    //     status: row["Trạng thái"] || "Active",
    //     hireDate: row["Ngày tuyển dụng"]
    //       ? dayjs(row["Ngày tuyển dụng"]).toDate()
    //       : new Date(),
    //     user: {
    //       id: 0,
    //       firstName: row["Tên"] || "",
    //       lastName: row["Họ"] || "",
    //       email: row["Email"] || "",
    //       phoneNumber: row["Số điện thoại"] || "",
    //       role: row["Chức vụ"] || "Shipper",
    //     },
    //   }));

    //   if (newEmployees.length === 0) {
    //     message.error("Không tìm thấy dữ liệu nhân viên trong file Excel!");
    //     return;
    //   }

    //   try {
        // const resultAction = await dispatch(importEmployees({ employees: newEmployees })).unwrap();

        // // Lấy object result nested từ backend
        // const importResultData = resultAction.result;

        // if (importResultData?.success) {
        //   message.success(
        //     importResultData.message ||
        //     `Import hoàn tất: ${importResultData.totalImported} thành công, ${importResultData.totalFailed} thất bại`
        //   );

        //   setImportResults(importResultData.results ?? []);
        //   setImportModalOpen(true);
        // } else {
        //   message.error(importResultData?.message || "Import thất bại");
        // }
    //   } catch (err: any) {
    //     message.error(err.message || "Có lỗi xảy ra khi import nhân viên");
    //   }
    // };
    reader.readAsArrayBuffer(file);
    return false;
  };

  const handleDownloadTemplate = () => {
    message.warning("Tính năng này đang được phát triển")
    // const wb = XLSX.utils.book_new();

    // const data = [
    //   {
    //     "Họ": "Nguyễn",
    //     "Tên": "Văn A",
    //     "Email": "example@gmail.com",
    //     "Số điện thoại": "0123456789",
    //     "Chức vụ": OFFICE_MANAGER_ADDABLE_ROLES!.join("/"),
    //     "Ca làm": EMPLOYEE_SHIFTS!.join("/"),
    //     "Trạng thái": EMPLOYEE_STATUSES!.join("/"),
    //     "Ngày tuyển dụng": "YYYY-MM-DD",
    //   },
    // ];

    // const ws = XLSX.utils.json_to_sheet(data);
    // const header = [
    //   "Họ",
    //   "Tên",
    //   "Email",
    //   "Số điện thoại",
    //   "Chức vụ",
    //   "Ca làm",
    //   "Trạng thái",
    //   "Ngày tuyển dụng",
    // ];
    // XLSX.utils.sheet_add_aoa(ws, [header], { origin: 0 });
    // ws["!cols"] = [
    //   { wch: 10 },
    //   { wch: 15 },
    //   { wch: 25 },
    //   { wch: 15 },
    //   { wch: 15 },
    //   { wch: 35 },
    //   { wch: 20 },
    //   { wch: 15 },
    // ];
    // XLSX.utils.book_append_sheet(wb, ws, "Template");
    // XLSX.writeFile(wb, "employee_template.xlsx");
  };

  useEffect(() => {
    setCurrentPage(1);
    fetchEmployees(currentPage);
  }, []);

  useEffect(() => {
    setCurrentPage(1);
    fetchEmployees(1);
  }, [searchText, filterShift, filterStatus, filterRole, filterSort, dateRange]);

  const handleSubmit = () => {
    if (modalMode == 'edit') {
      handleEditEmployee();
    } else {
      handleAddEmployee();
    }
  };

  const handleFilterChange = (filter: string, value: string) => {
    switch (filter) {
      case 'sort':
        setFilterSort(value);
        break;
      case 'shift':
        setFilterShift(value);
        break;
      case 'role':
        setFilterRole(value);
        break;
      case 'status':
        setFilterStatus(value);
        break;
    }
    setCurrentPage(1);
  };

  const handleClearFilters = () => {
    setSearchText('');
    setFilterShift('ALL');
    setFilterStatus('ALL');
    setFilterRole('ALL');
    setFilterSort('NEWEST');
    setDateRange(null);
    setCurrentPage(1);
  };

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          filterShift={filterShift}
          filterStatus={filterStatus}
          filterRole={filterRole}
          filterSort={filterSort}
          dateRange={dateRange}
          hover={hover}
          onSearchChange={setSearchText}
          onFilterChange={handleFilterChange}
          onDateRangeChange={setDateRange}
          onClearFilters={handleClearFilters}
          onHoverChange={setHover}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <TeamOutlined className="title-icon" />
              Danh sách nhân viên
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                onAdd={() => {
                  setIsModalOpen(true);
                  setModalMode("create");
                  setNewEmployee({});
                  form.resetFields();
                }}
                onImportExcel={handleExcelUpload}
                onDownloadTemplate={handleDownloadTemplate}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} nhân viên</Tag>

        <EmployeeTable
          data={employees}
          page={currentPage}
          limit={pageSize}
          total={total}
          loading={loading}
          onEdit={(employee) => {
            setModalMode('edit');
            setNewEmployee(employee);
            setIsModalOpen(true);
            form.setFieldsValue({
              lastName: employee.userLastName,
              firstName: employee.userFirstName,
              email: employee.userEmail,
              phoneNumber: employee.userPhoneNumber,
              role: employee.userRole,
              shift: employee.shift,
              status: employee.status,
              hireDate: dayjs(employee.hireDate)
            });
          }}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetchEmployees(page);
          }}
        />

        <AddEditModal
          open={isModalOpen}
          mode={modalMode}
          employee={newEmployee}
          onOk={handleSubmit}
          onCancel={() => setIsModalOpen(false)}
          loading={loading}
          form={form}
        />

        {bulkResult &&
          <BulkResult
            open={bulkModalOpen}
            results={bulkResult}
            onClose={() => setBulkModalOpen(false)}
          />
        }
      </div>
    </div>
  );
};

export default ManagerEmployeeList;