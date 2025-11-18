export const translateShippingRateRegionType = (value: string): string => {
  switch (value) {
    case 'INTRA_CITY': return 'Nội tỉnh';       
    case 'INTRA_REGION': return 'Nội miền';     
    case 'NEAR_REGION': return 'Cận miền';       
    case 'INTER_REGION': return 'Liên miền';          
    default: return value;
  }
};

export const getShippingRateRegionTypeNote = (value: string): string => {
  switch (value) {
    case "INTRA_CITY":
      return "Áp dụng cho các tuyến gửi nhận trong cùng tỉnh hoặc thành phố.";

    case "INTRA_REGION":
      return "Áp dụng cho các tuyến thuộc cùng khu vực Bắc, Trung hoặc Nam.";

    case "NEAR_REGION":
      return "Áp dụng cho các tuyến giữa những khu vực lân cận Bắc - Trung hoặc Trung - Nam.";

    case "INTER_REGION":
      return "Áp dụng cho các tuyến giữa những khu vực Bắc - Nam.";
      
    default:
      return value;
  }
};