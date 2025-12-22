import { useEffect, useRef, useState } from "react";
import {
  message,
  Row,
  Col,
  Tag,
  Form,
} from "antd";
import {
  TeamOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import Title from "antd/es/typography/Title";
import type { ManagerEmployee, ManagerEmployeeSearchRequest } from "../../../../types/employee";
import type { BulkResponse } from "../../../../types/response";
import employeeApi from "../../../../api/employeeApi";
import { useSearchParams } from "react-router-dom";
import EmployeeTable from "./components/Table";
import Actions from "./components/Actions";
import SearchFilters from "./components/SearchFilters";
import AddEditModal from "./components/AddEditModal";

const ManagerEmployeeList = () => {
  const latestRequestRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();
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
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [hover, setHover] = useState(false);
  const [form] = Form.useForm();

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterShift !== "ALL") params.shift = filterShift.toLowerCase();
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterRole !== "ALL") params.role = filterRole;
    params.sort = filterSort.toLowerCase();
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
    const shift = searchParams.get("shift")?.toLocaleUpperCase();
    const status = searchParams.get("status")?.toLocaleUpperCase();
    const r = searchParams.get("role");
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    setCurrentPage(pageParam);
    if (s) setSearchText(s);
    if (shift) setFilterShift(shift);
    if (status) setFilterStatus(status);
    if (r) setFilterRole(r);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetchEmployees = async (page = currentPage) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;
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
      if (requestId !== latestRequestRef.current) return;
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setEmployees(list);
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

  // Sửa nhân viên
  const handleEditEmployee = async () => {
    const values = form.getFieldsValue();

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

  useEffect(() => {
    fetchEmployees(currentPage);
    updateURL();
  }, [currentPage, pageSize, searchText, filterShift, filterStatus, filterRole, filterSort, dateRange]);

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
              lastName: employee.lastName,
              firstName: employee.firstName,
              email: employee.email,
              phoneNumber: employee.phoneNumber,
              role: employee.role,
              shift: employee.shift,
              status: employee.status,
              hireDate: dayjs(employee.hireDate)
            });
          }}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
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
      </div>
    </div>
  );
};

export default ManagerEmployeeList;