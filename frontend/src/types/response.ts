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
  list: T[];
  pagination: Pagination;
}

export interface BulkResult<T> {
  name: string;
  success: boolean;
  message: string;
  result: T;
}

export interface BulkResponse<T> {
  success: boolean;
  message: string;
  totalImported: number;
  totalFailed: number;
  results: BulkResult<T>[];
}