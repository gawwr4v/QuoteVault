# QuoteVault

QuoteVault is a modern Android application for discovering, saving, and sharing quotes, built with Jetpack Compose and Supabase.

## � Screenshots


## �🛠 Setup Instructions

### Prerequisites
*   Android Studio (Koala or newer recommended)
*   JDK 17
*   Supabase Account

### API Configuration
This project uses `BuildConfig` to secure sensitive API keys. **Do not commit your real keys to version control.**

1.  Open the `local.properties` file in the project root (this file is git-ignored).
2.  Add your Supabase credentials:
    ```properties
    SUPABASE_URL="https://<YOUR_PROJECT_ID>.supabase.co"
    SUPABASE_KEY="<YOUR_ANON_KEY>"
    ```
3.  **Sync Project**: Click "Sync Project with Gradle Files" in Android Studio.

## 🗄️ Backend Schema (Supabase)

This app relies on the following PostgreSQL structure. You can run this SQL script directly in the Supabase SQL Editor to set up your database.

```sql
-- 1. Enable UUID Extension
create extension if not exists "uuid-ossp";

-- 2. Categories Table
create table public.categories (
  id uuid primary key default uuid_generate_v4(),
  name text not null unique
);

-- 3. Quotes Table
create table public.quotes (
  id uuid primary key default uuid_generate_v4(),
  text text not null,
  author text not null,
  category_id uuid references public.categories(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Lists Table (User Collections)
create table public.lists (
  id uuid primary key default uuid_generate_v4(),
  name text not null,
  user_id uuid not null references auth.users(id) on delete cascade
);

-- 5. Favorites Table
create table public.favorites (
  id uuid primary key default uuid_generate_v4(),
  quote_id uuid not null references public.quotes(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  unique (quote_id, user_id)
);

-- 6. List Quotes Junction Table (Many-to-Many)
create table public.list_quotes (
  id uuid primary key default uuid_generate_v4(),
  list_id uuid not null references public.lists(id) on delete cascade,
  quote_id uuid not null references public.quotes(id) on delete cascade,
  unique (list_id, quote_id)
);

-- === Row Level Security (RLS) Policies ===

-- Enable RLS
alter table favorites enable row level security;
alter table lists enable row level security;
alter table list_quotes enable row level security;

-- Favorites: Users can only see/edit their own favorites
create policy "favorites_select" on favorites for select using (auth.uid() = user_id);
create policy "favorites_insert" on favorites for insert with check (auth.uid() = user_id);
create policy "favorites_delete" on favorites for delete using (auth.uid() = user_id);

-- Lists: Users can only see/edit their own lists
create policy "lists_all" on lists for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- List Quotes: Access granted if user owns the parent list
create policy "list_quotes_all" on list_quotes for all
using (
  exists (
    select 1 from lists
    where lists.id = list_quotes.list_id
    and lists.user_id = auth.uid()
  )
);

-- === Initial Data ===
insert into categories (name) values 
('Motivation'), ('Love'), ('Success'), ('Wisdom'), ('Humor'), ('Life');

-- Update any existing quotes with timestamps
UPDATE public.quotes SET created_at = now() WHERE created_at IS NULL;
```

## 🚀 Coding Approach & Workflow

### Architecture
We follow **Modern Android Development (MAD)** practices:
*   **MVVM (Model-View-ViewModel)**: Ensures clear separation between UI and business logic.
*   **Clean Architecture**: Divided into three distinct layers:
    *   **Data Layer**: Repositories, Supabase Data Source, DataStore/SharedPreferences.
    *   **Domain Layer**: Use cases and data models.
    *   **UI Layer**: Jetpack Compose screens and state holders.
*   **Dependency Injection**: Hilt (Dagger) for managing dependencies.

### Technical Highlights
*   **Declarative UI**: 100% Jetpack Compose with Material 3 Design.
*   **Reactive Data**: Built on Kotlin Coroutines and StateFlow.
*   **Widget Integration**: Uses Jetpack Glance for a modern, responsive home screen widget.
*   **Secure Config**: API credentials secured via `local.properties` and `BuildConfig`.



## ⚠️ Known Limitations
*   **Backend Tables**: The `public.profiles` table currently handles user data gracefully via fallback to `Auth User Metadata`.
*   **Widget Sync**: The widget updates via background work (WorkManager) or immediately upon app launch.
