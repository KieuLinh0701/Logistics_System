export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

export interface Pagination {
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface ListResponse<T> {
  data: T[];
  pagination: Pagination;
}