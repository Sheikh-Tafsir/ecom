import React, { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card.jsx';
import { Input } from '@/components/ui/input.jsx';
import StaredLabel from '@/components/common/StaredLabel';
import { Axios } from '@/services/http/Axios';
import { ButtonLoading } from "@/components/common/ButtonLoading";
import { TOAST_TYPE } from '@/utils/enums';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import { ToastAlert } from '@/components/common/ToastAlert';
import { URL_NOT_FOUND } from '@/utils';

const InventoryCreate = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [inventory, setInventory] = useState({
    quantityPurchased: '',
    cost: '',
  });
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState({ page: false, button: false });
  const [toastData, setToastData] = useState({ message: "", type: "", id: Date.now() });

  const handleError = (error) => {
    console.error(error);
    if ([403, 404].includes(error?.status)) navigate(URL_NOT_FOUND, { replace: true });
    setErrors(error.response?.data || { global: error.message });
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setIsLoading({ ...isLoading, button: true });
    setErrors({});

    try {
      await Axios.post(`/products/${id}/inventory`, {
        ...inventory
      });

      setInventory({
        quantityPurchased: '',
        cost: '',
      });

      showToast("Successfully created", TOAST_TYPE.SUCCESS);
    } catch (error) {
      handleError(error);
    } finally {
      setIsLoading({ ...isLoading, button: false });
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setInventory((prev) => ({ ...prev, [name]: value }));
  };

  const showToast = (message, type) => {
    setToastData({ message, type, id: Date.now() });
  };

  return (
    <>
      {isLoading.page && <PageLoadingOverlay />}

      <div className="container lg:flex min-h-[100vh] mt-[-60px]">
        <Card className="mx-auto my-auto w-[450px]">
          <form onSubmit={handleSave}>
            <CardHeader>
              <CardTitle>Restock inventory</CardTitle>
              <CardDescription>Add  by filling out the information below</CardDescription>
            </CardHeader>

            <CardContent className="space-y-4">
              <div className='space-y-2'>
                <StaredLabel label="Quantity" />
                <Input
                  type="number"
                  name="quantityPurchased"
                  placeholder="10"
                  value={inventory.quantityPurchased || ''}
                  onChange={handleChange}
                  min={1}
                  required
                />
                {errors.quantityPurchased && <p className='validation-error'>{errors.quantityPurchased}</p>}
              </div>


              <div className='space-y-2'>
                <StaredLabel label="Cost" />
                <Input
                  type="number"
                  name="cost"
                  placeholder="100"
                  value={inventory.cost || ''}
                  onChange={handleChange}
                  min={1}
                  required
                />
                {errors.price && <p className='validation-error'>{errors.cost}</p>}
              </div>

              {errors.global && <p className="validation-error">{errors.global}</p>}
            </CardContent>

            <CardFooter className="flex-col gap-2">
              {isLoading.button ? (
                <ButtonLoading />
              ) : (
                <Button type="submit" className="w-full cursor-pointer" style={{ backgroundColor: 'rgb(24,62,139)' }}>
                  Restock
                </Button>
              )}

              <Link to="/inventories" className='text-xs text-blue-600 mt-2'>Back to inventory list</Link>
            </CardFooter>
          </form>
        </Card>
      </div>

      <ToastAlert
        key={toastData.id}
        message={toastData.message}
        type={toastData.type}
      />
    </>
  );
};

export default InventoryCreate;