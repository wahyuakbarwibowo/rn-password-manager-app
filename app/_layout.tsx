import { useEffect, useState } from 'react';
import { StyleSheet } from 'react-native';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import {
  PaperProvider,
  MD3LightTheme,
  MD3DarkTheme,
  Text,
  Button,
  TextInput,
  ActivityIndicator,
} from 'react-native-paper';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { BlurView } from 'expo-blur';
import * as LocalAuthentication from 'expo-local-authentication';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import 'react-native-reanimated';

import { useColorScheme } from '@/hooks/use-color-scheme';
import { isBiometricEnabled } from '@/lib/settings-service';
import { getDatabase } from '@/lib/database';
import {
  ensureEncryptionKey,
  isVaultInitialized,
  initializeVault,
  unlockVault,
} from '@/lib/crypto';

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

  const [vaultStatus, setVaultStatus] = useState<'checking' | 'needsSetup' | 'needsUnlock' | 'ready'>(
    'checking'
  );
  const [vaultError, setVaultError] = useState<string | null>(null);
  const [vaultLoading, setVaultLoading] = useState(false);
  const [masterPassword, setMasterPassword] = useState('');
  const [masterPasswordConfirm, setMasterPasswordConfirm] = useState('');

  useEffect(() => {
    initApp();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function initApp() {
    // Pastikan DB dan skema siap
    await getDatabase();
    await ensureEncryptionKey();

    const vaultExists = await isVaultInitialized();
    setVaultStatus(vaultExists ? 'needsUnlock' : 'needsSetup');

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

  const showVaultSetup = !needsAuth || isAuthenticated;

  if (showVaultSetup && vaultStatus !== 'ready') {
    const backgroundColor = isDark ? '#151718' : '#fff';
    const textColor = isDark ? '#ECEDEE' : '#11181C';
    const secondaryColor = isDark ? '#9BA1A6' : '#6B7280';

    const isSetup = vaultStatus === 'needsSetup';

    async function handleSetup() {
      if (!masterPassword || masterPassword.length < 8) {
        setVaultError('Master password must be at least 8 characters.');
        return;
      }
      if (isSetup && masterPassword !== masterPasswordConfirm) {
        setVaultError('Passwords do not match.');
        return;
      }

      setVaultLoading(true);
      setVaultError(null);
      try {
        if (isSetup) {
          await initializeVault(masterPassword);
        } else {
          await unlockVault(masterPassword);
        }
        setVaultStatus('ready');
        setMasterPassword('');
        setMasterPasswordConfirm('');
      } catch (e: unknown) {
        const msg = e instanceof Error ? e.message : 'Unable to unlock vault';
        setVaultError(msg);
      } finally {
        setVaultLoading(false);
      }
    }

    return (
      <SafeAreaProvider>
        <PaperProvider theme={isDark ? paperDarkTheme : paperLightTheme}>
          <SafeAreaView style={[styles.lockScreen, { backgroundColor }]}>
            <MaterialIcons name="vpn-key" size={64} color={pinkColor} />
            <Text
              variant="headlineSmall"
              style={[styles.lockTitle, { color: textColor }]}
            >
              {isSetup ? 'Set Master Password' : 'Enter Master Password'}
            </Text>
            <Text
              variant="bodyMedium"
              style={[styles.lockSubtitle, { color: secondaryColor }]}
            >
              {isSetup
                ? 'Create a master password to protect all your passwords.'
                : 'Enter your master password to unlock your vault.'}
            </Text>

            <TextInput
              mode="outlined"
              label="Master Password"
              secureTextEntry
              value={masterPassword}
              onChangeText={setMasterPassword}
              style={{ width: '80%' }}
              activeOutlineColor={pinkColor}
            />
            {isSetup && (
              <TextInput
                mode="outlined"
                label="Confirm Master Password"
                secureTextEntry
                value={masterPasswordConfirm}
                onChangeText={setMasterPasswordConfirm}
                style={{ width: '80%' }}
                activeOutlineColor={pinkColor}
              />
            )}

            {vaultError && (
              <Text
                variant="bodySmall"
                style={{ color: '#EF4444', marginTop: 4, textAlign: 'center' }}
              >
                {vaultError}
              </Text>
            )}

            <Button
              mode="contained"
              onPress={handleSetup}
              style={styles.unlockButton}
              buttonColor={pinkColor}
              disabled={vaultLoading}
            >
              {isSetup ? 'Save & Continue' : 'Unlock'}
            </Button>

            {vaultLoading && <ActivityIndicator style={{ marginTop: 8 }} />}

            <StatusBar style="auto" />
          </SafeAreaView>
        </PaperProvider>
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
