import React from 'react';

const PrivacyPolicy = () => {
    return (
        <div className="container mx-auto py-10 px-4 max-w-4xl">
            <h1 className="text-4xl font-bold text-slate-900 mb-10 tracking-tighter uppercase">
                Privacy Policy
            </h1>
            <div className="prose prose-slate max-w-none prose-headings:font-bold prose-headings:tracking-tight prose-a:text-blue-600 font-medium text-slate-700 leading-relaxed">
                <p>Your privacy is important to us. It is our policy to respect your privacy regarding any information we may collect from you across our website.</p>
                <h2>1. Information We Collect</h2>
                <p>We only ask for personal information when we truly need it to provide a service to you. We collect it by fair and lawful means, with your knowledge and consent.</p>
                <h2>2. How We Use Information</h2>
                <p>We use the information we collect in various ways, including to provide, operate, and maintain our website, and to improve and personalize your experience.</p>
                <h2>3. Data Retention</h2>
                <p>We only retain collected information for as long as necessary to provide you with your requested service.</p>
            </div>
        </div>
    );
};

export default PrivacyPolicy;
