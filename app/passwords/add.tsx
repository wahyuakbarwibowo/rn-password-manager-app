import { StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
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
    <SafeAreaView style={[styles.safeArea, { backgroundColor }]} edges={['bottom']}>
      <PasswordForm onSubmit={handleSubmit} submitLabel="Save Password" />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
});
