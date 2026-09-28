import axios from '../lib/axios';
import { 
  Listing, 
  ListingSummary, 
  CreateListingRequest, 
  UpdateListingRequest, 
  PageResponse,
  Category,
  TransactionType
} from '../types';

export interface ListingFilters {
  search?: string;
  category?: Category;
  transactionType?: TransactionType;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export const listingService = {
  async getAllListings(filters: ListingFilters = {}): Promise<PageResponse<ListingSummary>> {
    const params = new URLSearchParams();
    
    if (filters.search) params.append('search', filters.search);
    if (filters.category) params.append('category', filters.category);
    if (filters.transactionType) params.append('transactionType', filters.transactionType);
    if (filters.minPrice !== undefined) params.append('minPrice', filters.minPrice.toString());
    if (filters.maxPrice !== undefined) params.append('maxPrice', filters.maxPrice.toString());
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());
    if (filters.sortBy) params.append('sortBy', filters.sortBy);
    if (filters.sortDirection) params.append('sortDirection', filters.sortDirection);

    const response = await axios.get<PageResponse<ListingSummary>>(`/api/listings?${params.toString()}`);
    return response.data;
  },

  async getListingById(id: number): Promise<Listing> {
    const response = await axios.get<Listing>(`/api/listings/${id}`);
    return response.data;
  },

  async createListing(data: CreateListingRequest): Promise<Listing> {
    const response = await axios.post<Listing>('/api/listings', data);
    return response.data;
  },

  async updateListing(id: number, data: UpdateListingRequest): Promise<Listing> {
    const response = await axios.put<Listing>(`/api/listings/${id}`, data);
    return response.data;
  },

  async deleteListing(id: number): Promise<void> {
    await axios.delete(`/api/listings/${id}`);
  },

  async markAsSold(id: number): Promise<Listing> {
    const response = await axios.patch<Listing>(`/api/listings/${id}/mark-sold`);
    return response.data;
  },

  async getMyListings(): Promise<Listing[]> {
    const response = await axios.get<Listing[]>('/api/listings/my-listings');
    return response.data;
  },

  async getUserListings(userId: number): Promise<Listing[]> {
    const response = await axios.get<Listing[]>(`/api/listings/user/${userId}`);
    return response.data;
  }
};
