import React from 'react';
import { Modal, Table, Tag, Row, Col, Statistic, Card } from 'antd';
import type { BulkResponse } from '../../../../../types/response';
import type { ManagerOrderShipment } from '../../../../../types/shipment';

interface BulkResultProps {
  open: boolean;
  results: BulkResponse<ManagerOrderShipment>;
  onClose: () => void;
}

const BulkResult: React.FC<BulkResultProps> = ({
  open,
  results,
  onClose,
}) => {
  const successCount = results.totalImported;
  const failCount = results.totalFailed;
  const totalCount = successCount + failCount;

  const columns = [
    {
      title: 'Mã đơn hàng',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (name: string) => (
        <div className="cell-content">
          {name}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'success',
      key: 'success',
      width: 120,
      render: (success: boolean) =>
        success ?
          <Tag color="green">Thành công</Tag> :
          <Tag color="red">Thất bại</Tag>,
    },
    {
      title: 'Thông báo',
      dataIndex: 'message',
      key: 'message',
      render: (message: string) => (
        <div className="cell-content message-cell">
          {message || '—'}
        </div>
      ),
    },
  ];

  return (
    <Modal
      title={<span className='modal-title'>Kết quả thêm đơn hàng vào chuyến</span>}
      open={open}
      onCancel={onClose}
      footer={null}
      width={1000}
      centered
      className="modal-bulk modal-hide-scrollbar"
    >
      {/* Thống kê */}
      <Row gutter={16} className="bulk-stat-row">
        <Col span={8}>
          <Card size="small" className="statistic-total">
            <Statistic title="Tổng đơn hàng" value={totalCount} />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small" className="statistic-success">
            <Statistic title="Thành công" value={successCount} />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small" className="statistic-fail">
            <Statistic title="Thất bại" value={failCount} />
          </Card>
        </Col>
      </Row>

      <div className="bulk-table-wrapper">
        <Table
          className="bulk-table"
          dataSource={results.results.map((r, i) => ({
            key: i,
            name: r.name || 'Không có tên',
            success: r.success ?? false,
            message: r.message || '',
          }))}
          columns={columns}
          pagination={false}
          size="middle"
          scroll={{ x: true }}
        />
      </div>
    </Modal>
  );
};

export default BulkResult;