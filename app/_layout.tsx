import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import 'react-native-reanimated';

import { useColorScheme } from '@/hooks/use-color-scheme';

export default function RootLayout() {
  const colorScheme = useColorScheme();

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Stack>
        <Stack.Screen name="index" options={{ title: 'Aminmart Password Manager' }} />
        <Stack.Screen
          name="passwords/add"
          options={{ presentation: 'modal', title: 'Add Password' }}
        />
        <Stack.Screen
          name="passwords/[id]"
          options={{ title: 'Password Detail' }}
        />
        <Stack.Screen
          name="passwords/edit/[id]"
          options={{ presentation: 'modal', title: 'Edit Password' }}
        />
      </Stack>
      <StatusBar style="auto" />
    </ThemeProvider>
  );
}
