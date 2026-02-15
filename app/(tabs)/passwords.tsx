import { useCallback, useRef, useState } from 'react';
import { FlatList, StyleSheet, TouchableOpacity } from 'react-native';
import { router, useFocusEffect, useNavigation } from 'expo-router';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';

import { ThemedView } from '@/components/themed-view';
import { SearchBar } from '@/components/search-bar';
import { PasswordListItem } from '@/components/password-list-item';
import { EmptyState } from '@/components/empty-state';
import { useThemeColor } from '@/hooks/use-theme-color';
import { getAllPasswords, searchPasswords } from '@/lib/password-service';
import type { PasswordEntry } from '@/types/password';

export default function PasswordListScreen() {
  const [passwords, setPasswords] = useState<PasswordEntry[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const tintColor = useThemeColor({}, 'tint');
  const navigation = useNavigation();

  const loadPasswords = useCallback(async () => {
    const data = searchQuery
      ? await searchPasswords(searchQuery)
      : await getAllPasswords();
    setPasswords(data);
  }, [searchQuery]);

  useFocusEffect(
    useCallback(() => {
      loadPasswords();
    }, [loadPasswords])
  );

  useFocusEffect(
    useCallback(() => {
      navigation.setOptions({
        headerRight: () => (
          <TouchableOpacity onPress={() => router.push('/passwords/add')} style={styles.headerButton}>
            <MaterialIcons name="add" size={28} color={tintColor} />
          </TouchableOpacity>
        ),
      });
    }, [navigation, tintColor])
  );

  function handleSearch(text: string) {
    setSearchQuery(text);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      const data = text ? await searchPasswords(text) : await getAllPasswords();
      setPasswords(data);
    }, 300);
  }

  return (
    <ThemedView style={styles.container}>
      <SearchBar value={searchQuery} onChangeText={handleSearch} />
      <FlatList
        data={passwords}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
          <PasswordListItem
            item={item}
            onPress={() => router.push(`/passwords/${item.id}`)}
          />
        )}
        ListEmptyComponent={
          <EmptyState
            icon="lock"
            title="No Passwords Yet"
            message={searchQuery ? 'No results found. Try a different search.' : 'Tap the + button to add your first password.'}
          />
        }
        contentContainerStyle={passwords.length === 0 ? styles.emptyList : styles.list}
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  headerButton: {
    marginRight: 8,
  },
  list: {
    paddingBottom: 16,
  },
  emptyList: {
    flexGrow: 1,
  },
});
