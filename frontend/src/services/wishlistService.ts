import axios from '../lib/axios';
import { WishlistItem } from '../types';

export const wishlistService = {
  async getWishlist(): Promise<WishlistItem[]> {
    const response = await axios.get<WishlistItem[]>('/api/wishlist');
    return response.data;
  },

  async addToWishlist(listingId: number): Promise<WishlistItem> {
    const response = await axios.post<WishlistItem>(`/api/wishlist/${listingId}`);
    return response.data;
  },

  async removeFromWishlist(listingId: number): Promise<void> {
    await axios.delete(`/api/wishlist/${listingId}`);
  },

  async isInWishlist(listingId: number): Promise<boolean> {
    const response = await axios.get<boolean>(`/api/wishlist/check/${listingId}`);
    return response.data;
  }
};
