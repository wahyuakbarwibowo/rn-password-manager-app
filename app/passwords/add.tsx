import { StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { PasswordForm } from '@/components/password-form';
import { createPassword } from '@/lib/password-service';
import type { CreatePasswordInput } from '@/types/password';

export default function AddPasswordScreen() {
  async function handleSubmit(values: CreatePasswordInput) {
    await createPassword(values);
    router.back();
  }

  return (
    <SafeAreaView style={styles.safeArea} edges={['top']}>
      <PasswordForm onSubmit={handleSubmit} submitLabel="Save Password" />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
});
