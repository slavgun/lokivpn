import React from 'react';

const PageHeader = ({ title }) => {
    return (
        <div className="page-title mb-3">
            <h2>{title}</h2>
        </div>
    );
};

export default PageHeader;
