import React from "react";
import {
  PieChart, Pie, Cell, ResponsiveContainer,
  Tooltip
} from "recharts";
import { translateEmployeeShift } from "../../../../utils/employeeUtils";

interface Props {
  data?: Record<string, number>;
}

export const EmployeeTypeChart: React.FC<Props> = ({ data }) => {
  if (!data || Object.keys(data).length === 0) {
    return (
      <div className="manager-dashboard-vehicle-type-chart">
        <div className="manager-dashboard-vehicle-type-header">
          <h3 className="manager-dashboard-vehicle-type-title">Tỷ lệ nhân viên theo ca</h3>
        </div>
        <div className="manager-dashboard-vehicle-type-empty">
          Chưa có dữ các nhân viên theo ca
        </div>
      </div>
    );
  }

  const filteredData = Object.entries(data)
    .filter(([_, value]) => value > 0)
    .slice(0, 4);

  if (filteredData.length === 0) {
    return (
      <div className="manager-dashboard-vehicle-type-chart">
        <div className="manager-dashboard-vehicle-type-header">
          <h3 className="manager-dashboard-vehicle-type-title">Tỷ lệ nhân viên theo ca</h3>
        </div>
        <div className="manager-dashboard-vehicle-type-empty">
          Không có nhân viên nào 
        </div>
      </div>
    );
  }

  const totalVehicles = filteredData.reduce((sum, [_, value]) => sum + value, 0);

  const chartData = filteredData.map(([name, value]) => ({
    name,
    value,
    percent: totalVehicles > 0 ? (value / totalVehicles) * 100 : 0
  }));

  const COLORS = ["#52c41a", "#fa8c16", "#1890ff"];
;

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="custom-tooltip">
          <div className="tooltip-name">{translateEmployeeShift(data.name)}</div>
          <div className="tooltip-value">{data.value} Nhân viên</div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="manager-dashboard-vehicle-type-chart">
      <h3 className="manager-dashboard-vehicle-type-title">Tỷ lệ nhân viên theo ca</h3>

      <div className="manager-dashboard-vehicle-type-content">
        <div className="manager-dashboard-vehicle-type-chart-container">
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={chartData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={90}
                paddingAngle={2}
                labelLine={false}
              >
                {chartData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={COLORS[index % COLORS.length]}
                    stroke="#fff"
                    strokeWidth={1}
                  />
                ))}
              </Pie>
              <Tooltip content={<CustomTooltip />} />
            </PieChart>
          </ResponsiveContainer>

          <div className="manager-dashboard-vehicle-type-total">
            <div className="total-value">{totalVehicles}</div>
            <div className="total-label">Tổng</div>
          </div>
        </div>

        <div className="manager-dashboard-vehicle-type-legend-mini">
          {chartData.map((item, index) => (
            <div key={index} className="manager-dashboard-vehicle-type-legend-mini-item">
              <span
                className="legend-mini-color"
                style={{ backgroundColor: COLORS[index % COLORS.length] }}
              />
              <span className="legend-mini-name">{translateEmployeeShift(item.name)}</span>
              <span className="legend-mini-value">{item.value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};