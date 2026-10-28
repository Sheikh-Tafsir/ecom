export const queryKeys = {
    banners: {
        all: ['banners'],
    },
    blogs: {
        all: ['blogs'],
        list: (filters) => ['blogs', { ...filters }],
    },
    categories: {
        all: ['categories'],
    },
    chats: {
        all: ['chats'],
        selected: (id) => ['selected_chat', id],
    },
    faqs: {
        all: ['faqs'],
    },
    orders: {
        all: ['orders'],
        list: (filters) => ['orders', { ...filters }],
        detail: (id) => ['orders', id],
    },
    products: {
        all: ['products'],
        list: (filters) => ['products', { ...filters }],
        detail: (id) => ['products', id],
        edit: (id) => ['products', 'edit', id],
    },
    profile: {
        all: ['profile'],
    },
    reviews: {
        all: (productId) => ['reviews', productId],
    },
    roles: {
        all: ['roles'],
        detail: (id) => ['roles', id],
    },
    sales: {
        all: ['sales'],
        list: (filters) => ['sales', { ...filters }],
    },
    stockItems: {
        all: ['stockItems'],
        list: (filters) => ['stockItems', { ...filters }],
    },
    stocks: {
        all: ['stock'],
        list: (filters) => ['stock', { ...filters }],
        detail: (id) => ['stock', id],
    },
    // Alias: some components use queryKeys.stock (singular)
    get stock() { return this.stocks; },
    users: {
        all: ['users'],
        list: (filters) => ['users', { ...filters }],
        detail: (id) => ['users', id],
    }
};
