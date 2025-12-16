import React from "react";
import { Result } from "antd";

const Forbidden: React.FC = () => {
  return (
    <Result
      status="403"
      title="403"
      subTitle="Xin lỗi, bạn không có quyền truy cập trang này."
    />
  );
};

export default Forbidden;