import { useEffect } from 'react';

/**
 * Reusable SEO component for managing document titles, meta tags,
 * canonical links, Open Graph properties, and JSON-LD schema markup.
 *
 * @param {Object} props
 * @param {string} [props.title] - Page title (appends "| Aspire" if not included)
 * @param {string} [props.description] - Page meta description (150-160 chars recommended)
 * @param {string} [props.canonicalUrl] - Relative or absolute canonical URL
 * @param {string} [props.ogImage] - Absolute or relative URL to the preview image
 * @param {string} [props.ogType='website'] - Open Graph type ('website', 'product', 'article')
 * @param {boolean} [props.noindex=false] - If true, sets robots to 'noindex, nofollow'
 * @param {Object|Array} [props.schema] - JSON-LD schema object(s) for structured data
 */
export default function SEO({
    title,
    description,
    canonicalUrl,
    ogImage,
    ogType = 'website',
    noindex = false,
    schema = null,
}) {
    useEffect(() => {
        const defaultTitle = 'Aspire | Modern E-Commerce';
        const siteName = 'Aspire';
        const origin = window.location.origin;

        // 1. Title
        const formattedTitle = title
            ? (title.includes(siteName) ? title : `${title} | ${siteName}`)
            : defaultTitle;
        document.title = formattedTitle;

        // Helper to update or create a meta tag
        const setMetaTag = (attribute, attrValue, content) => {
            if (!content) return;
            let tag = document.querySelector(`meta[${attribute}="${attrValue}"]`);
            if (!tag) {
                tag = document.createElement('meta');
                tag.setAttribute(attribute, attrValue);
                document.head.appendChild(tag);
            }
            tag.setAttribute('content', content);
        };

        // 2. Meta description
        if (description) {
            setMetaTag('name', 'description', description);
            setMetaTag('property', 'og:description', description);
            setMetaTag('name', 'twitter:description', description);
        }

        // 3. Robots
        setMetaTag('name', 'robots', noindex ? 'noindex, nofollow' : 'index, follow');

        // 4. Open Graph & Twitter basic tags
        setMetaTag('property', 'og:title', formattedTitle);
        setMetaTag('name', 'twitter:title', formattedTitle);
        setMetaTag('property', 'og:type', ogType);

        const currentUrl = canonicalUrl
            ? (canonicalUrl.startsWith('http') ? canonicalUrl : `${origin}${canonicalUrl}`)
            : window.location.href;
        setMetaTag('property', 'og:url', currentUrl);

        // 5. Image
        if (ogImage) {
            const absoluteImageUrl = ogImage.startsWith('http') ? ogImage : `${origin}${ogImage}`;
            setMetaTag('property', 'og:image', absoluteImageUrl);
            setMetaTag('name', 'twitter:image', absoluteImageUrl);
        }

        // 6. Canonical link
        let canonicalTag = document.querySelector('link[rel="canonical"]');
        if (!canonicalTag) {
            canonicalTag = document.createElement('link');
            canonicalTag.setAttribute('rel', 'canonical');
            document.head.appendChild(canonicalTag);
        }
        canonicalTag.setAttribute('href', currentUrl);

        // 7. Structured Data (JSON-LD)
        const schemaId = 'seo-structured-data';
        let scriptTag = document.getElementById(schemaId);
        if (schema) {
            if (!scriptTag) {
                scriptTag = document.createElement('script');
                scriptTag.setAttribute('id', schemaId);
                scriptTag.setAttribute('type', 'application/ld+json');
                document.head.appendChild(scriptTag);
            }
            scriptTag.textContent = JSON.stringify(schema);
        } else if (scriptTag) {
            scriptTag.remove();
        }

        // Cleanup on unmount or page change
        return () => {
            const dynamicScript = document.getElementById(schemaId);
            if (dynamicScript) {
                dynamicScript.remove();
            }
        };
    }, [title, description, canonicalUrl, ogImage, ogType, noindex, schema]);

    return null;
}
