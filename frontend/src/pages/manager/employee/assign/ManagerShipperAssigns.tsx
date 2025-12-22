import { useEffect, useRef, useState } from "react";
import {
  message,
  Row,
  Col,
  Tag,
} from "antd";
import {
  TeamOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import Title from "antd/es/typography/Title";
import SearchFilters from "./components/SearchFilters";
import Actions from "./components/Actions";
import EmployeeTable from "./components/Table";
import type { ManagerEmployee, ManagerEmployeeSearchRequest, ManagerEmployeeWithShipperAssignments } from "../../../../types/employee";
import AddEditModal from "./components/AddEditModal";
import employeeApi from "../../../../api/employeeApi";
import officeApi from "../../../../api/officeApi";
import SelectEmployeeModal from "./components/SelectEmployeeModal";
import type { ManagerShipperAssignmentEditRequest } from "../../../../types/shipperAssignment";
import shipperAssignmentApi from "../../../../api/shipperAssignmentApi";
import ConfirmModal from "../../../common/ConfirmModal";
import { useSearchParams } from "react-router-dom";
import "./ManagerShipperAssigns.css"

const ManagerShipperAssigns = () => {
  const latestRequestRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [employees, setEmployees] = useState<ManagerEmployeeWithShipperAssignments[] | []>([]);
  const [shippers, setShippers] = useState<ManagerEmployee[] | []>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<"create" | "edit">("create");
  const [newAssignment, setNewAssignment] = useState<Partial<ManagerEmployee>>({});
  const [searchText, setSearchText] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [officeCityCode, setOfficeCityCode] = useState<number | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<ManagerEmployee | null>(null);

  const [employeeModalOpen, setEmployeeModalOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [totalShipper, setTotalShipper] = useState(0);
  const [loadingShipper, setLoadingShipper] = useState(false);
  const [searchTextShipper, setSearchTextShipper] = useState("");

  const [modalConfirmOpen, setModalConfirmOpen] = useState(false);
  const [selectedDeleteId, setSelectedDeleteId] = useState<number | null>(null);

  const updateURL = () => {
    const params: any = {};
    if (searchText) params.search = searchText;
    if (currentPage) params.page = currentPage;

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const pageParam = Number(searchParams.get("page")) || 1;
    const s = searchParams.get("search");

    setCurrentPage(pageParam);
    if (s) setSearchText(s);
  }, [searchParams]);

  const fetchEmployees = async (page = currentPage) => {
    try {
      setLoading(true);
      const param: ManagerEmployeeSearchRequest = {
        page,
        limit: pageSize,
        search: searchText || undefined,
      };

      const result = await employeeApi.getManagerActiveShippersWithActiveAssignments(param);
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

  const fetchShippers = async (current = page) => {
    try {
      setLoadingShipper(true);
      const requestId = ++latestRequestRef.current;
      const param: ManagerEmployeeSearchRequest = {
        page: current,
        limit: limit,
        search: searchTextShipper || undefined,
      };

      const result = await employeeApi.getManagerActiveShippers(param);
      if (requestId !== latestRequestRef.current) return;
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setShippers(list);
        setTotalShipper(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách nhân viên");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách nhân viên");
    } finally {
      setLoadingShipper(false);
    }
  };

  useEffect(() => {
    const fetchOfficeCityCode = async () => {
      try {
        setLoading(true);

        const result = await officeApi.getManagerOfficeCityCode();
        if (result.success && result.data) {
          setOfficeCityCode(result.data);
        } else {
          message.error(result.message || "Lỗi khi thành phố bưu cục của bạn");
          setOfficeCityCode(null);
        }
      } catch (error: any) {
        message.error(error.message || "Lỗi khi thành phố bưu cục của bạn");
        setOfficeCityCode(null);
      } finally {
        setLoading(false);
      }
    };

    fetchOfficeCityCode();
  }, []);

  useEffect(() => {
    fetchEmployees(currentPage);
    updateURL();
  }, [currentPage, pageSize, searchText]);

  const handleSubmit = async (values: any) => {
    if (!selectedEmployee) return;

    const payload: ManagerShipperAssignmentEditRequest = {
      selectedEmployee: selectedEmployee.id,
      wardCode: values.wardCode,
      startAt: values.startAt
        ? dayjs(values.startAt).format("YYYY-MM-DDTHH:mm:ss")
        : null,
      endAt: values.endAt
        ? dayjs(values.endAt).format("YYYY-MM-DDTHH:mm:ss")
        : null,
      notes: values.notes,
    };

    try {
      setLoading(true);

      const result =
        modalMode === "edit"
          ? await shipperAssignmentApi.updateManagerShipperAssignment(
            newAssignment.id!,
            payload
          )
          : await shipperAssignmentApi.createManagerShipperAssignment(payload);

      if (result.success) {
        message.success(
          modalMode === "edit"
            ? "Cập nhật phân công thành công!"
            : "Tạo phân công thành công!"
        );
        setIsModalOpen(false);
        fetchEmployees(currentPage);
      } else {
        message.error(result.message || "Có lỗi xảy ra!");
      }
    } catch (err: any) {
      message.error(err.message || "Có lỗi xảy ra!");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenEmployeeModal = async () => {
    setEmployeeModalOpen(true);
    setPage(1);
    setSearchTextShipper("");
  }

  useEffect(() => {
    fetchShippers(page);
  }, [page, limit, searchTextShipper]);

  const handleRemoveSelectedEmployee = async () => {
    setSelectedEmployee(null);
  }

  const handleSelectEmployee = (employee: ManagerEmployee) => {
    setSelectedEmployee(employee);
    setEmployeeModalOpen(false);
  };

  const handleSearchEmployee = async (keyword: string) => {
    setSearchTextShipper(keyword);
    setPage(1);
  };

  const handleOpenDeleteConfirm = (id: number) => {
    setSelectedDeleteId(id);
    setModalConfirmOpen(true);
  };

  const confirmDelete = async () => {
    if (!selectedDeleteId) return;

    setModalConfirmOpen(false);

    try {
      setLoading(true);

      const result = await shipperAssignmentApi.deleteManagerFutureShipperAssignment(selectedDeleteId);

      if (result.success) {
        message.success(result.message || "Xóa phân công giao hàng thành công");
        fetchEmployees(currentPage);
      } else {
        message.error(result.message || "Có lỗi khi xóa phân công giao hàng!");
      }
    } catch (err: any) {
      message.error(err.message || "Có lỗi khi xóa phân công giao hàng!");
    } finally {
      setLoading(false);
      setSelectedDeleteId(null);
    }
  };

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          onSearchChange={(value) => {
            setSearchText(value)
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <TeamOutlined className="title-icon" />
              Danh sách nhân viên giao hàng
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                onAdd={() => {
                  setIsModalOpen(true);
                  setModalMode("create");
                  setSelectedEmployee(null);
                  setNewAssignment({});
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
          onEdit={(employee, assignment) => {
            setModalMode("edit");
            setNewAssignment(assignment);
            setSelectedEmployee(employee);
            setIsModalOpen(true);
          }}
          onAdd={(employee) => {
            setModalMode("create");
            setSelectedEmployee(employee);
            setIsModalOpen(true);
          }}
          onDelete={handleOpenDeleteConfirm}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
          }}
        />

        {officeCityCode && (
          <AddEditModal
            open={isModalOpen}
            mode={modalMode}
            assign={newAssignment}
            onOk={handleSubmit}
            onCancel={() => {
              setIsModalOpen(false);
              setSelectedEmployee(null);
            }}
            loading={loading}
            cityCode={officeCityCode}
            selectedEmployee={selectedEmployee}
            onSelectEmployee={handleOpenEmployeeModal}
            onClearEmployee={handleRemoveSelectedEmployee}
          />
        )}
      </div>

      <SelectEmployeeModal
        open={employeeModalOpen}
        employees={shippers}
        page={page}
        limit={limit}
        total={totalShipper}
        selectedEmployee={selectedEmployee}
        loading={loadingShipper}
        onClose={() => setEmployeeModalOpen(false)}
        onSearch={handleSearchEmployee}
        onSelectEmployee={handleSelectEmployee}
        onPageChange={(newPage) => setPage(newPage)}
      />

      <ConfirmModal
        title='Xác nhận xóa phân công giao hàng'
        message='Bạn có chắc chắn muốn xóa phân công giao hàng này không?'
        open={modalConfirmOpen}
        onOk={confirmDelete}
        onCancel={() => setModalConfirmOpen(false)}
        loading={loading}
      />
    </div>
  );
};

export default ManagerShipperAssigns;