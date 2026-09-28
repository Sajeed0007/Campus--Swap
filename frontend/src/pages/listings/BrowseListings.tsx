import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { listingService, ListingFilters } from '../../services/listingService';
import { ListingSummary, Category, TransactionType } from '../../types';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import ListingCard from '../../components/listings/ListingCard';
import SearchFilter from '../../components/listings/SearchFilter';
import { Package } from 'lucide-react';

const BrowseListings: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [listings, setListings] = useState<ListingSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [filters, setFilters] = useState<ListingFilters>({
    search: searchParams.get('search') || '',
    category: searchParams.get('category') as Category | undefined,
    transactionType: searchParams.get('type') as TransactionType | undefined,
    minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
    maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
    page: Number(searchParams.get('page')) || 0,
    size: 20,
    sortBy: 'createdAt',
    sortDirection: 'DESC',
  });

  useEffect(() => {
    fetchListings();
  }, [filters]);

  const fetchListings = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await listingService.getAllListings(filters);
      setListings(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load listings');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFilterChange = (newFilters: Partial<ListingFilters>) => {
    const updatedFilters = { ...filters, ...newFilters, page: 0 };
    setFilters(updatedFilters);

    // Update URL params
    const params = new URLSearchParams();
    if (updatedFilters.search) params.set('search', updatedFilters.search);
    if (updatedFilters.category) params.set('category', updatedFilters.category);
    if (updatedFilters.transactionType) params.set('type', updatedFilters.transactionType);
    if (updatedFilters.minPrice) params.set('minPrice', updatedFilters.minPrice.toString());
    if (updatedFilters.maxPrice) params.set('maxPrice', updatedFilters.maxPrice.toString());
    setSearchParams(params);
  };

  const handlePageChange = (newPage: number) => {
    setFilters({ ...filters, page: newPage });
    window.scrollTo(0, 0);
  };

  if (isLoading && listings.length === 0) {
    return <Loading />;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Browse Listings</h1>
        <p className="text-gray-600">
          {totalElements} {totalElements === 1 ? 'item' : 'items'} available
        </p>
      </div>

      <SearchFilter filters={filters} onFilterChange={handleFilterChange} />

      {error ? (
        <ErrorMessage message={error} retry={fetchListings} />
      ) : listings.length === 0 ? (
        <EmptyState
          icon={Package}
          title="No listings found"
          description="Try adjusting your search filters or check back later for new items."
        />
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {listings.map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                onClick={() => navigate(`/listings/${listing.id}`)}
              />
            ))}
          </div>

          {totalPages > 1 && (
            <div className="mt-8 flex justify-center">
              <nav className="flex items-center gap-2">
                <button
                  onClick={() => handlePageChange(filters.page! - 1)}
                  disabled={filters.page === 0}
                  className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                >
                  Previous
                </button>
                
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => handlePageChange(i)}
                    className={`px-3 py-2 rounded-lg border ${
                      filters.page === i
                        ? 'bg-primary-600 text-white border-primary-600'
                        : 'border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}

                <button
                  onClick={() => handlePageChange(filters.page! + 1)}
                  disabled={filters.page === totalPages - 1}
                  className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                >
                  Next
                </button>
              </nav>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default BrowseListings;
