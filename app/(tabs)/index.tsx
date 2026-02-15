import { useCallback, useState } from 'react';
import { ScrollView, StyleSheet, TouchableOpacity, View } from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { Text } from 'react-native-paper';

import { useThemeColor } from '@/hooks/use-theme-color';
import { getAllPasswords } from '@/lib/password-service';
import { isBiometricEnabled } from '@/lib/settings-service';
import { CATEGORIES, getCategoryByKey } from '@/constants/categories';
import type { PasswordEntry } from '@/types/password';

export default function DashboardScreen() {
  const [passwords, setPasswords] = useState<PasswordEntry[]>([]);
  const [biometricOn, setBiometricOn] = useState(true);

  const backgroundColor = useThemeColor({}, 'background');
  const cardColor = useThemeColor({}, 'card');
  const textColor = useThemeColor({}, 'text');
  const secondaryColor = useThemeColor({}, 'secondaryText');
  const tintColor = useThemeColor({}, 'tint');

  useFocusEffect(
    useCallback(() => {
      getAllPasswords().then(setPasswords);
      isBiometricEnabled().then(setBiometricOn);
    }, [])
  );

  const categoryCounts = CATEGORIES.map((cat) => ({
    ...cat,
    count: passwords.filter((p) => p.category === cat.key).length,
  })).filter((cat) => cat.count > 0);

  const recentPasswords = passwords.slice(0, 3);

  return (
    <ScrollView style={[styles.container, { backgroundColor }]} contentContainerStyle={styles.content}>
      {/* Security Warning Banner */}
      {!biometricOn && (
        <TouchableOpacity
          style={styles.warningBanner}
          onPress={() => router.push('/settings')}
          activeOpacity={0.8}
        >
          <MaterialIcons name="shield" size={24} color="#92400E" />
          <View style={styles.warningTextContainer}>
            <Text style={styles.warningTitle}>Your app is not protected</Text>
            <Text style={styles.warningMessage}>
              Enable biometric unlock in Settings.
            </Text>
          </View>
          <MaterialIcons name="chevron-right" size={24} color="#92400E" />
        </TouchableOpacity>
      )}

      {/* Total Passwords Card */}
      <View style={[styles.card, { backgroundColor: cardColor }]}>
        <View style={styles.cardHeader}>
          <MaterialIcons name="lock" size={24} color={tintColor} />
          <Text variant="titleMedium" style={[styles.cardTitle, { color: textColor }]}>
            Total Passwords
          </Text>
        </View>
        <Text variant="displaySmall" style={[styles.totalCount, { color: tintColor }]}>
          {passwords.length}
        </Text>
      </View>

      {/* Category Summaries */}
      {categoryCounts.length > 0 && (
        <View style={[styles.card, { backgroundColor: cardColor }]}>
          <View style={styles.cardHeader}>
            <MaterialIcons name="insights" size={24} color={tintColor} />
            <Text variant="titleMedium" style={[styles.cardTitle, { color: textColor }]}>
              Category Summaries
            </Text>
          </View>
          <View style={styles.summaryGrid}>
            {categoryCounts.map((cat) => (
              <View
                key={`${cat.key}-summary`}
                style={[
                  styles.summaryTile,
                  { borderColor: cat.color, backgroundColor: cardColor },
                ]}
              >
                <Text style={[styles.summaryLabel, { color: textColor }]}>{cat.label}</Text>
                <Text style={[styles.summaryValue, { color: secondaryColor }]}>
                  {cat.count} password{cat.count !== 1 ? 's' : ''}
                </Text>
              </View>
            ))}
          </View>
        </View>
      )}

      {/* Category Breakdown */}
      {categoryCounts.length > 0 && (
        <View style={[styles.card, { backgroundColor: cardColor }]}>
          <View style={styles.cardHeader}>
            <MaterialIcons name="category" size={24} color={tintColor} />
            <Text variant="titleMedium" style={[styles.cardTitle, { color: textColor }]}>
              Categories
            </Text>
          </View>
          {categoryCounts.map((cat) => (
            <View key={cat.key} style={styles.categoryRow}>
              <View style={[styles.categoryDot, { backgroundColor: cat.color }]} />
              <Text style={[styles.categoryLabel, { color: textColor }]}>{cat.label}</Text>
              <Text style={[styles.categoryCount, { color: secondaryColor }]}>{cat.count}</Text>
            </View>
          ))}
        </View>
      )}

      {/* Recently Added */}
      {recentPasswords.length > 0 && (
        <View style={[styles.card, { backgroundColor: cardColor }]}>
          <View style={styles.cardHeader}>
            <MaterialIcons name="schedule" size={24} color={tintColor} />
            <Text variant="titleMedium" style={[styles.cardTitle, { color: textColor }]}>
              Recently Added
            </Text>
          </View>
          {recentPasswords.map((pw) => {
            const cat = getCategoryByKey(pw.category);
            return (
              <TouchableOpacity
                key={pw.id}
                style={styles.recentItem}
                onPress={() => router.push(`/passwords/${pw.id}`)}
              >
                <View style={[styles.categoryDot, { backgroundColor: cat.color }]} />
                <View style={styles.recentInfo}>
                  <Text style={[styles.recentTitle, { color: textColor }]}>{pw.title}</Text>
                  <Text style={[styles.recentSub, { color: secondaryColor }]}>{pw.username}</Text>
                </View>
                <MaterialIcons name="chevron-right" size={20} color={secondaryColor} />
              </TouchableOpacity>
            );
          })}
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: 16,
    gap: 16,
  },
  warningBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FEF3C7',
    borderRadius: 12,
    padding: 14,
    gap: 12,
    borderWidth: 1,
    borderColor: '#FCD34D',
  },
  warningTextContainer: {
    flex: 1,
  },
  warningTitle: {
    fontWeight: '600',
    fontSize: 14,
    color: '#92400E',
  },
  warningMessage: {
    fontSize: 12,
    color: '#A16207',
    marginTop: 2,
  },
  card: {
    borderRadius: 12,
    padding: 16,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 12,
  },
  cardTitle: {
    fontWeight: '600',
  },
  totalCount: {
    fontWeight: 'bold',
    textAlign: 'center',
    marginVertical: 8,
  },
  categoryRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 6,
  },
  categoryDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: 10,
  },
  categoryLabel: {
    flex: 1,
    fontSize: 14,
  },
  categoryCount: {
    fontSize: 14,
    fontWeight: '600',
  },
  summaryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginTop: 6,
  },
  summaryTile: {
    width: '48%',
    borderWidth: 1,
    borderRadius: 12,
    padding: 10,
    marginBottom: 12,
  },
  summaryLabel: {
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 4,
  },
  summaryValue: {
    fontSize: 14,
  },
  recentItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
  },
  recentInfo: {
    flex: 1,
    marginLeft: 2,
  },
  recentTitle: {
    fontSize: 14,
    fontWeight: '500',
  },
  recentSub: {
    fontSize: 12,
    marginTop: 1,
  },
});
