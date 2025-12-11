export interface Product {
    id: number;
    code: string;
    image: string;
    name: string;
    weight: number;
    price: number;
    type: string;
    status: string;
    stock: number;
    soldQuantity: number;
    createdAt: Date;
    updatedAt: Date;
}

export interface UserProductForm {
  id?: number;
  name?: string;
  price?: number;
  weight?: number;
  stock?: number;
  type?: string;
  status?: string;

  image?: string;      
  imageFile?: File;    
}

export interface UserBulkProductForm {
  products: UserProductForm[];
}

export interface UserProductSearchRequest {
    page: number;
    limit: number;
    search?: string;
    type?: string;
    status?: string;
    stock?: string;
    sort?: string;
    startDate?: string;
    endDate?: string;
}

export interface UserProductActiveAndInstockRequest {
    page: number;
    limit: number;
    search?: string;
    type?: string;
}