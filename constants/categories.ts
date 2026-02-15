export interface Category {
  key: string;
  label: string;
  color: string;
}

export const CATEGORIES: Category[] = [
  { key: 'social', label: 'Social', color: '#3B82F6' },
  { key: 'email', label: 'Email', color: '#EF4444' },
  { key: 'finance', label: 'Finance', color: '#10B981' },
  { key: 'work', label: 'Work', color: '#8B5CF6' },
  { key: 'shopping', label: 'Shopping', color: '#F59E0B' },
  { key: 'entertainment', label: 'Entertainment', color: '#EC4899' },
  { key: 'other', label: 'Other', color: '#6B7280' },
];

export function getCategoryByKey(key: string): Category {
  return CATEGORIES.find((c) => c.key === key) ?? CATEGORIES[CATEGORIES.length - 1];
}
