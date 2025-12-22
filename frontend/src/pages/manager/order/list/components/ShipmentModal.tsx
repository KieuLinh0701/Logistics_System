import React, { useEffect, useState } from "react";
import { Modal, Input, Table, Tag, Row, Col, Tooltip, Button, Select } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import dayjs from 'dayjs';
import type { ManagerShipment } from "../../../../../types/shipment";
import { SHIPMENT_FILTER_SORT, SHIPMENT_TYPES, translateShipmentFilterSort, translateShipmentStatus, translateShipmentType } from "../../../../../utils/shipmentUtils";
import locationApi from "../../../../../api/locationApi";
import { Option } from "antd/es/mentions";

interface Props {
  open: boolean;
  data: ManagerShipment[];
  page: number;
  limit: number;
  total: number;
  loading?: boolean;
  onClose: () => void;
  hover: boolean;
  searchText: string;
  onSearch: (value: string) => void;
  filterType: string;
  sort: string;
  onPageChange: (page: number, limit?: number) => void;
  onHoverChange: (hover: boolean) => void;
  onFilterChange: (filter: string, value: string) => void;
  onSortChange: (value: string) => void;
  onClearFilters: () => void;
  onConfirm: (shipment: ManagerShipment) => void;
}

const ShipmentModal: React.FC<Props> = ({
  open,
  data,
  page,
  limit,
  total,
  loading,
  onClose,
  searchText,
  onSearch,
  filterType,
  sort,
  hover,
  onPageChange,
  onHoverChange,
  onFilterChange,
  onSortChange,
  onClearFilters,
  onConfirm,
}) => {

  const [cityMap, setCityMap] = useState<Record<number, string>>({});
  const [tempSelected, setTempSelected] = React.useState<ManagerShipment | null>(null);

  const handleConfirm = () => {
    if (!tempSelected) return;
    onConfirm(tempSelected);
  };

  useEffect(() => {
    const loadCities = async () => {
      const codes = new Set<number>();

      data.forEach((item) => {
        if (item.fromOffice?.cityCode) {
          codes.add(item.fromOffice.cityCode);
        }
        if (item.toOffice?.cityCode) {
          codes.add(item.toOffice.cityCode);
        }
      });

      const map: Record<number, string> = {};

      await Promise.all(
        Array.from(codes).map(async (code) => {
          try {
            map[code] = (await locationApi.getCityNameByCode(code)) ?? 'N/A';
          } catch {
            map[code] = 'N/A';
          }
        })
      );

      setCityMap(map);
    };

    if (data.length) {
      loadCities();
    }
  }, [data]);

  const columns: ColumnsType<ManagerShipment> = [
    {
      title: 'Mã chuyến',
      dataIndex: 'code',
      key: 'code',
      align: 'center',
      render: (_, record) => {
        return (
          <span
            className="custom-table-content-strong"
          >
            {record.code}
          </span>
        );
      }
    },
    {
      title: 'Loại chuyến',
      dataIndex: 'type',
      key: 'type',
      align: 'center',
      render: (type) => translateShipmentType(type)
    },
    {
      title: 'Phương tiện',
      dataIndex: 'vehicle',
      key: 'vehicle',
      align: 'left',
      render: (_, record) => {
        const vehicle = record.vehicle;

        if (!vehicle) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            {vehicle.licensePlate}<br />
            <span className="text-muted">
              ({vehicle.capacity} Kg)
            </span>
          </div>
        );
      }
    },
    {
      title: 'Tuyến đường',
      key: 'route',
      align: 'center',
      render: (_, record) => {
        const fromCity = record.fromOffice?.cityCode
          ? cityMap[record.fromOffice.cityCode]
          : 'N/A';

        const toCity = record.toOffice?.cityCode
          ? cityMap[record.toOffice.cityCode]
          : 'N/A';

        return (
          <>
            <span>
              {fromCity} -
            </span><br />
            <span>
              {toCity}
            </span>
          </>
        );
      },
    },
    {
      title: 'Nhân viên',
      dataIndex: 'employee',
      key: 'employee',
      align: 'left',
      render: (_, record) => {
        const employee = record.employee;

        if (!employee) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            {employee.lastName} {employee.firstName} <br />
            <span className="text-muted">({employee.code})</span>
          </div>
        );
      }
    },
    {
      title: 'Thời gian bắt đầu',
      key: 'time',
      align: 'left',
      render: (_, record) => {
        const createdAt = record.createdAt
          ? dayjs(record.createdAt).format('HH:mm:ss DD/MM/YYYY')
          : null;

        if (!createdAt) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            {createdAt && (
              <div>
                {createdAt}
              </div>
            )}
          </div>
        );
      }
    },
  ];

  return (
    <Modal
      title={<span className="modal-title">Chọn chuyến hàng</span>}
      open={open}
      onCancel={onClose}
      onOk={handleConfirm}
      okText="Chọn"
      okButtonProps={{
        className: "modal-ok-button",
        disabled: !tempSelected,
        loading,
      }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      cancelText="Hủy"
      width={1200}
      className="modal-hide-scrollbar"
    >
      <div className="search-filters-container">
        <Row className="search-filters-row" gutter={16}>
          <Col span={24}>
            <div className="list-page-actions">
              <Tooltip title="Tìm theo mã chuyến hàng, biển số xe, mã/tên/SĐT/email nhân viên nhận chuyến">
                <Input
                  className="search-input"
                  placeholder="Tìm kiếm..."
                  prefix={<SearchOutlined />}
                  value={searchText}
                  onChange={(e) => onSearch(e.target.value)}
                  allowClear
                />
              </Tooltip>

              <Select
                value={filterType}
                onChange={(val) => onFilterChange('type', val)}
                className="filter-select"
              >
                <Option value="ALL">Tất cả loại chuyến</Option>
                {SHIPMENT_TYPES.map((t) => (
                  <Option key={t} value={t}>{translateShipmentType(t)}</Option>
                ))}
              </Select>

              <Select
                value={sort}
                onChange={onSortChange}
                className="filter-select-fit"
              >
                {SHIPMENT_FILTER_SORT.map((t) => (
                  <Option key={t} value={t}>{translateShipmentFilterSort(t)}</Option>
                ))}
              </Select>

              <Button
                type="default"
                icon={<CloseCircleOutlined />}
                onClick={onClearFilters}
                onMouseEnter={() => onHoverChange(true)}
                onMouseLeave={() => onHoverChange(false)}
                className="filter-button filter-button-icon-only"
              >
                {hover && 'Bỏ lọc'}
              </Button>
            </div>
          </Col>
        </Row>
      </div>

      <div className="manager-shipper-assigns-divide" />

      <Tag className="list-page-tag">Kết quả trả về: {total} chuyến hàng</Tag>

      <div className="table-container">
        <Table
          rowKey="id"
          dataSource={data}
          loading={loading}
          scroll={{ x: "max-content" }}
          className="list-page-table"
          pagination={{
            current: page,
            pageSize: limit,
            total: total,
            onChange: (newPage, newLimit) => onPageChange(newPage, newLimit),
          }}
          rowSelection={{
            type: "radio",
            selectedRowKeys: tempSelected ? [tempSelected.id] : [],
            onChange: (_, selectedRows) => {
              setTempSelected(selectedRows[0] as ManagerShipment);
            },
          }}
          columns={columns}
        />
      </div>
    </Modal>
  );
};

export default ShipmentModal;