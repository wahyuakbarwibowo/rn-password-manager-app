export interface PasswordEntry {
  id: number;
  title: string;
  username: string;
  password: string;
  website: string;
  notes: string;
  category: string;
  created_at: string;
  updated_at: string;
}

export interface PasswordRow {
  id: number;
  title: string;
  username: string;
  password: string; // encrypted
  website: string;
  notes: string; // encrypted
  category: string;
  created_at: string;
  updated_at: string;
}

export interface CreatePasswordInput {
  title: string;
  username: string;
  password: string;
  website: string;
  notes: string;
  category: string;
}

export interface UpdatePasswordInput extends Partial<CreatePasswordInput> {
  id: number;
}
