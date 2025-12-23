import React, { useEffect, useState } from 'react';
import { Table, Button, Tag, Switch, Dropdown } from 'antd';
import { CheckOutlined, CloseOutlined, EditOutlined, DeleteOutlined, DownOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import locationApi from '../../../../../../api/locationApi';

export interface Address {
  id: number;
  wardCode: number;
  cityCode: number;
  detail: string;
  isDefault: boolean;
  name: string;
  phoneNumber: string;
}

interface AddressTableProps {
  data: Address[];
  loading?: boolean;
  onEdit: (address: Address) => void;
  onDelete: (addressId: number) => void;
  onSetDefault: (addressId: number) => void;
}

const AddressTable: React.FC<AddressTableProps> = ({
  data,
  loading = false,
  onEdit,
  onDelete,
  onSetDefault,
}) => {
  const [locationMap, setLocationMap] = useState<Record<number, { city: string; ward: string }>>({});

  useEffect(() => {
    const fetchLocations = async () => {
      const map: Record<number, { city: string; ward: string }> = {};
      for (const address of data) {
        const cityName = await locationApi.getCityNameByCode(address.cityCode) || "Unknown";
        const wardName = await locationApi.getWardNameByCode(address.cityCode, address.wardCode) || "Unknown";
        map[address.id] = { city: cityName, ward: wardName };
      }
      setLocationMap(map);
    };
    fetchLocations();
  }, [data]);

  const columns: ColumnsType<Address> = [
    {
      title: 'Tên',
      dataIndex: 'name',
      key: 'name',
      align: 'left'
    },
    {
      title: 'Số điện thoại',
      dataIndex: 'phoneNumber',
      key: 'phoneNumber',
      align: 'left'
    },
    {
      title: 'Địa chỉ',
      dataIndex: 'detail',
      key: 'detail',
      align: 'left',
      render: (text: string, record: Address) => {
        const location = locationMap[record.id];
        const cityName = location?.city || "";
        const wardName = location?.ward || "";

        return (
          <div>
            <span>{text}</span><br />
            <span>{wardName}, {cityName}</span>
          </div>
        );
      }
    },
    {
      title: 'Mặc định',
      dataIndex: 'isDefault',
      key: 'isDefault',
      align: 'left',
      render: (val: boolean, record: Address) => (
        <Switch
          className={"custom-switch"}
          checked={val}
          disabled={val}
          onChange={() => onSetDefault(record.id)}
          checkedChildren={<CheckOutlined />}
          unCheckedChildren={<CloseOutlined />}
        />
      ),
    },
    {
      key: 'action',
      align: 'left',
      render: (_: any, record: Address) => {
        const items = [
          {
            key: "edit",
            icon: <EditOutlined />,
            label: "Sửa",
            onClick: () => onEdit(record),
          },
          {
            key: "delete",
            icon: <DeleteOutlined />,
            label: "Xóa",
            onClick: () => onDelete(record.id),
          },
        ];

        return (
          <Dropdown menu={{ items }} trigger={['click']}>
            <Button className="dropdown-trigger-button">
              Hành động <DownOutlined />
            </Button>
          </Dropdown>
        );
      },
    },
  ];

  const tableData = data.map((p) => ({ ...p, key: p.id }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="key"
        loading={loading}
        scroll={{ x: "max-content" }}
        className="list-page-table"
        pagination={false}
      />
    </div>
  );
};

export default AddressTable;