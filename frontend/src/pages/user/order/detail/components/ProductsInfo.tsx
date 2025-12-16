import React from "react";
import Title from "antd/es/typography/Title";
import type { OrderProduct } from "../../../../../types/orderProduct";

interface Props {
  products: OrderProduct[];
}

const ProductsInfo: React.FC<Props> = ({ products }) => {
  if (!products.length) return null;

  return (
    <div className="order-detail-product-info container">
      <Title level={5} className="order-detail-product-info title">Sản phẩm trong đơn hàng</Title>
      <table className="order-detail-product-info-table">
        <thead>
          <tr className="order-detail-product-info-table-header">
            <th className="order-detail-product-info-table-th">Mã sản phẩm</th>
            <th className="order-detail-product-info-table-th">Tên sản phẩm</th>
            <th className="order-detail-product-info-table-th">Khối lượng</th>
            <th className="order-detail-product-info-table-th">Giá tiền (VNĐ)</th>
            <th className="order-detail-product-info-table-th">Số lượng</th>
          </tr>
        </thead>
        <tbody>
          {products.map((item, index) => (
            <tr key={index} className="order-detail-product-info-table-tr">
              <td className="order-detail-product-info-table-th">{item.productCode}</td>
              <td className="order-detail-product-info-table-th">{item.productName}</td>
              <td className="order-detail-product-info-table-th">{item.productWeight}</td>
              <td className="order-detail-product-info-table-th">{item.price.toLocaleString()}</td>
              <td className="order-detail-product-info-table-th">{item.quantity}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default ProductsInfo;