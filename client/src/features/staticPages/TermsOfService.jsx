import React from 'react';

const TermsOfService = () => {
    return (
        <div className="container mx-auto py-10 px-4 max-w-4xl">
            <h1 className="text-4xl font-bold text-slate-900 mb-10 tracking-tighter uppercase">
                Terms of Service
            </h1>
            <div className="prose prose-slate max-w-none prose-headings:font-bold prose-headings:tracking-tight prose-a:text-blue-600 font-medium text-slate-700 leading-relaxed">
                <p>By accessing our website, you are agreeing to be bound by these terms of service, all applicable laws and regulations, and agree that you are responsible for compliance with any applicable local laws.</p>
                <h2>1. Use License</h2>
                <p>Permission is granted to temporarily download one copy of the materials on our website for personal, non-commercial transitory viewing only.</p>
                <h2>2. Disclaimer</h2>
                <p>The materials on our website are provided on an 'as is' basis. We make no warranties, expressed or implied, and hereby disclaim and negate all other warranties.</p>
                <h2>3. Limitations</h2>
                <p>In no event shall we or our suppliers be liable for any damages arising out of the use or inability to use the materials on our website.</p>
            </div>
        </div>
    );
};

export default TermsOfService;
