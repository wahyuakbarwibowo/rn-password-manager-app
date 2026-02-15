import { StyleSheet, Text, View } from 'react-native';
import { getCategoryByKey } from '@/constants/categories';

interface CategoryBadgeProps {
  categoryKey: string;
}

export function CategoryBadge({ categoryKey }: CategoryBadgeProps) {
  const category = getCategoryByKey(categoryKey);

  return (
    <View style={[styles.badge, { backgroundColor: category.color + '20' }]}>
      <Text style={[styles.text, { color: category.color }]}>{category.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
  },
  text: {
    fontSize: 12,
    fontWeight: '600',
  },
});
