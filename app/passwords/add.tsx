import { StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { ThemedText } from '@/components/themed-text';
import { PasswordForm } from '@/components/password-form';
import { useThemeColor } from '@/hooks/use-theme-color';
import { createPassword } from '@/lib/password-service';
import type { CreatePasswordInput } from '@/types/password';

export default function AddPasswordScreen() {
  async function handleSubmit(values: CreatePasswordInput) {
    await createPassword(values);
    router.back();
  }

  const backgroundColor = useThemeColor({}, 'background');

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor }]} edges={['top', 'bottom']}>
      <View style={styles.headerContainer}>
        <ThemedText type="title" style={styles.headerTitle}>Add Password</ThemedText>
      </View>
      <PasswordForm onSubmit={handleSubmit} submitLabel="Save Password" />
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
});
