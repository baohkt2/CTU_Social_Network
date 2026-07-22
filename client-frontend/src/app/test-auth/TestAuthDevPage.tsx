'use client';

import React from 'react';
import Card from '@/components/ui/Card';

const TestAuthDevPage: React.FC = () => {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">Authentication Test Page</h1>

      <div className="max-w-4xl mx-auto space-y-6">
        <Card className="p-6">
          <h2 className="text-xl font-semibold mb-4">Debug Information</h2>
          <p className="text-gray-600 mb-4">
            This page helps debug authentication issues. (Note: Test components are temporarily removed).
          </p>
        </Card>
      </div>
    </div>
  );
};

export default TestAuthDevPage;
