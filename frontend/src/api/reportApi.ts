import axiosClient from './axiosClient';

function extractFileName(headers: Headers, fallback: string): string {
  const cd = headers.get('content-disposition') || headers.get('Content-Disposition');
  if (!cd) return fallback;
  const utf8 = cd.match(/filename\*=UTF-8''([^;\n]+)/i);
  if (utf8 && utf8[1]) {
    try {
      return decodeURIComponent(utf8[1].trim());
    } catch {
      return utf8[1].trim();
    }
  }
  const quoted = cd.match(/filename="([^"]+)"/i);
  if (quoted && quoted[1]) {
    return quoted[1].trim();
  }
  return fallback;
}

async function fetchExport(url: string, fallbackName: string): Promise<{ blob: Blob; fileName: string }> {
  const token = sessionStorage.getItem('token');
  const resp = await fetch(url, { headers: { Authorization: token ? `Bearer ${token}` : '' } });
  if (!resp.ok) throw new Error('Export failed');
  const blob = await resp.blob();
  const fileName = extractFileName(resp.headers, fallbackName);
  return { blob, fileName };
}

function buildQuery(start?: string, end?: string): string {
  const params: string[] = [];
  if (start) params.push(`start=${start}`);
  if (end) params.push(`end=${end}`);
  return params.length ? `?${params.join('&')}` : '';
}

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';
const API_BASE_ALT = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const reportApi = {
  getFinancial: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/financial${buildQuery(start, end)}`),
  getOverview: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/overview${buildQuery(start, end)}`),

  getShippers: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/shippers${buildQuery(start, end)}`),

  getShipper: (shipperId: number, start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/shipper/${shipperId}${buildQuery(start, end)}`),

  getTransferred: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/transferred${buildQuery(start, end)}`),

  getFees: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/fees${buildQuery(start, end)}`),

  getOperations: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/operations${buildQuery(start, end)}`),

  getOffice: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/offices${buildQuery(start, end)}`),

  getShippersDetailed: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/shippers${buildQuery(start, end)}`),

  getOfficesDetailed: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/offices${buildQuery(start, end)}`),

  getFinance: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/finance${buildQuery(start, end)}`),

  getShop: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/shop${buildQuery(start, end)}`),

  exportOperations: async (start?: string, end?: string) =>
    fetchExport(`${API_BASE}/admin/reports/operations/export${buildQuery(start, end)}`, 'Bao_cao_van_hanh.xlsx'),

  exportOffice: async (start?: string, end?: string) =>
    fetchExport(`${API_BASE}/admin/reports/office/export${buildQuery(start, end)}`, 'Bao_cao_theo_buu_cuc.xlsx'),

  exportOverview: async (start?: string, end?: string) =>
    fetchExport(`${API_BASE_ALT}/admin/reports/overview/export${buildQuery(start, end)}`, 'Bao_cao_tong_quan.xlsx'),

  exportShippers: async (start?: string, end?: string) =>
    fetchExport(`${API_BASE_ALT}/admin/reports/shippers/export${buildQuery(start, end)}`, 'Bao_cao_chi_tiet_shipper.xlsx'),

  exportFinance: async (start?: string, end?: string) =>
    fetchExport(`${API_BASE_ALT}/admin/reports/finance/export${buildQuery(start, end)}`, 'Bao_cao_tai_chinh.xlsx'),

  exportOfficesDetailed: async (start?: string, end?: string) =>
    fetchExport(`${API_BASE_ALT}/admin/reports/offices/export${buildQuery(start, end)}`, 'Bao_cao_chi_tiet_buu_cuc.xlsx'),

  exportShop: async (start?: string, end?: string) =>
    fetchExport(`${API_BASE}/admin/reports/shop/export${buildQuery(start, end)}`, 'Bao_cao_theo_cua_hang.xlsx'),
};