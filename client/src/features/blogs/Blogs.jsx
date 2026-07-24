import React from 'react';
import { useUserStore } from '@/store/useUserStore';
import { hasPermission } from '@/utils/AuthUtils';
import { PERMISSION } from '@/utils/enums';
import BlogManager from './BlogManager';
import BlogList from './BlogList';

const Blogs = () => {
    const { user } = useUserStore();
    const isAdmin = hasPermission(user, [PERMISSION.ADMIN_ACCESS, PERMISSION.SUPER_ADMIN_ACCESS]);

    if (isAdmin) {
        return <BlogManager />;
    }

    return <BlogList />;
};

export default Blogs;
