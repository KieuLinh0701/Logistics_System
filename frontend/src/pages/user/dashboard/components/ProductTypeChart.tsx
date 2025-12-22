import React from "react";
import { Link } from "react-router-dom";
import {
  PieChart, Pie, Cell, ResponsiveContainer,
  Tooltip
} from "recharts";
import { translateProductType } from "../../../../utils/productUtils";

interface Props {
  data?: Record<string, number>;
}

export const ProductTypeChart: React.FC<Props> = ({ data }) => {
  if (!data || Object.keys(data).length === 0) {
    return (
      <div className="dashboard-product-type-chart">
        <div className="dashboard-product-type-header">
          <h3 className="dashboard-product-type-title">Loại sản phẩm</h3>
        </div>
        <div className="dashboard-product-type-empty">
          Chưa có dữ liệu sản phẩm
        </div>
      </div>
    );
  }

  const filteredData = Object.entries(data)
    .filter(([_, value]) => value > 0)
    .slice(0, 4);

  if (filteredData.length === 0) {
    return (
      <div className="dashboard-product-type-chart">
        <div className="dashboard-product-type-header">
          <h3 className="dashboard-product-type-title">Loại sản phẩm</h3>
        </div>
        <div className="dashboard-product-type-empty">
          Không có sản phẩm nào
        </div>
      </div>
    );
  }

  const totalProducts = filteredData.reduce((sum, [_, value]) => sum + value, 0);

  const chartData = filteredData.map(([name, value]) => ({
    name,
    value,
    percent: totalProducts > 0 ? (value / totalProducts) * 100 : 0
  }));

  const COLORS = ["#1C3D90", "#2E5BBA", "#52c41a", "#fa8c16"];

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="custom-tooltip">
          <div className="tooltip-name">{translateProductType(data.name)}</div>
          <div className="tooltip-value">{data.value} sản phẩm</div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="dashboard-product-type-chart">
      <h3 className="dashboard-product-type-title">Loại sản phẩm</h3>

      <div className="dashboard-product-type-content">
        <div className="dashboard-product-type-chart-container">
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

          <div className="dashboard-product-type-total">
            <div className="total-value">{totalProducts}</div>
            <div className="total-label">Tổng</div>
          </div>
        </div>

        <div className="dashboard-product-type-legend-mini">
          {chartData.map((item, index) => (
            <div key={index} className="dashboard-product-type-legend-mini-item">
              <span
                className="legend-mini-color"
                style={{ backgroundColor: COLORS[index % COLORS.length] }}
              />
              <span className="legend-mini-name">{translateProductType(item.name)}</span>
              <span className="legend-mini-value">{item.value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};