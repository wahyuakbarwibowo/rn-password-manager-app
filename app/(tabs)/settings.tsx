import { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { useFocusEffect } from 'expo-router';
import * as LocalAuthentication from 'expo-local-authentication';
import {
  List,
  Switch,
  Divider,
  Text,
  Portal,
  Dialog,
  Button,
  TextInput,
  ActivityIndicator,
} from 'react-native-paper';

import { useThemeColor } from '@/hooks/use-theme-color';
import { isBiometricEnabled, setBiometricEnabled } from '@/lib/settings-service';
import { exportBackup, importBackup } from '@/lib/backup-service';

export default function SettingsScreen() {
  const [biometricOn, setBiometricOn] = useState(false);
  const [loading, setLoading] = useState(false);

  const [backupDialogVisible, setBackupDialogVisible] = useState(false);
  const [restoreDialogVisible, setRestoreDialogVisible] = useState(false);
  const [backupPassword, setBackupPassword] = useState('');

  const backgroundColor = useThemeColor({}, 'background');
  const textColor = useThemeColor({}, 'text');
  const secondaryColor = useThemeColor({}, 'secondaryText');

  useFocusEffect(
    useCallback(() => {
      isBiometricEnabled().then(setBiometricOn);
    }, [])
  );

  async function handleBiometricToggle(value: boolean) {
    if (value) {
      const hasHardware = await LocalAuthentication.hasHardwareAsync();
      if (!hasHardware) {
        Alert.alert('Not Available', 'Your device does not support biometric authentication.');
        return;
      }
      const isEnrolled = await LocalAuthentication.isEnrolledAsync();
      if (!isEnrolled) {
        Alert.alert('Not Set Up', 'No biometrics enrolled on this device. Please set up fingerprint or face unlock in your device settings.');
        return;
      }

      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: 'Verify to enable biometric unlock',
      });
      if (!result.success) return;
    }

    setBiometricOn(value);
    await setBiometricEnabled(value);
  }

  function handleBackupPress() {
    setBackupPassword('');
    setBackupDialogVisible(true);
  }

  async function handleBackupConfirm() {
    if (!backupPassword.trim()) {
      Alert.alert('Error', 'Please enter a backup password.');
      return;
    }
    setBackupDialogVisible(false);
    setLoading(true);
    try {
      const uri = await exportBackup(backupPassword.trim());
      Alert.alert('Success', `Backup created successfully.\n\nLocation:\n${uri}`);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Backup failed';
      Alert.alert('Error', msg);
    } finally {
      setLoading(false);
      setBackupPassword('');
    }
  }

  function handleRestorePress() {
    Alert.alert(
      'Restore Passwords',
      'This will add passwords from the backup file. Existing passwords won\'t be deleted.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Continue',
          onPress: () => {
            setBackupPassword('');
            setRestoreDialogVisible(true);
          },
        },
      ]
    );
  }

  async function handleRestoreConfirm() {
    if (!backupPassword.trim()) {
      Alert.alert('Error', 'Please enter the backup password.');
      return;
    }
    setRestoreDialogVisible(false);
    setLoading(true);
    try {
      const count = await importBackup(backupPassword.trim());
      Alert.alert(
        'Success',
        `Restored ${count} new password${count !== 1 ? 's' : ''} from backup.`
      );
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Restore failed';
      Alert.alert('Error', msg);
    } finally {
      setLoading(false);
      setBackupPassword('');
    }
  }

  return (
    <ScrollView style={[styles.container, { backgroundColor }]}>
      {loading && (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size="large" />
        </View>
      )}

      <List.Section>
        <List.Subheader style={{ color: secondaryColor }}>Security</List.Subheader>
        <List.Item
          title="Biometric Unlock"
          titleStyle={{ color: textColor }}
          description="Use fingerprint or face to unlock the app"
          descriptionStyle={{ color: secondaryColor }}
          left={(props) => <List.Icon {...props} icon="fingerprint" color={textColor} />}
          right={() => (
            <Switch value={biometricOn} onValueChange={handleBiometricToggle} color="#E91E8A" />
          )}
        />
      </List.Section>

      <Divider />

      <List.Section>
        <List.Subheader style={{ color: secondaryColor }}>Data</List.Subheader>
        <List.Item
          title="Backup Passwords"
          titleStyle={{ color: textColor }}
          description="Export encrypted backup file"
          descriptionStyle={{ color: secondaryColor }}
          left={(props) => <List.Icon {...props} icon="cloud-upload" color={textColor} />}
          right={(props) => <List.Icon {...props} icon="chevron-right" color={secondaryColor} />}
          onPress={handleBackupPress}
        />
        <List.Item
          title="Restore Passwords"
          titleStyle={{ color: textColor }}
          description="Import from backup file"
          descriptionStyle={{ color: secondaryColor }}
          left={(props) => <List.Icon {...props} icon="cloud-download" color={textColor} />}
          right={(props) => <List.Icon {...props} icon="chevron-right" color={secondaryColor} />}
          onPress={handleRestorePress}
        />
      </List.Section>

      <Divider />

      <List.Section>
        <List.Subheader style={{ color: secondaryColor }}>About</List.Subheader>
        <List.Item
          title="Version"
          titleStyle={{ color: textColor }}
          description="1.0.0"
          descriptionStyle={{ color: secondaryColor }}
          left={(props) => <List.Icon {...props} icon="information" color={textColor} />}
        />
      </List.Section>

      <Portal>
        <Dialog visible={backupDialogVisible} onDismiss={() => setBackupDialogVisible(false)}>
          <Dialog.Title>Backup Password</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium" style={{ marginBottom: 12 }}>
              {"Enter a password to encrypt your backup. You'll need this password to restore."}
            </Text>
            <TextInput
              mode="outlined"
              label="Backup Password"
              secureTextEntry
              value={backupPassword}
              onChangeText={setBackupPassword}
              activeOutlineColor="#E91E8A"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setBackupDialogVisible(false)} textColor="#999">Cancel</Button>
            <Button onPress={handleBackupConfirm} textColor="#E91E8A">Backup</Button>
          </Dialog.Actions>
        </Dialog>

        <Dialog visible={restoreDialogVisible} onDismiss={() => setRestoreDialogVisible(false)}>
          <Dialog.Title>Enter Backup Password</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium" style={{ marginBottom: 12 }}>
              Enter the password you used when creating this backup.
            </Text>
            <TextInput
              mode="outlined"
              label="Backup Password"
              secureTextEntry
              value={backupPassword}
              onChangeText={setBackupPassword}
              activeOutlineColor="#E91E8A"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setRestoreDialogVisible(false)} textColor="#999">Cancel</Button>
            <Button onPress={handleRestoreConfirm} textColor="#E91E8A">Restore</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  loadingOverlay: {
    padding: 20,
    alignItems: 'center',
  },
});
