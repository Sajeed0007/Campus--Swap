import axios from '../lib/axios';
import { CreateReportRequest, ReportResponse } from '../types';

export const reportService = {
  async createReport(data: CreateReportRequest): Promise<ReportResponse> {
    const response = await axios.post<ReportResponse>('/api/reports', data);
    return response.data;
  },

  async getAllReports(): Promise<ReportResponse[]> {
    const response = await axios.get<ReportResponse[]>('/api/reports');
    return response.data;
  },

  async getPendingReports(): Promise<ReportResponse[]> {
    const response = await axios.get<ReportResponse[]>('/api/reports/pending');
    return response.data;
  },

  async resolveReport(id: number): Promise<ReportResponse> {
    const response = await axios.patch<ReportResponse>(`/api/reports/${id}/resolve`);
    return response.data;
  },

  async dismissReport(id: number): Promise<ReportResponse> {
    const response = await axios.patch<ReportResponse>(`/api/reports/${id}/dismiss`);
    return response.data;
  }
};
