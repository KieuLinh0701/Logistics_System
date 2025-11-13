import React from "react";
import ShippingFeeBody from "./ShippingFeeBody";
import HeaderHome from "../../../components/common/HeaderHome";
import FooterHome from "../../../components/common/FooterHome";

const ShippingFee: React.FC = () => {
  return (
    <div>
      <HeaderHome />
      <ShippingFeeBody />
      <FooterHome />
    </div>
  );
};

export default ShippingFee;