import { useCallback, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { router, useLocalSearchParams, useFocusEffect } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ThemedText } from '@/components/themed-text';
import { PasswordForm } from '@/components/password-form';
import { getPasswordById, updatePassword } from '@/lib/password-service';
import { useThemeColor } from '@/hooks/use-theme-color';
import type { PasswordEntry, CreatePasswordInput } from '@/types/password';

export default function EditPasswordScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [entry, setEntry] = useState<PasswordEntry | null>(null);

  useFocusEffect(
    useCallback(() => {
      async function load() {
        const data = await getPasswordById(Number(id));
        setEntry(data);
      }
      load();
    }, [id])
  );

  const backgroundColor = useThemeColor({}, 'background');

  if (!entry) {
    return (
      <SafeAreaView style={[styles.safeArea, { backgroundColor }]} edges={['top', 'bottom']}>
        <View style={styles.loadingContainer}>
          <ThemedText>Loading...</ThemedText>
        </View>
      </SafeAreaView>
    );
  }

  async function handleSubmit(values: CreatePasswordInput) {
    await updatePassword({ id: Number(id), ...values });
    router.back();
  }

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor }]} edges={['top', 'bottom']}>
      <View style={styles.headerContainer}>
        <ThemedText type="title" style={styles.headerTitle}>Edit Password</ThemedText>
      </View>
      <PasswordForm
        initialValues={{
          title: entry.title,
          username: entry.username,
          password: entry.password,
          website: entry.website,
          notes: entry.notes,
          category: entry.category,
        }}
        onSubmit={handleSubmit}
        submitLabel="Update Password"
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  headerContainer: {
    paddingHorizontal: 16, // Memberikan padding horizontal agar sejajar dengan form
    paddingTop: 10, // Memberikan sedikit ruang dari atas
    paddingBottom: 10, // Memberikan sedikit ruang dari bawah
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
