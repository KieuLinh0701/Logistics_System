import React, { useState } from "react";
import { Segmented, Table, Tag } from "antd";
import type { UserTopProductItem } from "../../../../types/dashboard";

interface Props {
  topSelling: UserTopProductItem[];
  topReturned: UserTopProductItem[];
}

const TopProductTable: React.FC<Props> = ({
  topSelling,
  topReturned,
}) => {
  const [showReturned, setShowReturned] = useState(false);
  const currentData = showReturned ? topReturned : topSelling;
  const title = showReturned ? "Hoàn hàng" : "Bán chạy";

  return (
    <div className="dashboard-top-product">
      <h3 className="dashboard-top-product-title">
        Top 5 sản phẩm
      </h3>

      <div className="dashboard-top-product-content">
        <div className="dashboard-top-product-filter">
          <Segmented
            options={["Bán chạy", "Hoàn hàng"]}
            value={title}
            onChange={(value) => setShowReturned(value === "Hoàn hàng")}
            className="dashboard-top-product-segmented"
          />
        </div>

        <div className="dashboard-top-product-table-container">
          <Table
            dataSource={currentData}
            pagination={false}
            columns={[
              {
                title: "STT",
                key: "index",
                width: 60,
                align: "center",
                render: (_, __, index) => (
                  <div className="product-rank">
                    {index === 0 ? "🥇" :
                      index === 1 ? "🥈" :
                        index === 2 ? "🥉" :
                          `${index + 1}.`}
                  </div>
                ),
              },
              {
                title: "Tên sản phẩm",
                dataIndex: "name",
                key: "name",
                ellipsis: true,
                className: "product-name-column",
              },
              {
                title: showReturned ? "Số lần hoàn" : "Đã bán",
                dataIndex: "total",
                key: "total",
                width: 120,
                align: "center",
                render: (total: number) => (
                  <Tag
                    className={showReturned ? "returned-tag" : "sold-tag"}
                  >
                    {total.toLocaleString()}
                  </Tag>
                ),
              },
            ]}
            rowKey="id"
            size="small"
            showHeader={true}
            rowClassName={() => "dashboard-top-product-row"}
          />
        </div>
      </div>
    </div>
  );
};

export default TopProductTable;