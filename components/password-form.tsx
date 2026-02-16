import { useState } from 'react';
import {
  StyleSheet,
  TextInput,
  View,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { ThemedText } from '@/components/themed-text';
import { CategoryBadge } from '@/components/category-badge';
import { useThemeColor } from '@/hooks/use-theme-color';
import { generatePassword } from '@/lib/crypto';
import { CATEGORIES } from '@/constants/categories';
import type { CreatePasswordInput } from '@/types/password';

interface PasswordFormProps {
  initialValues?: Partial<CreatePasswordInput>;
  onSubmit: (values: CreatePasswordInput) => void;
  submitLabel: string;
}

export function PasswordForm({ initialValues, onSubmit, submitLabel }: PasswordFormProps) {
  const [title, setTitle] = useState(initialValues?.title ?? '');
  const [username, setUsername] = useState(initialValues?.username ?? '');
  const [password, setPassword] = useState(initialValues?.password ?? '');
  const [website, setWebsite] = useState(initialValues?.website ?? '');
  const [notes, setNotes] = useState(initialValues?.notes ?? '');
  const [category, setCategory] = useState(initialValues?.category ?? 'other');
  const [showPassword, setShowPassword] = useState(false);

  const backgroundColor = useThemeColor({}, 'background');
  const inputBg = useThemeColor({}, 'inputBackground');
  const inputBorder = useThemeColor({}, 'inputBorder');
  const textColor = useThemeColor({}, 'text');
  const tintColor = useThemeColor({}, 'tint');
  const iconColor = useThemeColor({}, 'icon');
  const secondaryColor = useThemeColor({}, 'secondaryText');

  const inputStyle = [
    styles.input,
    { backgroundColor: inputBg, borderColor: inputBorder, color: textColor },
  ];

  function handleSubmit() {
    if (!title.trim()) {
      Alert.alert('Validation', 'Title is required.');
      return;
    }
    onSubmit({
      title: title.trim(),
      username: username.trim(),
      password,
      website: website.trim(),
      notes: notes.trim(),
      category,
    });
  }

  function handleGeneratePassword() {
    setPassword(generatePassword(20));
    setShowPassword(true);
  }

  return (
    <ScrollView style={[styles.container, { backgroundColor }]} keyboardShouldPersistTaps="handled">
      <View style={styles.field}>
        <ThemedText style={styles.label}>Title *</ThemedText>
        <TextInput
          style={inputStyle}
          value={title}
          onChangeText={setTitle}
          placeholder="e.g. Gmail, Twitter"
          placeholderTextColor={secondaryColor}
        />
      </View>

      <View style={styles.field}>
        <ThemedText style={styles.label}>Username</ThemedText>
        <TextInput
          style={inputStyle}
          value={username}
          onChangeText={setUsername}
          placeholder="Username or email"
          placeholderTextColor={secondaryColor}
          autoCapitalize="none"
          autoCorrect={false}
        />
      </View>

      <View style={styles.field}>
        <ThemedText style={styles.label}>Password</ThemedText>
        <View style={styles.passwordRow}>
          <TextInput
            style={[...inputStyle, styles.passwordInput]}
            value={password}
            onChangeText={setPassword}
            placeholder="Password"
            placeholderTextColor={secondaryColor}
            secureTextEntry={!showPassword}
            autoCapitalize="none"
            autoCorrect={false}
          />
          <TouchableOpacity
            onPress={() => setShowPassword(!showPassword)}
            style={styles.iconButton}
          >
            <MaterialIcons
              name={showPassword ? 'visibility-off' : 'visibility'}
              size={22}
              color={iconColor}
            />
          </TouchableOpacity>
          <TouchableOpacity onPress={handleGeneratePassword} style={styles.iconButton}>
            <MaterialIcons name="autorenew" size={22} color={tintColor} />
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.field}>
        <ThemedText style={styles.label}>Website</ThemedText>
        <TextInput
          style={inputStyle}
          value={website}
          onChangeText={setWebsite}
          placeholder="https://example.com"
          placeholderTextColor={secondaryColor}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
        />
      </View>

      <View style={styles.field}>
        <ThemedText style={styles.label}>Notes</ThemedText>
        <TextInput
          style={[...inputStyle, styles.notesInput]}
          value={notes}
          onChangeText={setNotes}
          placeholder="Additional notes..."
          placeholderTextColor={secondaryColor}
          multiline
          textAlignVertical="top"
        />
      </View>

      <View style={styles.field}>
        <ThemedText style={styles.label}>Category</ThemedText>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryScroll}>
          {CATEGORIES.map((cat) => (
            <TouchableOpacity
              key={cat.key}
              onPress={() => setCategory(cat.key)}
              style={[
                styles.categoryChip,
                {
                  borderColor: cat.key === category ? cat.color : inputBorder,
                  borderWidth: cat.key === category ? 2 : 1,
                },
              ]}
            >
              <CategoryBadge categoryKey={cat.key} />
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      <TouchableOpacity
        style={[styles.submitButton, { backgroundColor: tintColor }]}
        onPress={handleSubmit}
        activeOpacity={0.8}
      >
        <ThemedText style={styles.submitText}>{submitLabel}</ThemedText>
      </TouchableOpacity>

      <View style={styles.bottomSpacer} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  field: {
    marginBottom: 16,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 6,
  },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
  },
  field: {
    marginBottom: 16,
  },
  passwordRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  passwordInput: {
    flex: 1,
  },
  iconButton: {
    padding: 8,
  },
  notesInput: {
    minHeight: 80,
    paddingTop: 12,
  },
  categoryScroll: {
    flexDirection: 'row',
  },
  categoryChip: {
    marginRight: 8,
    borderRadius: 16,
    padding: 4,
  },
  submitButton: {
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  submitText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  bottomSpacer: {
    height: 32,
  },
});
