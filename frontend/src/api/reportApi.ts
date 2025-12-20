import axiosClient from './axiosClient';

export const reportApi = {
  getFinancial: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/financial${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  getShippers: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/shipper${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  getShipper: (shipperId: number, start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/shipper/${shipperId}${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  getTransferred: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/transferred${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  getFees: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/fees${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  getOperations: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/operations${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  getOffice: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/office${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  getShop: (start?: string, end?: string) =>
    axiosClient.get(`/admin/reports/shop${start || end ? `?${start?`start=${start}`:''}${start&&end?'&':''}${end?`end=${end}`:''}` : ''}`),
  
  exportOperations: async (start?: string, end?: string) => {
    const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
    const token = sessionStorage.getItem('token');
    const params = [];
    if (start) params.push(`start=${start}`);
    if (end) params.push(`end=${end}`);
    const url = `${API_BASE}/admin/reports/operations/export${params.length?`?${params.join('&')}`:''}`;
    const resp = await fetch(url, { headers: { Authorization: token ? `Bearer ${token}` : '' } });
    if (!resp.ok) throw new Error('Export failed');
    const blob = await resp.blob();
    return blob;
  },

  exportOffice: async (start?: string, end?: string) => {
    const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
    const token = sessionStorage.getItem('token');
    const params = [];
    if (start) params.push(`start=${start}`);
    if (end) params.push(`end=${end}`);
    const url = `${API_BASE}/admin/reports/office/export${params.length?`?${params.join('&')}`:''}`;
    const resp = await fetch(url, { headers: { Authorization: token ? `Bearer ${token}` : '' } });
    if (!resp.ok) throw new Error('Export failed');
    return await resp.blob();
  },

  exportShop: async (start?: string, end?: string) => {
    const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
    const token = sessionStorage.getItem('token');
    const params = [];
    if (start) params.push(`start=${start}`);
    if (end) params.push(`end=${end}`);
    const url = `${API_BASE}/admin/reports/shop/export${params.length?`?${params.join('&')}`:''}`;
    const resp = await fetch(url, { headers: { Authorization: token ? `Bearer ${token}` : '' } });
    if (!resp.ok) throw new Error('Export failed');
    return await resp.blob();
  },
};
