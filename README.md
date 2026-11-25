# Durchzug

Android app that helps you air your home properly. It compares the air in your room with the current weather at your location and tells you whether airing is worthwhile, how humid the room will be afterwards and how long you should air.

## Features

- Enter temperature and relative humidity of your room
- Location search and current outdoor conditions from [OpenWeatherMap](https://openweathermap.org)
- Absolute humidity indoors and outdoors (Magnus formula)
- Recommendation whether airing makes the room air drier
- Estimated relative humidity after airing
- Recommended airing duration based on outdoor temperature, wind and temperature difference

## Build

Requirements: Android Studio with Android SDK 37.1 and JDK 17 or newer.

1. Clone the repository:
   ```
   git clone https://github.com/Avarast/Durchzug.git
   ```
2. Create a free API key at [openweathermap.org](https://openweathermap.org/api). Activation can take up to two hours.
3. Copy `local.properties.example` to `local.properties` and add your key:
   ```
   OWM_API_KEY=your_openweathermap_key
   ```
4. Open the project in Android Studio and run it, or use the command line:
   ```
   ./gradlew test assembleDebug
   ```
   The APK is placed in `app/build/outputs/apk/debug/`.

> [!WARNING]
> Your API key is embedded in the APK and can be extracted from it. If you share the APK, everyone using it makes requests with your key, which counts against your OpenWeatherMap quota and may incur costs.

## Data source

Weather data provided by [OpenWeatherMap](https://openweathermap.org), licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
