import { useCallback, useState } from 'react';
import { router, useLocalSearchParams, useFocusEffect } from 'expo-router';
import { ThemedView } from '@/components/themed-view';
import { ThemedText } from '@/components/themed-text';
import { PasswordForm } from '@/components/password-form';
import { getPasswordById, updatePassword } from '@/lib/password-service';
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

  if (!entry) {
    return (
      <ThemedView style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ThemedText>Loading...</ThemedText>
      </ThemedView>
    );
  }

  async function handleSubmit(values: CreatePasswordInput) {
    await updatePassword({ id: Number(id), ...values });
    router.back();
  }

  return (
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
  );
}
