import { useEffect, useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { PaperProvider, MD3LightTheme, MD3DarkTheme, Text, Button } from 'react-native-paper';
import * as LocalAuthentication from 'expo-local-authentication';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import 'react-native-reanimated';

import { useColorScheme } from '@/hooks/use-color-scheme';
import { isBiometricEnabled } from '@/lib/settings-service';
import { getDatabase } from '@/lib/database';

const pinkColor = '#E91E8A';

const paperLightTheme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: pinkColor,
    secondary: '#F48FB1',
  },
};

const paperDarkTheme = {
  ...MD3DarkTheme,
  colors: {
    ...MD3DarkTheme.colors,
    primary: pinkColor,
    secondary: '#F48FB1',
  },
};

const navLightTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    primary: pinkColor,
  },
};

const navDarkTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: pinkColor,
  },
};

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [needsAuth, setNeedsAuth] = useState(false);
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    checkAuth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function checkAuth() {
    // Ensure DB is initialized before checking settings
    await getDatabase();
    const enabled = await isBiometricEnabled();
    if (enabled) {
      setNeedsAuth(true);
      setAuthChecked(true);
      authenticate();
    } else {
      setIsAuthenticated(true);
      setAuthChecked(true);
    }
  }

  async function authenticate() {
    const result = await LocalAuthentication.authenticateAsync({
      promptMessage: 'Unlock Aminmart Password Manager',
      fallbackLabel: 'Use Passcode',
    });
    if (result.success) {
      setIsAuthenticated(true);
    }
  }

  const isDark = colorScheme === 'dark';

  if (!authChecked) {
    return (
      <View style={[styles.lockScreen, { backgroundColor: isDark ? '#151718' : '#fff' }]}>
        <StatusBar style="auto" />
      </View>
    );
  }

  if (needsAuth && !isAuthenticated) {
    return (
      <PaperProvider theme={isDark ? paperDarkTheme : paperLightTheme}>
        <View style={[styles.lockScreen, { backgroundColor: isDark ? '#151718' : '#fff' }]}>
          <MaterialIcons name="lock" size={64} color={pinkColor} />
          <Text variant="headlineSmall" style={[styles.lockTitle, { color: isDark ? '#ECEDEE' : '#11181C' }]}>
            Aminmart Password Manager
          </Text>
          <Text variant="bodyMedium" style={[styles.lockSubtitle, { color: isDark ? '#9BA1A6' : '#6B7280' }]}>
            Authenticate to unlock
          </Text>
          <Button mode="contained" onPress={authenticate} style={styles.unlockButton} buttonColor={pinkColor}>
            Unlock
          </Button>
          <StatusBar style="auto" />
        </View>
      </PaperProvider>
    );
  }

  return (
    <PaperProvider theme={isDark ? paperDarkTheme : paperLightTheme}>
      <ThemeProvider value={isDark ? navDarkTheme : navLightTheme}>
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
          <Stack.Screen
            name="settings"
            options={{ title: 'Settings' }}
          />
        </Stack>
        <StatusBar style="auto" />
      </ThemeProvider>
    </PaperProvider>
  );
}

const styles = StyleSheet.create({
  lockScreen: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 12,
  },
  lockTitle: {
    marginTop: 16,
    fontWeight: 'bold',
  },
  lockSubtitle: {
    marginBottom: 8,
  },
  unlockButton: {
    marginTop: 16,
    paddingHorizontal: 16,
  },
});
