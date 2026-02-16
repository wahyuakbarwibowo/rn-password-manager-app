import { useCallback, useState } from 'react';
import {
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  View,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router, useLocalSearchParams, useFocusEffect, useNavigation } from 'expo-router';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import * as Clipboard from 'expo-clipboard';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { CategoryBadge } from '@/components/category-badge';
import { useThemeColor } from '@/hooks/use-theme-color';
import { getPasswordById, deletePassword } from '@/lib/password-service';
import type { PasswordEntry } from '@/types/password';

export default function PasswordDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [entry, setEntry] = useState<PasswordEntry | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const navigation = useNavigation();

  const tintColor = useThemeColor({}, 'tint');
  const cardColor = useThemeColor({}, 'card');
  const secondaryColor = useThemeColor({}, 'secondaryText');
  const dangerColor = useThemeColor({}, 'danger');
  const iconColor = useThemeColor({}, 'icon');
  const backgroundColor = useThemeColor({}, 'background');

  useFocusEffect(
    useCallback(() => {
      async function load() {
        const data = await getPasswordById(Number(id));
        setEntry(data);
      }
      load();
    }, [id])
  );

  useFocusEffect(
    useCallback(() => {
      navigation.setOptions({
        headerRight: () => (
          <TouchableOpacity
            onPress={() => router.push(`/passwords/edit/${id}`)}
            style={styles.headerButton}
          >
            <MaterialIcons name="edit" size={24} color={tintColor} />
          </TouchableOpacity>
        ),
      });
    }, [navigation, tintColor, id])
  );

  async function copyToClipboard(text: string, label: string) {
    await Clipboard.setStringAsync(text);
    Alert.alert('Copied', `${label} copied to clipboard.`);
  }

  function handleDelete() {
    Alert.alert(
      'Delete Password',
      'Are you sure you want to delete this password? This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await deletePassword(Number(id));
            router.back();
          },
        },
      ]
    );
  }

  if (!entry) {
    return (
      <SafeAreaView style={[styles.safeArea, { backgroundColor }]} edges={['top', 'bottom']}>
        <View style={styles.headerContainer}>
          <ThemedText type="title" style={styles.headerTitle}>Password Detail</ThemedText>
        </View>
        <View style={styles.centeredContent}>
          <ThemedText>Loading...</ThemedText>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView 
      style={[styles.safeArea, { backgroundColor }]}
      edges={['top', 'bottom']}
    >
      <View style={styles.headerContainer}>
        <View style={styles.headerRow}>
          <ThemedText type="title" style={styles.headerTitle}>Password Detail</ThemedText>
          <TouchableOpacity
            onPress={() => router.push(`/passwords/edit/${id}`)}
            style={styles.headerButton}
          >
            <MaterialIcons name="edit" size={24} color={tintColor} />
          </TouchableOpacity>
        </View>
      </View>
      <ScrollView 
        style={styles.container}
        contentInsetAdjustmentBehavior="automatic"
        automaticallyAdjustKeyboardInsets={true}
      >
      <View style={styles.header}>
        <ThemedText type="title" style={styles.title}>{entry.title}</ThemedText>
        <CategoryBadge categoryKey={entry.category} />
      </View>

      {entry.username ? (
        <DetailRow
          label="Username"
          value={entry.username}
          cardColor={cardColor}
          secondaryColor={secondaryColor}
          iconColor={iconColor}
          onCopy={() => copyToClipboard(entry.username, 'Username')}
        />
      ) : null}

      <View style={[styles.card, { backgroundColor: cardColor }]}>
        <View style={styles.cardHeader}>
          <ThemedText style={[styles.label, { color: secondaryColor }]}>Password</ThemedText>
          <View style={styles.cardActions}>
            <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.actionButton}>
              <MaterialIcons
                name={showPassword ? 'visibility-off' : 'visibility'}
                size={20}
                color={iconColor}
              />
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => copyToClipboard(entry.password, 'Password')}
              style={styles.actionButton}
            >
              <MaterialIcons name="content-copy" size={20} color={iconColor} />
            </TouchableOpacity>
          </View>
        </View>
        <ThemedText style={styles.value}>
          {showPassword ? entry.password : '\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022'}
        </ThemedText>
      </View>

      {entry.website ? (
        <DetailRow
          label="Website"
          value={entry.website}
          cardColor={cardColor}
          secondaryColor={secondaryColor}
          iconColor={iconColor}
          onCopy={() => copyToClipboard(entry.website, 'Website')}
        />
      ) : null}

      {entry.notes ? (
        <View style={[styles.card, { backgroundColor: cardColor }]}>
          <ThemedText style={[styles.label, { color: secondaryColor }]}>Notes</ThemedText>
          <ThemedText style={styles.value}>{entry.notes}</ThemedText>
        </View>
      ) : null}

      <View style={[styles.card, { backgroundColor: cardColor }]}>
        <ThemedText style={[styles.label, { color: secondaryColor }]}>Created</ThemedText>
        <ThemedText style={styles.value}>{entry.created_at}</ThemedText>
        <ThemedText style={[styles.label, { color: secondaryColor, marginTop: 12 }]}>
          Last Updated
        </ThemedText>
        <ThemedText style={styles.value}>{entry.updated_at}</ThemedText>
      </View>

      <TouchableOpacity
        style={[styles.deleteButton, { borderColor: dangerColor }]}
        onPress={handleDelete}
        activeOpacity={0.7}
      >
        <MaterialIcons name="delete" size={20} color={dangerColor} />
        <ThemedText style={[styles.deleteText, { color: dangerColor }]}>Delete Password</ThemedText>
      </TouchableOpacity>

      <View style={styles.bottomSpacer} />
    </ScrollView>
    </SafeAreaView>
  );
}

function DetailRow({
  label,
  value,
  cardColor,
  secondaryColor,
  iconColor,
  onCopy,
}: {
  label: string;
  value: string;
  cardColor: string;
  secondaryColor: string;
  iconColor: string;
  onCopy: () => void;
}) {
  return (
    <View style={[styles.card, { backgroundColor: cardColor }]}>
      <View style={styles.cardHeader}>
        <ThemedText style={[styles.label, { color: secondaryColor }]}>{label}</ThemedText>
        <TouchableOpacity onPress={onCopy} style={styles.actionButton}>
          <MaterialIcons name="content-copy" size={20} color={iconColor} />
        </TouchableOpacity>
      </View>
      <ThemedText style={styles.value}>{value}</ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  container: {
    flex: 1,
    padding: 16,
  },
  headerContainer: {
    paddingHorizontal: 16, // Memberikan padding horizontal agar sejajar dengan konten
    paddingTop: 10, // Memberikan sedikit ruang dari atas
    paddingBottom: 10, // Memberikan sedikit ruang dari bawah
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerButton: {
    padding: 8,
  },
  centeredContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  title: {
    flex: 1,
    marginRight: 12,
  },
  headerButton: {
    marginRight: 8,
  },
  card: {
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  cardActions: {
    flexDirection: 'row',
    gap: 8,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 4,
  },
  value: {
    fontSize: 16,
  },
  actionButton: {
    padding: 4,
  },
  deleteButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    borderWidth: 1,
    borderRadius: 12,
    paddingVertical: 14,
    marginTop: 8,
  },
  deleteText: {
    fontSize: 16,
    fontWeight: '600',
  },
  bottomSpacer: {
    height: 32,
  },
});
