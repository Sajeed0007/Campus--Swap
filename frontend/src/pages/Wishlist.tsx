import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { wishlistService } from '../services/wishlistService';
import { WishlistItem } from '../types';
import Loading from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';
import EmptyState from '../components/common/EmptyState';
import { Heart, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';

const Wishlist: React.FC = () => {
  const navigate = useNavigate();
  const [wishlist, setWishlist] = useState<WishlistItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchWishlist();
  }, []);

  const fetchWishlist = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await wishlistService.getWishlist();
      setWishlist(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load wishlist');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRemove = async (listingId: number) => {
    try {
      await wishlistService.removeFromWishlist(listingId);
      setWishlist(wishlist.filter((item) => item.listing.id !== listingId));
      toast.success('Removed from wishlist');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to remove item');
    }
  };

  if (isLoading) return <Loading />;
  if (error) return <ErrorMessage message={error} retry={fetchWishlist} />;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">My Wishlist</h1>
        <p className="text-gray-600">{wishlist.length} items saved</p>
      </div>

      {wishlist.length === 0 ? (
        <EmptyState
          icon={Heart}
          title="Your wishlist is empty"
          description="Browse listings and save items you're interested in to your wishlist."
          action={{
            label: 'Browse Listings',
            onClick: () => navigate('/'),
          }}
        />
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {wishlist.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-lg shadow p-6 flex items-center gap-6"
            >
              <div
                onClick={() => navigate(`/listings/${item.listing.id}`)}
                className="w-24 h-24 bg-gray-200 rounded-lg flex-shrink-0 cursor-pointer"
              >
                {item.listing.primaryImageUrl ? (
                  <img
                    src={item.listing.primaryImageUrl}
                    alt={item.listing.title}
                    className="w-full h-full object-cover rounded-lg"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <Heart className="w-8 h-8 text-gray-400" />
                  </div>
                )}
              </div>

              <div className="flex-1 min-w-0">
                <h3
                  onClick={() => navigate(`/listings/${item.listing.id}`)}
                  className="text-lg font-semibold text-gray-900 hover:text-primary-600 cursor-pointer mb-2 truncate"
                >
                  {item.listing.title}
                </h3>

                <div className="flex flex-wrap gap-2 mb-2">
                  <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full">
                    {item.listing.transactionType}
                  </span>
                  <span className="px-2 py-1 bg-gray-100 text-gray-800 text-xs rounded-full">
                    {item.listing.itemCondition.replace('_', ' ')}
                  </span>
                </div>

                <p className="text-sm text-gray-600">
                  Added {new Date(item.addedAt).toLocaleDateString()}
                </p>
              </div>

              <div className="flex flex-col items-end gap-3">
                <span className="text-xl font-bold text-primary-600">
                  {item.listing.price ? `$${item.listing.price.toFixed(2)}` : 'Free'}
                </span>
                <button
                  onClick={() => handleRemove(item.listing.id)}
                  className="p-2 text-gray-600 hover:text-red-600 hover:bg-gray-100 rounded-lg transition-colors"
                  title="Remove from wishlist"
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

export default Wishlist;
