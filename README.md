# WIA2007 Infrastructure Reporter

An Android application for reporting and managing infrastructure issues, connecting citizens, council members, and engineers in a streamlined workflow.

## 🎯 Overview

The Infrastructure Reporter app enables citizens to report infrastructure problems (roads, utilities, public facilities) and allows council members to manage and assign these reports to engineers for resolution.

### User Roles

- **👥 Citizens** - Report infrastructure issues, track ticket status, view resolution updates
- **🏛️ Council** - View all reports, assign tickets to engineers, manage spam, track statistics
- **🔧 Engineers** - View assigned tickets, accept/reject/complete work, provide status updates

## 🚀 Features

### For Citizens
- Submit infrastructure reports with photos, location, and description
- Track ticket status in real-time
- View engineer responses and resolution notes
- Soft-delete unwanted tickets from personal view
- Receive notifications on ticket updates

### For Council Members
- Dashboard with comprehensive statistics
- View all submitted tickets by status (Pending, Under Review, Completed, Spam)
- Assign tickets to available engineers
- Mark tickets as spam
- Track average response times
- Soft-delete tickets from council view

### For Engineers
- Personal dashboard showing assigned tickets
- Accept or reject ticket assignments
- Mark tickets as completed with resolution notes
- Flag tickets as spam if needed
- Track performance metrics and response times
- Soft-delete resolved tickets from view

## 🏗️ Technical Stack

- **Platform**: Android (Java)
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Backend**: Supabase (PostgreSQL with Row-Level Security)
- **Authentication**: Supabase Auth
- **Build System**: Gradle with Kotlin DSL
- **JDK**: Java SE 25 LTS

## 📁 Project Structure

```
WIA2007-Infrastructur-Reporter/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/infrastructureproject/
│   │   │   │   ├── activities/          # Activity classes
│   │   │   │   ├── fragments/           # Fragment classes
│   │   │   │   ├── adapters/            # RecyclerView adapters
│   │   │   │   ├── models/              # Data models
│   │   │   │   ├── repositories/        # Data layer
│   │   │   │   └── utils/               # Utility classes
│   │   │   └── res/                     # Resources (layouts, drawables, etc.)
│   │   ├── androidTest/                 # Instrumented tests
│   │   └── test/                        # Unit tests
│   └── build.gradle.kts                 # App-level build configuration
├── supabase/                            # Database SQL scripts
│   ├── 01_initial_setup.sql
│   ├── 02_ticket_actions.sql
│   ├── 03_rls_policies.sql
│   ├── 04_profiles_rls.sql
│   ├── 05_soft_delete_functions.sql
│   ├── 06_spam_handling.sql
│   ├── 07_statistics_views.sql
│   └── 08_diagnostic_queries.sql
├── build.gradle.kts                     # Project-level build configuration
├── SQL_SETUP_GUIDE.md                   # Complete database setup guide
└── README.md                            # This file
```

## 🔧 Setup Instructions

### Prerequisites

- Android Studio (latest version recommended)
- JDK 25 or higher
- Supabase account
- Android device or emulator (API 24+)

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/WIA2007-Infrastructur-Reporter.git
cd WIA2007-Infrastructur-Reporter
```

### 2. Configure Supabase

#### Create Supabase Project
1. Go to [Supabase](https://supabase.com) and create a new project
2. Note your project URL and anon key

#### Set up Database
1. Open Supabase SQL Editor
2. Run SQL files from `supabase/` folder in order (01 → 08)
3. Follow detailed instructions in `SQL_SETUP_GUIDE.md`

#### Configure App
1. Create `local.properties` in project root (if not exists)
2. Add your Supabase credentials:
```properties
supabase.url=https://your-project-id.supabase.co
supabase.key=your-anon-key
```

### 3. Build and Run

```bash
# Using Gradle wrapper
./gradlew build
./gradlew installDebug

# Or open in Android Studio and click Run
```

## 📊 Database Schema

### Key Tables

**tickets** - Main ticket/report table
- Stores all infrastructure reports
- Tracks status, severity, assignment, and deletion flags
- Supports soft-delete per role

**profiles** - User information
- Links to Supabase auth users
- Stores role, name, contact info
- Used for role-based access control

**ticket_actions** - Audit log
- Records all engineer actions (accept/reject/spam)
- Enables performance tracking
- Provides action history

See `SQL_SETUP_GUIDE.md` for complete schema details.

## 🔒 Security

- **Row-Level Security (RLS)** - All database tables use RLS policies
- **Role-Based Access** - Each user role has specific permissions
- **Soft Delete** - Tickets hidden per role, not permanently deleted
- **Audit Trail** - All significant actions logged in ticket_actions
- **Secure Functions** - Sensitive operations use SECURITY DEFINER

## 🎨 Key Features Implementation

### Soft Delete System
Tickets remain in database but can be hidden from specific role views:
- Citizens can hide tickets from their personal view
- Council can hide tickets from council dashboard
- Engineers can hide tickets from their assigned list
- Other roles can still see the ticket (maintains data integrity)

### Spam Management
Two-tier spam handling:
- Engineers mark tickets as SPAM (hidden from engineer view)
- Council marks tickets as spam (appears in Spam tab)
- Spam tickets tracked separately with `is_spam` flag

### Status Workflow
```
Pending → UNDER_REVIEW → Accepted/Rejected/SPAM
   ↓           ↓              ↓
Council    Engineer      Completed
Assigns    Processes     or Closed
```

## 📱 Screenshots

<!-- Add screenshots here -->

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run all tests
./gradlew check
```

## 🐛 Troubleshooting

### Common Issues

**Build Failures**
- Ensure JDK 25 is installed and configured
- Sync Gradle files in Android Studio
- Clear build cache: `./gradlew clean`

**Database Connection Errors**
- Verify Supabase URL and key in `local.properties`
- Check RLS policies are properly configured
- See `supabase/08_diagnostic_queries.sql` for diagnostics

**Authentication Issues**
- Confirm Supabase Auth is enabled
- Check user roles in profiles table
- Verify RLS policies allow user access

See `SQL_SETUP_GUIDE.md` for detailed database troubleshooting.

## 📚 Documentation

- **SQL_SETUP_GUIDE.md** - Complete database setup and configuration
- **Supabase SQL Scripts** - Located in `supabase/` folder
- **Javadoc** - Generate with `./gradlew javadoc`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Follow standard Java conventions
- Use meaningful variable names
- Comment complex logic
- Keep methods focused and concise

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Team

**WIA2007 Project Team**
- Course: Software Engineering
- Institution: [Your University Name]
- Academic Year: 2025/2026

## 🙏 Acknowledgments

- Supabase for backend infrastructure
- Android development community
- Course instructors and teaching assistants

## 📞 Support

For issues and questions:
- Open an issue on GitHub
- Check `SQL_SETUP_GUIDE.md` for database help
- Review diagnostic queries in `supabase/08_diagnostic_queries.sql`

---

**Version**: 3.0  
**Last Updated**: January 8, 2026  
**Status**: Active Development
