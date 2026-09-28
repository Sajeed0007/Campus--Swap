import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { listingService } from '../../services/listingService';
import { wishlistService } from '../../services/wishlistService';
import { Listing, ListingStatus } from '../../types';
import { useAuth } from '../../context/AuthContext';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ReportModal from '../../components/modals/ReportModal';
import { Heart, Edit, Trash2, Phone, MapPin, Calendar, Flag, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const ListingDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [listing, setListing] = useState<Listing | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isInWishlist, setIsInWishlist] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isMarkingSold, setIsMarkingSold] = useState(false);

  useEffect(() => {
    fetchListing();
    if (isAuthenticated) {
      checkWishlistStatus();
    }
  }, [id, isAuthenticated]);

  const fetchListing = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await listingService.getListingById(Number(id));
      setListing(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load listing');
    } finally {
      setIsLoading(false);
    }
  };

  const checkWishlistStatus = async () => {
    try {
      const status = await wishlistService.isInWishlist(Number(id));
      setIsInWishlist(status);
    } catch (err) {
      // Ignore error
    }
  };

  const handleToggleWishlist = async () => {
    if (!isAuthenticated) {
      toast.error('Please login to add to wishlist');
      navigate('/login');
      return;
    }

    try {
      if (isInWishlist) {
        await wishlistService.removeFromWishlist(Number(id));
        setIsInWishlist(false);
        toast.success('Removed from wishlist');
      } else {
        await wishlistService.addToWishlist(Number(id));
        setIsInWishlist(true);
        toast.success('Added to wishlist');
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this listing?')) return;

    setIsDeleting(true);
    try {
      await listingService.deleteListing(Number(id));
      toast.success('Listing deleted successfully');
      navigate('/my-listings');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to delete listing');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleMarkAsSold = async () => {
    if (!window.confirm('Mark this item as sold?')) return;

    setIsMarkingSold(true);
    try {
      const updated = await listingService.markAsSold(Number(id));
      setListing(updated);
      toast.success('Listing marked as sold');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Operation failed');
    } finally {
      setIsMarkingSold(false);
    }
  };

  if (isLoading) return <Loading />;
  if (error) return <ErrorMessage message={error} retry={fetchListing} />;
  if (!listing) return <ErrorMessage message="Listing not found" />;

  const isOwner = user?.id === listing.seller.id;
  const isSold = listing.status === ListingStatus.SOLD;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Images */}
        <div>
          <div className="bg-gray-200 rounded-lg overflow-hidden aspect-square">
            {listing.images.length > 0 ? (
              <img
                src={listing.images[0].imageUrl}
                alt={listing.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <MapPin className="w-24 h-24 text-gray-400" />
              </div>
            )}
          </div>
        </div>

        {/* Details */}
        <div className="space-y-6">
          <div>
            <div className="flex items-start justify-between mb-2">
              <h1 className="text-3xl font-bold text-gray-900">{listing.title}</h1>
              {isSold && (
                <span className="px-3 py-1 bg-gray-800 text-white text-sm font-medium rounded-full">
                  SOLD
                </span>
              )}
            </div>
            <div className="flex items-center gap-2 mb-4">
              <span className="text-2xl font-bold text-primary-600">
                {listing.price ? `$${listing.price.toFixed(2)}` : 'Free'}
              </span>
              <span className="px-2 py-1 bg-blue-100 text-blue-800 text-sm rounded-full">
                {listing.transactionType}
              </span>
              <span className="px-2 py-1 bg-gray-100 text-gray-800 text-sm rounded-full">
                {listing.itemCondition.replace('_', ' ')}
              </span>
            </div>
          </div>

          <div className="prose max-w-none">
            <p className="text-gray-700">{listing.description}</p>
          </div>

          <div className="border-t pt-6">
            <h3 className="text-lg font-semibold mb-4">Seller Information</h3>
            <div className="space-y-2 text-gray-700">
              <p className="font-medium">{listing.seller.fullName}</p>
              {listing.seller.phoneNumber && (
                <div className="flex items-center gap-2">
                  <Phone className="w-4 h-4" />
                  <span>{listing.seller.phoneNumber}</span>
                </div>
              )}
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4" />
                <span>{listing.seller.college}</span>
              </div>
              {listing.seller.hostelOrDorm && (
                <p className="text-sm text-gray-600">{listing.seller.hostelOrDorm}</p>
              )}
            </div>
          </div>

          <div className="border-t pt-6">
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <Calendar className="w-4 h-4" />
              <span>Posted {new Date(listing.createdAt).toLocaleDateString()}</span>
            </div>
          </div>

          {/* Actions */}
          <div className="flex flex-wrap gap-3 pt-6 border-t">
            {isOwner ? (
              <>
                {!isSold && (
                  <>
                    <Button onClick={() => navigate(`/listings/${id}/edit`)}>
                      <Edit className="w-4 h-4 mr-2" />
                      Edit
                    </Button>
                    <Button
                      onClick={handleMarkAsSold}
                      isLoading={isMarkingSold}
                      variant="secondary"
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      Mark as Sold
                    </Button>
                  </>
                )}
                <Button
                  onClick={handleDelete}
                  isLoading={isDeleting}
                  variant="danger"
                >
                  <Trash2 className="w-4 h-4 mr-2" />
                  Delete
                </Button>
              </>
            ) : (
              <>
                {!isSold && (
                  <Button onClick={handleToggleWishlist} variant="outline">
                    <Heart className={`w-4 h-4 mr-2 ${isInWishlist ? 'fill-current' : ''}`} />
                    {isInWishlist ? 'Remove from Wishlist' : 'Add to Wishlist'}
                  </Button>
                )}
                <Button onClick={() => setShowReportModal(true)} variant="outline">
                  <Flag className="w-4 h-4 mr-2" />
                  Report
                </Button>
              </>
            )}
          </div>
        </div>
      </div>

      {showReportModal && (
        <ReportModal
          listingId={Number(id)}
          onClose={() => setShowReportModal(false)}
        />
      )}
    </div>
  );
};

export default ListingDetail;
