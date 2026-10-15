/**
 * Script to automatically generate sitemap.xml for Aspire E-Commerce Platform.
 * Run with: node scripts/generate-sitemap.js
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BASE_URL = process.env.VITE_SITE_URL || 'https://aspire-shop.com';
const API_URL = process.env.VITE_API_BASE_URL || 'http://localhost:8080';
const SITEMAP_PATH = path.resolve(__dirname, '../public/sitemap.xml');

const staticRoutes = [
    { loc: '/', priority: '1.0', changefreq: 'daily' },
    { loc: '/products', priority: '0.9', changefreq: 'daily' },
    { loc: '/blogs', priority: '0.8', changefreq: 'weekly' },
    { loc: '/about', priority: '0.6', changefreq: 'monthly' },
    { loc: '/terms', priority: '0.3', changefreq: 'yearly' },
    { loc: '/privacy', priority: '0.3', changefreq: 'yearly' },
];

async function fetchDynamicRoutes() {
    const dynamicRoutes = [];
    const today = new Date().toISOString().split('T')[0];

    try {
        // Attempt to fetch public products if backend is accessible
        const productRes = await fetch(`${API_URL}/products?size=100`);
        if (productRes.ok) {
            const data = await productRes.json();
            const products = data?.data?.content || data?.data || [];
            products.forEach((p) => {
                if (p.id) {
                    dynamicRoutes.push({
                        loc: `/products/${p.id}`,
                        lastmod: p.updatedAt ? p.updatedAt.split('T')[0] : today,
                        priority: '0.8',
                        changefreq: 'weekly',
                    });
                }
            });
            console.log(`[Sitemap] Added ${dynamicRoutes.length} products to sitemap.`);
        }
    } catch (err) {
        console.warn(`[Sitemap] Backend not reachable at ${API_URL}, skipping dynamic product fetch.`);
    }

    try {
        // Attempt to fetch public blogs
        const blogRes = await fetch(`${API_URL}/blogs?size=50`);
        if (blogRes.ok) {
            const data = await blogRes.json();
            const blogs = data?.data?.content || data?.data || [];
            blogs.forEach((b) => {
                if (b.title) {
                    dynamicRoutes.push({
                        loc: `/blogs/${encodeURIComponent(b.title)}`,
                        lastmod: b.updatedAt ? b.updatedAt.split('T')[0] : today,
                        priority: '0.7',
                        changefreq: 'monthly',
                    });
                }
            });
        }
    } catch (err) {
        // ignore if offline
    }

    return dynamicRoutes;
}

async function generate() {
    const today = new Date().toISOString().split('T')[0];
    const dynamicRoutes = await fetchDynamicRoutes();
    const allRoutes = [...staticRoutes, ...dynamicRoutes];

    const xml = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${allRoutes
    .map(
        (r) => `    <url>
        <loc>${BASE_URL}${r.loc}</loc>
        <lastmod>${r.lastmod || today}</lastmod>
        <changefreq>${r.changefreq}</changefreq>
        <priority>${r.priority}</priority>
    </url>`
    )
    .join('\n')}
</urlset>
`;

    fs.writeFileSync(SITEMAP_PATH, xml, 'utf8');
    console.log(`[Sitemap] Successfully wrote ${allRoutes.length} URLs to ${SITEMAP_PATH}`);
}

generate();
