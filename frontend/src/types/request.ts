export interface SearchRequest {
  page?: number;
  limit?: number;
  search?: string;
  status?: string;
  sort?: string;
  startDate?: string;
  endDate?: string;
}