import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { listingService } from '../../services/listingService';
import { Listing, ListingStatus } from '../../types';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import Button from '../../components/common/Button';
import { Package, Plus, Edit, Trash2, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const MyListings: React.FC = () => {
  const navigate = useNavigate();
  const [listings, setListings] = useState<Listing[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchMyListings();
  }, []);

  const fetchMyListings = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await listingService.getMyListings();
      setListings(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load listings');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this listing?')) return;

    try {
      await listingService.deleteListing(id);
      setListings(listings.filter((l) => l.id !== id));
      toast.success('Listing deleted successfully');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to delete listing');
    }
  };

  const handleMarkAsSold = async (id: number) => {
    if (!window.confirm('Mark this item as sold?')) return;

    try {
      const updated = await listingService.markAsSold(id);
      setListings(listings.map((l) => (l.id === id ? updated : l)));
      toast.success('Listing marked as sold');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  if (isLoading) return <Loading />;
  if (error) return <ErrorMessage message={error} retry={fetchMyListings} />;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My Listings</h1>
          <p className="text-gray-600 mt-1">{listings.length} items</p>
        </div>
        <Button onClick={() => navigate('/listings/create')}>
          <Plus className="w-4 h-4 mr-2" />
          Create Listing
        </Button>
      </div>

      {listings.length === 0 ? (
        <EmptyState
          icon={Package}
          title="No listings yet"
          description="Create your first listing to start selling, swapping, or donating items."
          action={{
            label: 'Create Listing',
            onClick: () => navigate('/listings/create'),
          }}
        />
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {listings.map((listing) => (
            <div
              key={listing.id}
              className="bg-white rounded-lg shadow p-6 flex items-center gap-6"
            >
              <div className="w-24 h-24 bg-gray-200 rounded-lg flex-shrink-0">
                {listing.images[0] ? (
                  <img
                    src={listing.images[0].imageUrl}
                    alt={listing.title}
                    className="w-full h-full object-cover rounded-lg"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <Package className="w-8 h-8 text-gray-400" />
                  </div>
                )}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between mb-2">
                  <h3
                    onClick={() => navigate(`/listings/${listing.id}`)}
                    className="text-lg font-semibold text-gray-900 hover:text-primary-600 cursor-pointer truncate"
                  >
                    {listing.title}
                  </h3>
                  <span className="text-lg font-bold text-primary-600 ml-4">
                    {listing.price ? `$${listing.price.toFixed(2)}` : 'Free'}
                  </span>
                </div>

                <div className="flex flex-wrap gap-2 mb-2">
                  <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full">
                    {listing.transactionType}
                  </span>
                  <span className="px-2 py-1 bg-gray-100 text-gray-800 text-xs rounded-full">
                    {listing.itemCondition.replace('_', ' ')}
                  </span>
                  {listing.status === ListingStatus.SOLD && (
                    <span className="px-2 py-1 bg-gray-800 text-white text-xs rounded-full">
                      SOLD
                    </span>
                  )}
                </div>

                <p className="text-sm text-gray-600 line-clamp-2">{listing.description}</p>
              </div>

              <div className="flex flex-col gap-2">
                {listing.status === ListingStatus.AVAILABLE && (
                  <>
                    <button
                      onClick={() => navigate(`/listings/${listing.id}/edit`)}
                      className="p-2 text-gray-600 hover:text-primary-600 hover:bg-gray-100 rounded-lg transition-colors"
                      title="Edit"
                    >
                      <Edit className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => handleMarkAsSold(listing.id)}
                      className="p-2 text-gray-600 hover:text-green-600 hover:bg-gray-100 rounded-lg transition-colors"
                      title="Mark as Sold"
                    >
                      <CheckCircle className="w-5 h-5" />
                    </button>
                  </>
                )}
                <button
                  onClick={() => handleDelete(listing.id)}
                  className="p-2 text-gray-600 hover:text-red-600 hover:bg-gray-100 rounded-lg transition-colors"
                  title="Delete"
                >
                  <Trash2 className="w-5 h-5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MyListings;
