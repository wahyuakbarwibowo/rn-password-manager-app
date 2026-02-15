import { router } from 'expo-router';
import { PasswordForm } from '@/components/password-form';
import { createPassword } from '@/lib/password-service';
import type { CreatePasswordInput } from '@/types/password';

export default function AddPasswordScreen() {
  async function handleSubmit(values: CreatePasswordInput) {
    await createPassword(values);
    router.back();
  }

  return <PasswordForm onSubmit={handleSubmit} submitLabel="Save Password" />;
}
