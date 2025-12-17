export interface SearchRequest {
  page?: number;
  limit?: number;
  search?: string;
  status?: string;
  type?: string;
  sort?: string;
  cod?: string;
  payer?: string;
  startDate?: string;
  endDate?: string;
}