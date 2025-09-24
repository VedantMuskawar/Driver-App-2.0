# Drivers App 2.0

An Android delivery driver app built with Kotlin, Jetpack Compose, and Firebase.

## Features

- **Firebase Phone Authentication**: Secure phone-based login with OTP verification
- **Organization Management**: Multi-organization support with role-based access
- **Offline Support**: Local caching with Room database and encrypted preferences
- **Material 3 Design**: Modern UI following Material Design guidelines
- **MVVM Architecture**: Clean architecture with separation of concerns

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Local Storage**: Room Database + EncryptedSharedPreferences
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Compose
- **Target SDK**: 34, Min SDK: 24

## Setup Instructions

### 1. Firebase Configuration

1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name: `com.example.apex`
3. Download the `google-services.json` file and replace the one in `app/` directory
4. Enable Phone Authentication in Firebase Console
5. Set up Firestore database with the following collection structure:

#### MEMBERSHIP Collection Structure
```json
{
  "userID": "string",
  "orgID": "string",
  "orgName": "string",
  "role": "number", // 0=admin, 1=manager, 2=driver
  "member": {
    "name": "string",
    "phoneNumber": "string"
  }
}
```

### 2. Android Studio Setup

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Ensure you have the latest Android SDK and build tools installed

### 3. Running the App

1. Connect an Android device or start an emulator
2. Run the app using `./gradlew installDebug` or through Android Studio

## Project Structure

```
app/src/main/java/com/pave/driversapp/
├── data/
│   ├── local/           # Room database and secure preferences
│   ├── remote/          # Firebase data sources
│   └── repository/      # Repository implementations
├── domain/
│   ├── model/           # Domain models
│   └── repository/      # Repository interfaces
├── presentation/
│   ├── ui/
│   │   ├── auth/        # Login screens
│   │   ├── home/        # Home screen
│   │   └── orgselect/   # Organization selection
│   ├── viewmodel/       # ViewModels
│   └── navigation/      # Navigation setup
└── di/                  # Dependency injection modules
```

## Key Features Implementation

### Authentication Flow
1. User enters phone number
2. Firebase sends OTP via SMS
3. User verifies OTP
4. App queries Firestore for user membership
5. If multiple organizations, user selects one
6. User session is saved securely

### Offline Support
- User data cached in Room database
- EncryptedSharedPreferences for sensitive data
- Automatic fallback to cached data when network fails

### Error Handling
- Clear error messages for common scenarios
- Retry logic for network operations
- Offline fallback mechanisms

## Testing

Testing is not required as test phone numbers and MEMBERSHIP collection documents are already configured in Firebase.

## Security Features

- Encrypted SharedPreferences for sensitive data
- Secure Firebase authentication
- Data validation and sanitization
- ProGuard rules for code obfuscation

## Future Enhancements

- Order management system
- Real-time notifications
- Driver location tracking
- Push notifications
- Offline order synchronization

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
