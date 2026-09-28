// Enums
export enum Role {
  STUDENT = 'STUDENT',
  ADMIN = 'ADMIN'
}

export enum Category {
  BOOKS = 'BOOKS',
  ELECTRONICS = 'ELECTRONICS',
  STATIONERY = 'STATIONERY',
  HOSTEL_ITEMS = 'HOSTEL_ITEMS',
  OTHER = 'OTHER'
}

export enum TransactionType {
  SELL = 'SELL',
  SWAP = 'SWAP',
  DONATE = 'DONATE'
}

export enum ItemCondition {
  NEW = 'NEW',
  LIKE_NEW = 'LIKE_NEW',
  GOOD = 'GOOD',
  FAIR = 'FAIR',
  POOR = 'POOR'
}

export enum ListingStatus {
  AVAILABLE = 'AVAILABLE',
  RESERVED = 'RESERVED',
  SOLD = 'SOLD',
  REMOVED = 'REMOVED'
}

// User Types
export interface User {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  college: string;
  hostelOrDorm?: string;
  role: Role;
  activeListingsCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface SellerInfo {
  id: number;
  fullName: string;
  phoneNumber?: string;
  hostelOrDorm?: string;
  college: string;
}

// Auth Types
export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  college: string;
  phoneNumber?: string;
  hostelOrDorm?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  id: number;
  email: string;
  fullName: string;
  college: string;
  role: Role;
  token: string;
  tokenType: string;
}

// Listing Types
export interface ListingImage {
  id: number;
  imageUrl: string;
  isPrimary: boolean;
  createdAt: string;
}

export interface Listing {
  id: number;
  title: string;
  description: string;
  price?: number;
  category: Category;
  transactionType: TransactionType;
  itemCondition: ItemCondition;
  status: ListingStatus;
  images: ListingImage[];
  seller: SellerInfo;
  createdAt: string;
  updatedAt: string;
}

export interface ListingSummary {
  id: number;
  title: string;
  price?: number;
  category: Category;
  transactionType: TransactionType;
  itemCondition: ItemCondition;
  status: ListingStatus;
  primaryImageUrl?: string;
  createdAt: string;
}

export interface CreateListingRequest {
  title: string;
  description: string;
  price?: number;
  category: Category;
  transactionType: TransactionType;
  itemCondition: ItemCondition;
}

export interface UpdateListingRequest {
  title?: string;
  description?: string;
  price?: number;
  category?: Category;
  transactionType?: TransactionType;
  itemCondition?: ItemCondition;
}

// Pagination
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// Wishlist
export interface WishlistItem {
  id: number;
  listing: ListingSummary;
  addedAt: string;
}

// Report
export interface CreateReportRequest {
  listingId: number;
  reason: string;
}

export interface ReportResponse {
  id: number;
  listingId: number;
  listingTitle: string;
  reporterId: number;
  reporterName: string;
  reason: string;
  status: string;
  reportedAt: string;
  resolvedAt?: string;
}

// Error Response
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
