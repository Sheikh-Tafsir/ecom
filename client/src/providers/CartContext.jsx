import { LOCAL_STORAGE_CART } from '@/utils';
import { createContext, useContext, useState, useEffect, useRef } from 'react';

const CartContext = createContext();

export const useCartContext = () => useContext(CartContext);

export const CartProvider = ({ children }) => {
    const [cart, setCart] = useState([]);
    const didMount = useRef(false);

    // Load cart from localStorage on mount
    useEffect(() => {
        const storedCart = localStorage.getItem(LOCAL_STORAGE_CART);
        if (storedCart?.length > 0) {
            setCart(JSON.parse(storedCart));
        }
    }, []);

    // Sync cart to localStorage whenever it changes
    useEffect(() => {
        if (didMount.current) {
            localStorage.setItem(LOCAL_STORAGE_CART, JSON.stringify(cart));
        } else {
            didMount.current = true;
        }
    }, [cart]);

    // Add or update product in cart
    // const addToCart = (product, quantity = 1) => {
    //     setCart(prevCart => {
    //         const existingItem = prevCart.find(item => item.id === product.id);
    //         if (existingItem) {
    //             return prevCart.map(item =>
    //                 item.id === product.id
    //                     ? { ...item, quantity: item.quantity + quantity }
    //                     : item
    //             );
    //         }
    //         return [...prevCart, { ...product, quantity }];
    //     });
    // };
    const addToCart = (product, quantity = 1) => {
        const isNew = !cart.some(p => p.id === product.id); // cart must be from state or context

        setCart(prevCart => {
            const existingItem = prevCart.find(p => p.id === product.id);
            if (existingItem) {
                return prevCart.map(p =>
                    p.id === product.id
                        ? { ...p, quantity: p.quantity + quantity }
                        : p
                );
            }
            return [...prevCart, { ...product, quantity }];
        });

        return isNew ? 1 : 0;
    };


    const removeFromCart = (productId) => {
        setCart(prevCart => prevCart.filter(item => item.id !== productId));
    };

    const updateQuantity = (productId, quantity) => {
        if (quantity < 1) return removeFromCart(productId);
        setCart(prevCart =>
            prevCart.map(item =>
                item.id === productId ? { ...item, quantity } : item
            )
        );
    };

    const clearCart = () => {
        setCart([]);
    };

    const cartTotal = cart.reduce((acc, item) => acc + item.price * item.quantity, 0);

    return (
        <CartContext.Provider
            value={{
                cart,
                addToCart,
                removeFromCart,
                updateQuantity,
                clearCart,
                cartTotal,
            }}
        >
            {children}
        </CartContext.Provider>
    );
};
