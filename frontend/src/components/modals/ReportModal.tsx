import React, { useState } from 'react';
import { reportService } from '../../services/reportService';
import Textarea from '../common/Textarea';
import Button from '../common/Button';
import { X } from 'lucide-react';
import toast from 'react-hot-toast';

interface ReportModalProps {
  listingId: number;
  onClose: () => void;
}

const ReportModal: React.FC<ReportModalProps> = ({ listingId, onClose }) => {
  const [reason, setReason] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!reason.trim()) {
      setError('Please provide a reason for reporting');
      return;
    }

    setIsLoading(true);
    try {
      await reportService.createReport({ listingId, reason });
      toast.success('Report submitted successfully');
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to submit report');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-900">Report Listing</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Textarea
            label="Reason for reporting"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              setError('');
            }}
            error={error}
            required
            rows={5}
            placeholder="Please describe why you're reporting this listing..."
          />

          <div className="flex gap-3">
            <Button type="submit" isLoading={isLoading} fullWidth>
              Submit Report
            </Button>
            <Button type="button" variant="outline" onClick={onClose} fullWidth>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ReportModal;
