export interface OrderProduct {
  id?: number;
  productId: number;
  productPrice: number;
  productType: string;
  productStock: number;
  productCode: string;
  productName: string;
  productWeight: number;
  quantity: number;
  price: number;
  deliveredQuantity?: number;
  returnedQuantity?: number;
}

export interface OrderProductPrint {
  productName: string;
  quantity: number;
}

export interface OrderProductRequest {
  productId: number;
  quantity: number;
}