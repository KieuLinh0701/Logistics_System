import React from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Legend,
  CartesianGrid,
} from "recharts";
import dayjs from "dayjs";
import type { UserOrderTimeLineItem } from "../../../../types/dashboard";

interface Props {
  data?: UserOrderTimeLineItem[];
}

export const OrderTimelineChart: React.FC<Props> = ({ data }) => {
  const fillTimelineData = (
    data: UserOrderTimeLineItem[] | undefined
  ) => {
    const map = new Map<string, UserOrderTimeLineItem>();

    data?.forEach(item => {
      const key = dayjs(item.date).format("YYYY-MM-DD");
      map.set(key, item);
    });

    const days = data && data.length > 0 ? 7 : 1;

    const result: UserOrderTimeLineItem[] = [];

    for (let i = days - 1; i >= 0; i--) {
      const date = dayjs().subtract(i, "day");
      const key = date.format("YYYY-MM-DD");

      const item = map.get(key);

      result.push({
        date: date.toISOString(),
        createdCount: item?.createdCount ?? 0,
        deliveredCount: item?.deliveredCount ?? 0,
      });
    }

    return result;
  };

  const normalizedData = fillTimelineData(data);

  const chartData = normalizedData.map(item => ({
    date: dayjs(item.date).format("DD/MM"),
    fullDate: dayjs(item.date).format("DD/MM/YYYY"),
    created: item.createdCount,
    delivered: item.deliveredCount,
  }));

  // Custom Tooltip
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const { fullDate } = payload[0].payload;

      return (
        <div className="custom-tooltip">
          <p className="tooltip-date">{fullDate}</p>

          {payload.map((entry: any, index: number) => (
            <p
              key={index}
              className="tooltip-item"
              style={{ color: entry.color }}
            >
              <span
                className="tooltip-dot"
                style={{ backgroundColor: entry.color }}
              />
              {entry.name}: <strong>{entry.value}</strong>
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  return (
    <div className="dashboard-order-timeline">
      <div className="dashboard-order-timeline-header">
        <h3 className="dashboard-order-timeline-title">Lịch sử đơn hàng</h3>
      </div>

      <div className="dashboard-order-timeline-content">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart
            data={chartData}
            margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
          >
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="#f5f5f5"
              vertical={false}
            />

            <XAxis
              dataKey="date"
              stroke="#666"
              fontSize={12}
              tickLine={false}
              axisLine={{ stroke: '#f0f0f0' }}
            />

            <YAxis
              stroke="#666"
              fontSize={12}
              tickLine={false}
              axisLine={{ stroke: '#f0f0f0' }}
            />

            <Tooltip content={<CustomTooltip />} />
            <Legend
              wrapperStyle={{ paddingTop: 10 }}
              iconType="circle"
              iconSize={8}
              fontSize={12}
            />

            <Line
              type="monotone"
              dataKey="created"
              name="Đơn mới"
              stroke="#fa8c16"
              strokeWidth={2.5}
              dot={{ stroke: '#fa8c16', strokeWidth: 2, r: 4 }}
              activeDot={{ r: 6, strokeWidth: 0 }}
            />

            <Line
              type="monotone"
              dataKey="delivered"
              name="Đã giao"
              stroke="#52c41a"
              strokeWidth={2.5}
              dot={{ stroke: '#52c41a', strokeWidth: 2, r: 4 }}
              activeDot={{ r: 6, strokeWidth: 0 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};