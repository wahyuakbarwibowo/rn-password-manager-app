// Contoh penggunaan fungsi backup dan restore di layar UI

import { Alert } from 'react-native';
import { exportBackup, importBackup, importBackupOverwrite } from '@/lib/backup-service';

// Fungsi untuk membuat backup
const handleBackup = async () => {
  try {
    // Dapatkan master password dari pengguna (misalnya lewat modal input)
    const masterPassword = await getMasterPasswordFromUser(); // Implementasi UI untuk mendapatkan password
    
    // Buat backup
    const backupUri = await exportBackup(masterPassword);
    
    // Beri tahu pengguna bahwa backup berhasil
    Alert.alert('Success', 'Backup created successfully!');
    
    // Opsional: bagikan file backup
    // await shareBackupFile(backupUri);
  } catch (error: any) {
    Alert.alert('Backup Failed', error.message);
  }
};

// Fungsi untuk restore dengan mode merge
const handleRestoreMerge = async () => {
  try {
    // Dapatkan master password dari pengguna
    const masterPassword = await getMasterPasswordFromUser();
    
    // Restore dengan mode merge
    const count = await importBackup(masterPassword);
    
    Alert.alert('Success', `Restored ${count} new password${count !== 1 ? 's' : ''} from backup.`);
  } catch (error: any) {
    Alert.alert('Restore Failed', error.message);
  }
};

// Fungsi untuk restore dengan mode overwrite
const handleRestoreOverwrite = async () => {
  try {
    // Konfirmasi kepada pengguna bahwa ini akan menghapus data yang ada
    const confirmed = await confirmOverwriteAction();
    if (!confirmed) return;
    
    // Dapatkan master password dari pengguna
    const masterPassword = await getMasterPasswordFromUser();
    
    // Restore dengan mode overwrite
    const count = await importBackupOverwrite(masterPassword);
    
    Alert.alert('Success', `Overwritten with ${count} password${count !== 1 ? 's' : ''} from backup.`);
  } catch (error: any) {
    Alert.alert('Restore Failed', error.message);
  }
};

// Fungsi bantu untuk mendapatkan password dari pengguna
const getMasterPasswordFromUser = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    // Implementasi UI untuk meminta password dari pengguna
    // Ini bisa berupa modal input atau navigasi ke layar khusus
    // Contoh pseudocode:
    /*
    showModal({
      title: 'Enter Master Password',
      message: 'Please enter your master password to proceed with backup/restore',
      inputs: [{
        label: 'Master Password',
        secureTextEntry: true,
        onChangeText: (text) => { /* store text * /
      }],
      buttons: [
        { text: 'Cancel', onPress: () => reject(new Error('User cancelled')) },
        { text: 'OK', onPress: () => resolve(storedPassword) }
      ]
    });
    */
    
    // Placeholder - gantilah dengan implementasi UI yang sesungguhnya
    reject(new Error('getMasterPasswordFromUser needs UI implementation'));
  });
};

// Fungsi bantu untuk konfirmasi overwrite
const confirmOverwriteAction = (): Promise<boolean> => {
  return new Promise((resolve) => {
    Alert.alert(
      'Confirm Overwrite',
      'This will delete all current passwords and replace them with the backup data. Continue?',
      [
        { text: 'Cancel', onPress: () => resolve(false), style: 'cancel' },
        { text: 'OK', onPress: () => resolve(true) }
      ]
    );
  });
};

// Catatan keamanan:
// 1. Master password tidak pernah disimpan dalam bentuk apapun
// 2. Semua operasi enkripsi/dekripsi dilakukan secara lokal
// 3. File backup terenkripsi dan hanya bisa dibuka dengan master password yang benar
// 4. HMAC digunakan untuk memverifikasi integritas file backup