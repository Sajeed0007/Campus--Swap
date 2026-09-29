import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { listingService } from '../../services/listingService';
import { Category, TransactionType, ItemCondition } from '../../types';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Select from '../../components/common/Select';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import toast from 'react-hot-toast';

const EditListing: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    price: '',
    category: '' as Category,
    transactionType: '' as TransactionType,
    itemCondition: '' as ItemCondition,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    fetchListing();
  }, [id]);

  const fetchListing = async () => {
    try {
      const data = await listingService.getListingById(Number(id));
      // The fetched listing is only needed to seed the form, so it is not held in
      // state. formData is the single source of truth for the edit form.
      setFormData({
        title: data.title,
        description: data.description,
        price: data.price?.toString() || '',
        category: data.category,
        transactionType: data.transactionType,
        itemCondition: data.itemCondition,
      });
    } catch (err) {
      toast.error('Failed to load listing');
      navigate('/my-listings');
    } finally {
      setIsLoading(false);
    }
  };

  const categoryOptions = Object.values(Category).map((cat) => ({
    value: cat,
    label: cat.replace('_', ' '),
  }));

  const typeOptions = Object.values(TransactionType).map((type) => ({
    value: type,
    label: type,
  }));

  const conditionOptions = Object.values(ItemCondition).map((cond) => ({
    value: cond,
    label: cond.replace('_', ' '),
  }));

  const validate = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.title) newErrors.title = 'Title is required';
    if (!formData.description) newErrors.description = 'Description is required';
    if (!formData.category) newErrors.category = 'Category is required';
    if (!formData.transactionType) newErrors.transactionType = 'Transaction type is required';
    if (!formData.itemCondition) newErrors.itemCondition = 'Condition is required';

    if (formData.transactionType === TransactionType.SELL && !formData.price) {
      newErrors.price = 'Price is required for selling items';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    setIsSaving(true);
    try {
      await listingService.updateListing(Number(id), {
        title: formData.title,
        description: formData.description,
        price: formData.price ? Number(formData.price) : undefined,
        category: formData.category,
        transactionType: formData.transactionType,
        itemCondition: formData.itemCondition,
      });

      toast.success('Listing updated successfully!');
      navigate(`/listings/${id}`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to update listing');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) return <Loading />;

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="bg-white rounded-lg shadow p-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Edit Listing</h1>

        <form onSubmit={handleSubmit} className="space-y-6">
          <Input
            label="Title"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            error={errors.title}
            required
          />

          <Textarea
            label="Description"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            error={errors.description}
            required
            rows={5}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Select
              label="Category"
              value={formData.category}
              onChange={(e) => setFormData({ ...formData, category: e.target.value as Category })}
              error={errors.category}
              options={categoryOptions}
              required
            />

            <Select
              label="Transaction Type"
              value={formData.transactionType}
              onChange={(e) => setFormData({ ...formData, transactionType: e.target.value as TransactionType })}
              error={errors.transactionType}
              options={typeOptions}
              required
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Select
              label="Condition"
              value={formData.itemCondition}
              onChange={(e) => setFormData({ ...formData, itemCondition: e.target.value as ItemCondition })}
              error={errors.itemCondition}
              options={conditionOptions}
              required
            />

            <Input
              label="Price"
              type="number"
              min="0"
              step="0.01"
              value={formData.price}
              onChange={(e) => setFormData({ ...formData, price: e.target.value })}
              error={errors.price}
              required={formData.transactionType === TransactionType.SELL}
            />
          </div>

          <div className="flex gap-3 pt-6 border-t">
            <Button type="submit" isLoading={isSaving}>
              Save Changes
            </Button>
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditListing;
