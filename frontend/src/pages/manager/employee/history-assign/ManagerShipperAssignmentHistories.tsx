import { useEffect, useState } from "react";
import {
  message,
  Row,
  Col,
  Tag,
} from "antd";
import {
  TeamOutlined,
} from "@ant-design/icons";
import Title from "antd/es/typography/Title";
import Actions from "./components/Actions";
import type { ManagerShipperAssignment, ManagerShipperAssignmentSearchRequest } from "../../../../types/shipperAssignment";
import shipperAssignmentApi from "../../../../api/shipperAssignmentApi";
import AssignmentHistoryTable from "./components/Table";
import dayjs from "dayjs";
import officeApi from "../../../../api/officeApi";
import SearchFilters from "./components/SearchFilters";
import { useSearchParams } from "react-router-dom";

const ManagerShipperAssignmentHistories = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const [loading, setLoading] = useState(false);
  const [histories, setHistories] = useState<ManagerShipperAssignment[] | []>([]);

  const [hover, setHover] = useState(false);
  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState<string>("NEWEST");
  const [filterWardCode, setFilterWardCode] = useState<number | undefined>(undefined);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(
    null
  );

  const [officeCityCode, setOfficeCityCode] = useState<number | undefined>(undefined);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterWardCode !== undefined) params.ward = filterWardCode.toString();
    params.sort = filterSort.toLowerCase();
    if (currentPage) params.page = currentPage;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const s = searchParams.get("search");
    const ward = searchParams.get("ward")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    if (s) setSearchText(s);
    if (ward) setFilterWardCode(Number(ward));
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetchShipperAssignments = async (page = currentPage) => {
    try {
      setLoading(true);
      const param: ManagerShipperAssignmentSearchRequest = {
        page,
        limit: pageSize,
        search: searchText || undefined,
        sort: filterSort,
        wardCode: filterWardCode != undefined ? filterWardCode : undefined,
      };

      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await shipperAssignmentApi.listManagerShipperAssignments(param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setHistories(list);
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

  const handleExportShipperAsssignmentHistoryToExcel = async () => {
    try {
      setLoading(true);
      const param: ManagerShipperAssignmentSearchRequest = {
        page: currentPage,
        limit: pageSize,
        search: searchText || undefined,
        sort: filterSort,
        wardCode: filterWardCode != undefined ? filterWardCode : undefined,
      };

      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await shipperAssignmentApi.exportManagerShipperAssignments(param);

      if (!result.success) {
        console.error("Export thất bại:", result.error);
      }

    } catch (error: any) {
      console.error("Export lỗi:", error.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const fetchOfficeCityCode = async () => {
      try {
        setLoading(true);

        const result = await officeApi.getManagerOfficeCityCode();
        if (result.success && result.data) {
          setOfficeCityCode(result.data);
        } else {
          message.error(result.message || "Lỗi khi thành phố bưu cục của bạn");
          setOfficeCityCode(undefined);
        }
      } catch (error: any) {
        message.error(error.message || "Lỗi khi thành phố bưu cục của bạn");
        setOfficeCityCode(undefined);
      } finally {
        setLoading(false);
      }
    };

    setCurrentPage(1);
    fetchOfficeCityCode();
    fetchShipperAssignments(currentPage);
  }, []);

  useEffect(() => {
    setCurrentPage(1);
    fetchShipperAssignments(1);
    updateURL();
  }, [searchText, filterSort, filterWardCode, dateRange]);

  const handleFilterChange = (filter: string, value: string | number | undefined) => {
    switch (filter) {
      case 'sort':
        setFilterSort(String(value));
        break;
      case 'ward':
        setFilterWardCode(Number(value));
        break;
    }
    setCurrentPage(1);
  };

  const handleClearFilters = () => {
    setSearchText('');
    setFilterSort('NEWEST');
    setFilterWardCode(undefined);
    setDateRange(null);
    setCurrentPage(1);
  };

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        {officeCityCode !== undefined && (
          <SearchFilters
            cityCode={officeCityCode}
            searchText={searchText}
            filterWardCode={filterWardCode}
            filterSort={filterSort}
            dateRange={dateRange}
            hover={hover}
            onSearchChange={setSearchText}
            onFilterChange={handleFilterChange}
            onDateRangeChange={setDateRange}
            onClearFilters={handleClearFilters}
            onHoverChange={setHover}
          />
        )}

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <TeamOutlined className="title-icon" />
              Lịch sử phân công giao hàng
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                onExport={handleExportShipperAsssignmentHistoryToExcel}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} phân công</Tag>

        <AssignmentHistoryTable
          data={histories}
          page={currentPage}
          limit={pageSize}
          total={total}
          loading={loading}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetchShipperAssignments(page);
          }}
        />
      </div>
    </div>
  );
};

export default ManagerShipperAssignmentHistories;