import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

const LOCAL_STORAGE_CART = import.meta.env.VITE_LOCAL_STORAGE_CART;

export const useCartStore = create(
    persist(
        (set, get) => ({
            cart: [],

            addToCart: (product, quantity = 1) => {
                const cart = get().cart;

                const existingItem = cart.find(
                    item => item.productId == product.id
                );

                if (existingItem) {
                    set({
                        cart: cart.map(item =>
                            item.productId == product.id
                                ? {
                                    ...item,
                                    quantity: item.quantity + quantity,
                                    stock: product.quantity,
                                }
                                : item
                        ),
                    });

                    return 0;
                }

                set({
                    cart: [
                        ...cart,
                        {
                            productId: product.id,
                            name: product.name,
                            description: product.description,
                            image: product.image ?? product.images?.[0]?.image ?? null,
                            price: product.price,
                            quantity,
                            stock: product.quantity,
                        },
                    ],
                });

                return 1;
            },

            removeFromCart: (productId) => {
                set({
                    cart: get().cart.filter(
                        item => item.productId != productId
                    ),
                });
            },

            updateQuantity: (productId, quantity) => {
                if (quantity <= 0) {
                    get().removeFromCart(productId);
                    return;
                }

                set({
                    cart: get().cart.map(item =>
                        item.productId == productId
                            ? { ...item, quantity }
                            : item
                    ),
                });
            },

            clearCart: () => {
                set({ cart: [] });
            },

            getCartTotal: () =>
                get().cart.reduce(
                    (total, item) => total + item.price * item.quantity,
                    0
                ),

            getCartCount: () =>
                get().cart.reduce(
                    (count, item) => count + item.quantity,
                    0
                ),
        }),
        {
            name: LOCAL_STORAGE_CART,
            storage: createJSONStorage(() => localStorage),
        }
    )
);