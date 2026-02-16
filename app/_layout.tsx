import { useEffect, useState } from 'react';
import { StyleSheet } from 'react-native';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { PaperProvider, MD3LightTheme, MD3DarkTheme, Text, Button } from 'react-native-paper';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { BlurView } from 'expo-blur';
import * as LocalAuthentication from 'expo-local-authentication';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import 'react-native-reanimated';

import { useColorScheme } from '@/hooks/use-color-scheme';
import { isBiometricEnabled } from '@/lib/settings-service';
import { getDatabase } from '@/lib/database';
import { ensureEncryptionKey } from '@/lib/crypto';

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
  const [authInProgress, setAuthInProgress] = useState(false);

  useEffect(() => {
    checkAuth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function checkAuth() {
    // Ensure encryption key exists before anything else
    await ensureEncryptionKey();
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
    setAuthInProgress(true);
    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: 'Unlock Aminmart Password Manager',
        fallbackLabel: 'Use Passcode',
      });
      if (result.success) {
        setIsAuthenticated(true);
      }
    } finally {
      setAuthInProgress(false);
    }
  }

  const isDark = colorScheme === 'dark';

  if (!authChecked) {
    return (
      <SafeAreaProvider>
        <SafeAreaView style={[styles.lockScreen, { backgroundColor: isDark ? '#151718' : '#fff' }]}>
          <StatusBar style="auto" />
        </SafeAreaView>
      </SafeAreaProvider>
    );
  }

  if (needsAuth && !isAuthenticated) {
    return (
      <SafeAreaProvider>
        <PaperProvider theme={isDark ? paperDarkTheme : paperLightTheme}>
          <SafeAreaView style={[styles.lockScreen, { backgroundColor: isDark ? '#151718' : '#fff' }]}>
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
            {authInProgress && (
              <BlurView
                intensity={90}
                tint={isDark ? 'dark' : 'light'}
                style={styles.blurOverlay}
              />
            )}
          </SafeAreaView>
        </PaperProvider>
      </SafeAreaProvider>
    );
  }

  return (
    <SafeAreaProvider>
      <PaperProvider theme={isDark ? paperDarkTheme : paperLightTheme}>
        <ThemeProvider value={isDark ? navDarkTheme : navLightTheme}>
          <Stack>
            <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
            <Stack.Screen
              name="passwords/add"
              options={{
                headerShown: false, // Sembunyikan header dan tampilkan dalam konten
              }}
            />
            <Stack.Screen
              name="passwords/[id]"
              options={{ 
                headerShown: false, // Sembunyikan header dan tampilkan dalam konten seperti halaman add/edit
              }}
            />
            <Stack.Screen
              name="passwords/edit/[id]"
              options={{
                headerShown: false, // Sembunyikan header dan tampilkan dalam konten
              }}
            />
          </Stack>
          <StatusBar style="auto" />
        </ThemeProvider>
      </PaperProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  lockScreen: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 12,
    position: 'relative',
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
  blurOverlay: {
    ...StyleSheet.absoluteFillObject,
  },
});
