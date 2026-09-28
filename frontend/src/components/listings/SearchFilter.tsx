import React, { useState } from 'react';
import { Search, Filter, X } from 'lucide-react';
import { ListingFilters } from '../../services/listingService';
import { Category, TransactionType } from '../../types';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';

interface SearchFilterProps {
  filters: ListingFilters;
  onFilterChange: (filters: Partial<ListingFilters>) => void;
}

const SearchFilter: React.FC<SearchFilterProps> = ({ filters, onFilterChange }) => {
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [localFilters, setLocalFilters] = useState(filters);

  const categoryOptions = Object.values(Category).map((cat) => ({
    value: cat,
    label: cat.replace('_', ' '),
  }));

  const typeOptions = Object.values(TransactionType).map((type) => ({
    value: type,
    label: type,
  }));

  const handleSearchChange = (value: string) => {
    setLocalFilters({ ...localFilters, search: value });
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onFilterChange(localFilters);
  };

  const handleClearFilters = () => {
    const cleared = {
      search: '',
      category: undefined,
      transactionType: undefined,
      minPrice: undefined,
      maxPrice: undefined,
    };
    setLocalFilters(cleared);
    onFilterChange(cleared);
    setShowAdvanced(false);
  };

  const hasActiveFilters = 
    filters.search || 
    filters.category || 
    filters.transactionType || 
    filters.minPrice || 
    filters.maxPrice;

  return (
    <div className="bg-white rounded-lg shadow p-4 mb-6">
      <form onSubmit={handleSearchSubmit} className="space-y-4">
        {/* Search Bar */}
        <div className="flex gap-2">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search listings..."
              value={localFilters.search || ''}
              onChange={(e) => handleSearchChange(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <Button type="submit">
            Search
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => setShowAdvanced(!showAdvanced)}
          >
            <Filter className="w-5 h-5" />
          </Button>
        </div>

        {/* Advanced Filters */}
        {showAdvanced && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 pt-4 border-t">
            <Select
              label="Category"
              value={localFilters.category || ''}
              onChange={(e) =>
                setLocalFilters({
                  ...localFilters,
                  category: e.target.value as Category | undefined,
                })
              }
              options={categoryOptions}
            />

            <Select
              label="Type"
              value={localFilters.transactionType || ''}
              onChange={(e) =>
                setLocalFilters({
                  ...localFilters,
                  transactionType: e.target.value as TransactionType | undefined,
                })
              }
              options={typeOptions}
            />

            <Input
              label="Min Price"
              type="number"
              min="0"
              step="0.01"
              value={localFilters.minPrice || ''}
              onChange={(e) =>
                setLocalFilters({
                  ...localFilters,
                  minPrice: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />

            <Input
              label="Max Price"
              type="number"
              min="0"
              step="0.01"
              value={localFilters.maxPrice || ''}
              onChange={(e) =>
                setLocalFilters({
                  ...localFilters,
                  maxPrice: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
          </div>
        )}

        {/* Active Filters */}
        {hasActiveFilters && (
          <div className="flex items-center gap-2 pt-2 border-t">
            <span className="text-sm text-gray-600">Active filters:</span>
            <div className="flex flex-wrap gap-2">
              {filters.search && (
                <span className="px-2 py-1 bg-primary-100 text-primary-800 text-sm rounded-full">
                  Search: {filters.search}
                </span>
              )}
              {filters.category && (
                <span className="px-2 py-1 bg-primary-100 text-primary-800 text-sm rounded-full">
                  {filters.category.replace('_', ' ')}
                </span>
              )}
              {filters.transactionType && (
                <span className="px-2 py-1 bg-primary-100 text-primary-800 text-sm rounded-full">
                  {filters.transactionType}
                </span>
              )}
              {(filters.minPrice || filters.maxPrice) && (
                <span className="px-2 py-1 bg-primary-100 text-primary-800 text-sm rounded-full">
                  ${filters.minPrice || 0} - ${filters.maxPrice || '∞'}
                </span>
              )}
            </div>
            <button
              type="button"
              onClick={handleClearFilters}
              className="ml-auto text-sm text-red-600 hover:text-red-700 flex items-center gap-1"
            >
              <X className="w-4 h-4" />
              Clear all
            </button>
          </div>
        )}
      </form>
    </div>
  );
};

export default SearchFilter;
