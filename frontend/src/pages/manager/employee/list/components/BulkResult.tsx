import React from 'react';
import { Modal, Table, Tag, Row, Col, Statistic, Card } from 'antd';
import type { BulkResponse } from '../../../../../types/response';
import type { ManagerEmployee } from '../../../../../types/employee';

interface BulkResultProps {
  open: boolean;
  results: BulkResponse<ManagerEmployee>;
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
      title: 'Tên nhân viên',
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
          <Tag color="green" className="bulk-tag">Thành công</Tag> :
          <Tag color="red" className="bulk-tag">Thất bại</Tag>,
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
      title="Kết quả thêm nhân viên"
      open={open}
      onCancel={onClose}
      footer={null}
      width={800}
      centered
      className="modal-bulk"
    >
      {/* Thống kê */}
      <Row gutter={16} className="bulk-stat-row">
        <Col span={8}>
          <Card size="small" className="statistic-total">
            <Statistic title="Tổng nhân viên" value={totalCount} />
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