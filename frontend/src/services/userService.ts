import axios from '../lib/axios';
import { User } from '../types';

export interface UpdateUserRequest {
  fullName?: string;
  phoneNumber?: string;
  hostelOrDorm?: string;
}

export const userService = {
  async getCurrentUser(): Promise<User> {
    const response = await axios.get<User>('/api/users/me');
    return response.data;
  },

  async updateCurrentUser(data: UpdateUserRequest): Promise<User> {
    const response = await axios.put<User>('/api/users/me', data);
    return response.data;
  },

  async getUserById(id: number): Promise<User> {
    const response = await axios.get<User>(`/api/users/${id}`);
    return response.data;
  }
};
