# QuoteVault

QuoteVault is a modern Android application for discovering, saving, and sharing quotes, built with Jetpack Compose and Supabase.

## ✨ Features

### Core Features
- **Authentication**: Email/password sign up, login, logout, password reset
- **Quote Browsing**: Infinite scroll feed, category filtering (Motivation, Love, Success, Wisdom, Humor, Life)
- **Search**: Full-text search by quote content or author
- **Favorites**: Save quotes with cloud sync across devices
- **Collections**: Create custom collections to organize quotes
- **Daily Quote Widget**: Home screen widget with daily inspiration
- **Push Notifications**: Configurable daily quote notifications

### Sharing & Export
- Share quotes as text via system share sheet
- Generate beautiful quote cards with 3 template styles (Minimal, Bold, Artistic)
- Save quote cards as images to device

### Personalization
- Dark/Light mode with system default option
- Multiple accent color themes
- Adjustable quote text size
- All settings sync to user profile

## 📱 Screenshots
<!-- Add your screenshots here -->

### 🎨 Theme Screen Showcase
<p align="center">
  <img src="https://github.com/user-attachments/assets/1e874244-7d3f-4dd7-8b18-f4b898a24490" width="300" style="margin-right: 24px;" />
  <img src="https://github.com/user-attachments/assets/dd885d2f-6410-4ede-ad54-6e2ae51231dd" width="300" />

</p>
<p align="center"><b>Dark Theme</b> &nbsp;&nbsp;&nbsp;&nbsp; <b>Light Theme</b></p>

<br />

---

### 📌 Radial Menu – Showcase
<p align="center">
  <img src="https://github.com/user-attachments/assets/4d7846a8-0332-4138-ad4b-28b196f14f11" width="300" style="margin-right: 24px;" />
  <img src="https://github.com/user-attachments/assets/2f4bec52-21eb-4cc9-9012-f88a20596c6b" width="300" />

</p>
<p align="center"><b>Black Theme</b> &nbsp;&nbsp;&nbsp;&nbsp; <b>Light Theme</b></p>

<br />

---

### ❤️ Radial Menu – Like a Quote
<p align="center">
  <img src="https://github.com/user-attachments/assets/322194f2-89bc-459d-8bc5-5617673ba2a3" width="300" style="margin-right: 24px;" />

  <img src="https://github.com/user-attachments/assets/1c64e7c8-0316-4d9a-8759-102fb3e3587f" width="300" />

</p>
<p align="center"><b>Black Theme</b> &nbsp;&nbsp;&nbsp;&nbsp; <b>Light Theme</b></p>

<br />

---

### 🔄 Radial Menu – Share a Quote
<p align="center">
  <img src="https://github.com/user-attachments/assets/7b08e456-f7e0-4c9c-824d-bbd3f732c9ef" width="300" style="margin-right: 240px;" />
  <img src="https://github.com/user-attachments/assets/2ec602be-0c6d-4ce6-81b9-554c4a081878" width="300" />
</p>
<p align="center"><b>Black Theme</b> &nbsp;&nbsp;&nbsp;&nbsp; <b>Light Theme</b></p>

<br />

---

### 💾 Radial Menu – Save a Quote to Collections
<p align="center">
  <img src="https://github.com/user-attachments/assets/fe115107-78fb-4918-94d4-0ecd15f5340c" width="300" style="margin-right: 24px;" /> 
  <img src="https://github.com/user-attachments/assets/42357980-3439-46cf-82f4-a3b17527204b" width="300" />
</p>
<p align="center"><b>Black Theme</b> &nbsp;&nbsp;&nbsp;&nbsp; <b>Light Theme</b></p>



<!-- <img src="screenshots/home.png" width="250" /> -->

## 🛠 Setup Instructions

### Prerequisites
- **Android Studio**: Koala (2024.1.1) or newer
- **JDK**: 17 or higher
- **Minimum SDK**: Android 10 (API 29)
- **Supabase Account**: Free tier works

### Clone & Open
```bash
git clone https://github.com/gawwr4v/QuoteVault.git
cd QuoteVault
# Open in Android Studio
```

### API Configuration
This project uses `BuildConfig` to secure sensitive API keys. **Do not commit your real keys to version control.**

1. Open `local.properties` in the project root (git-ignored by default)
2. Add your Supabase credentials:
   ```properties
   SUPABASE_URL="https://<YOUR_PROJECT_ID>.supabase.co"
   SUPABASE_KEY="<YOUR_ANON_KEY>"
   ```
3. Click **"Sync Project with Gradle Files"** in Android Studio

### Build & Run
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run all tests
./gradlew test
```

Or simply press ▶️ **Run** in Android Studio.

## 🗄️ Backend Schema (Supabase)

Run this SQL script in the Supabase SQL Editor to set up your database:

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

alter table favorites enable row level security;
alter table lists enable row level security;
alter table list_quotes enable row level security;

create policy "favorites_select" on favorites for select using (auth.uid() = user_id);
create policy "favorites_insert" on favorites for insert with check (auth.uid() = user_id);
create policy "favorites_delete" on favorites for delete using (auth.uid() = user_id);

create policy "lists_all" on lists for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

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
```

### Seeding Quotes
Seed your database with 100+ quotes across categories. You can use any public quote API or dataset.

## 🏗 Architecture

### Modern Android Development (MAD)
- **MVVM**: Clear separation between UI and business logic
- **Clean Architecture**: Three distinct layers:
    - **Data Layer**: Repositories, Supabase Data Source, DataStore
    - **Domain Layer**: Use cases and data models
    - **UI Layer**: Jetpack Compose screens and ViewModels
- **Dependency Injection**: Hilt (Dagger)

### Project Structure
```
app/src/main/java/com/quotevault/
├── data/               # Repositories, data sources
├── di/                 # Hilt modules
├── domain/             # Models, repository interfaces
├── ui/
│   ├── components/     # Reusable Compose components
│   ├── navigation/     # NavGraph, Screen routes
│   ├── screens/        # Feature screens (auth, home, search, etc.)
│   └── theme/          # Colors, Typography, Theme
├── widget/             # Home screen Glance widget
├── worker/             # WorkManager for notifications
└── MainActivity.kt
```

### Technical Stack
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose (Material 3)
- **Dependency Injection**: Hilt
- **Backend & Auth**: Supabase (GoTrue, Postgrest)
- **Network**: Ktor Client
- **Image Loading**: Coil
- **Local Storage**: DataStore Preferences
- **Background Tasks**: WorkManager
- **Widgets**: Jetpack Glance
- **Architecture**: Clean Architecture + MVVM

## 📋 Permissions
- `INTERNET` - API calls to Supabase
- `POST_NOTIFICATIONS` - Daily quote notifications (Android 13+)
- `VIBRATE` - Haptic feedback for radial menu
- `SCHEDULE_EXACT_ALARM` - Precise notification scheduling

## ⚠️ Known Limitations
- **Offline Mode**: App requires internet for initial data load
- **Widget Sync**: Updates via WorkManager or immediately on app launch

## 📄 License
This project is licensed under the [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) License.

