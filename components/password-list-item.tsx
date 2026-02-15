import { StyleSheet, TouchableOpacity, View } from 'react-native';
import { ThemedText } from '@/components/themed-text';
import { CategoryBadge } from '@/components/category-badge';
import { useThemeColor } from '@/hooks/use-theme-color';
import type { PasswordEntry } from '@/types/password';

interface PasswordListItemProps {
  item: PasswordEntry;
  onPress: () => void;
}

export function PasswordListItem({ item, onPress }: PasswordListItemProps) {
  const cardColor = useThemeColor({}, 'card');
  const secondaryColor = useThemeColor({}, 'secondaryText');

  return (
    <TouchableOpacity
      style={[styles.container, { backgroundColor: cardColor }]}
      onPress={onPress}
      activeOpacity={0.7}
    >
      <View style={styles.content}>
        <ThemedText type="defaultSemiBold" numberOfLines={1}>{item.title}</ThemedText>
        {item.username ? (
          <ThemedText style={[styles.username, { color: secondaryColor }]} numberOfLines={1}>
            {item.username}
          </ThemedText>
        ) : null}
      </View>
      <CategoryBadge categoryKey={item.category} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    marginHorizontal: 16,
    marginVertical: 4,
    borderRadius: 12,
  },
  content: {
    flex: 1,
    marginRight: 12,
  },
  username: {
    fontSize: 14,
    marginTop: 2,
  },
});
