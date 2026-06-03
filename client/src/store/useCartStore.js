import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { LOCAL_STORAGE_CART } from '@/utils';

export const useCartStore = create(
    persist(
        (set, get) => ({
            cart: [],

            addToCart: (product, quantity = 1) => {
                const cart = get().cart;
                const existingItem = cart.find(item => item.id === product.id);
                let isNew = false;

                if (existingItem) {
                    set({
                        cart: cart.map(item =>
                            item.id === product.id
                                ? { ...item, quantity: item.quantity + quantity }
                                : item
                        )
                    });
                } else {
                    set({ cart: [...cart, { ...product, quantity }] });
                    isNew = true;
                }
                return isNew ? 1 : 0;
            },

            removeFromCart: (productId) => {
                set({ cart: get().cart.filter(item => item.id !== productId) });
            },

            updateQuantity: (productId, quantity) => {
                if (quantity < 1) {
                    get().removeFromCart(productId);
                    return;
                }
                set({
                    cart: get().cart.map(item =>
                        item.id === productId ? { ...item, quantity } : item
                    )
                });
            },

            clearCart: () => {
                set({ cart: [] });
            },

            getCartTotal: () => {
                return get().cart.reduce((acc, item) => acc + item.price * item.quantity, 0);
            },

            getCartCount: () => {
                return get().cart.reduce((acc, item) => acc + item.quantity, 0);
            },
        }),
        {
            name: LOCAL_STORAGE_CART,
            storage: createJSONStorage(() => localStorage),
        }
    )
);
