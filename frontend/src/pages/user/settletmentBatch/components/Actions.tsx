import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined, ScheduleOutlined } from "@ant-design/icons";
import { weekdayOrder } from "../../../../utils/userSettlementScheduleUtils";

interface Props {
  weekdays: string[];
  onExport: () => void;
  onSetSchedule: () => void;
}

const Actions: React.FC<Props> = ({ weekdays, onExport, onSetSchedule }) => {
  return (
    <Space align="center">
      {weekdays.length > 0 && (
        <div className="text-muted">
          COD của bạn sẽ được chuyển vào thứ{" "}
          {weekdays
            .slice()
            .sort((a, b) => (weekdayOrder[a] || 0) - (weekdayOrder[b] || 0))
            .map((day, index, arr) => (
              <span key={day}>
                {weekdayOrder[day]}
                {index < arr.length - 1 ? ", " : ""}
              </span>
            ))}
          {" "}lúc 20:00 hàng tuần
        </div>
      )}
      <Button
        className="primary-button"
        icon={<ScheduleOutlined />}
        onClick={onSetSchedule}
      >
        Đổi lịch đối soát
      </Button>
      <Button
        className="success-button"
        icon={<FileExcelOutlined />}
        onClick={onExport}
      >
        Xuất Excel
      </Button>
    </Space>
  );
};

export default Actions;