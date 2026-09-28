import React from 'react';
import { ListingSummary } from '../../types';
import { MapPin, Tag } from 'lucide-react';

interface ListingCardProps {
  listing: ListingSummary;
  onClick: () => void;
}

const ListingCard: React.FC<ListingCardProps> = ({ listing, onClick }) => {
  const formatPrice = (price?: number) => {
    if (!price) return 'Free';
    return `$${price.toFixed(2)}`;
  };

  const getTransactionBadge = () => {
    const colors = {
      SELL: 'bg-green-100 text-green-800',
      SWAP: 'bg-blue-100 text-blue-800',
      DONATE: 'bg-purple-100 text-purple-800',
    };
    return (
      <span className={`px-2 py-1 text-xs font-medium rounded-full ${colors[listing.transactionType]}`}>
        {listing.transactionType}
      </span>
    );
  };

  const getConditionBadge = () => {
    const label = listing.itemCondition.replace('_', ' ');
    return (
      <span className="px-2 py-1 text-xs font-medium rounded-full bg-gray-100 text-gray-800">
        {label}
      </span>
    );
  };

  return (
    <div
      onClick={onClick}
      className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden"
    >
      <div className="aspect-w-16 aspect-h-12 bg-gray-200 relative">
        {listing.primaryImageUrl ? (
          <img
            src={listing.primaryImageUrl}
            alt={listing.title}
            className="w-full h-48 object-cover"
          />
        ) : (
          <div className="w-full h-48 flex items-center justify-center bg-gray-100">
            <Tag className="w-12 h-12 text-gray-400" />
          </div>
        )}
      </div>

      <div className="p-4">
        <div className="flex items-start justify-between gap-2 mb-2">
          <h3 className="text-lg font-semibold text-gray-900 line-clamp-2 flex-1">
            {listing.title}
          </h3>
          <span className="text-lg font-bold text-primary-600">
            {formatPrice(listing.price)}
          </span>
        </div>

        <div className="flex flex-wrap gap-2 mb-3">
          {getTransactionBadge()}
          {getConditionBadge()}
        </div>

        <div className="flex items-center gap-1 text-sm text-gray-600">
          <MapPin className="w-4 h-4" />
          <span className="truncate">{listing.category.replace('_', ' ')}</span>
        </div>
      </div>
    </div>
  );
};

export default ListingCard;
