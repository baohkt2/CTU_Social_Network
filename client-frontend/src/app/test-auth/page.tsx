import { notFound } from 'next/navigation';
import TestAuthDevPage from './TestAuthDevPage';

/**
 * Debug route: only available outside production builds.
 */
export default function TestAuthPage() {
  if (process.env.NODE_ENV === 'production') {
    notFound();
  }
  return <TestAuthDevPage />;
}
