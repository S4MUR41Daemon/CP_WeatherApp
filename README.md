# ATMOS//OS

App Android nativa del tiempo (Kotlin + Jetpack Compose) con estética cyberpunk — paleta **2a Night City** del prototipo.

## Qué incluye

- Pantalla única con scroll y 8 secciones: ahora, próximas horas, 14 días, viento, calidad del aire, polen, sol/luna y radar.
- Lluvia digital propia a 20 fps, reactiva al tiempo real, con pausa por ciclo de vida y modo ahorro de batería.
- Widget 4x2 (Jetpack Glance) con temperatura, código, ciudad, min/max y fila de 6 horas.
- Caché offline (DataStore) + refresco en segundo plano con WorkManager cada 60 min (`NetworkType.CONNECTED` + `requiresBatteryNotLow`).
- APIs sin clave: Open-Meteo (forecast + air quality + geocoding), RainViewer (radar) y `android.location.Geocoder` offline.
- Lógica portada 1:1 desde `WeatherApp.dc.html` (`fetchAll`, `renderVals`, umbrales de avisos).

## Compilar en local

Requiere JDK 17 y Android SDK (plataforma 34).

```bash
./gradlew assembleDebug
```

APK en `app/build/outputs/apk/debug/app-debug.apk`.

Si no tienes `./gradlew` (falta el jar del wrapper, binario no versionado), bootstrap con una instalación de Gradle 8.9:

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

## Compilar en GitHub Actions

El workflow `.github/workflows/build-apk.yml` se dispara en push a `main` y manualmente (`workflow_dispatch`). Instala Android SDK, JDK 17, genera el wrapper si falta, ejecuta tests y publica el APK como artifact `atmos-debug`.

Descarga: Actions → Build APK → Artifacts.

## Instalar en el móvil

1. Transfiere el `app-debug.apk` al teléfono (USB, Drive, lo que prefieras).
2. En Android abre el archivo. Si es la primera vez, concede permiso de «Instalar apps desconocidas» para el gestor de archivos.
3. También funciona vía ADB: `adb install app-debug.apk`.

## Añadir el widget

Mantén pulsado en el escritorio → Widgets → ATMOS → arrastra el 4x2.

## Permisos

- `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`: para la ubicación por GPS. Si se deniega, se abre la hoja de búsqueda directamente.
- `INTERNET`, `ACCESS_NETWORK_STATE`: para consultar Open-Meteo y RainViewer.

## Créditos / atribuciones

- Datos meteorológicos: [Open-Meteo](https://open-meteo.com) (CC BY 4.0).
- Radar: [RainViewer](https://www.rainviewer.com/).
- Mapa base: © OpenStreetMap, © CARTO (tiles `dark_all`).
- Fuentes: Chakra Petch y Share Tech Mono (OFL) vía Google Fonts (downloadable fonts).

## Notas

- El Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) no está en el repo porque es un binario. En CI se regenera automáticamente. En local, `gradle wrapper --gradle-version 8.9` lo crea.
- AEMET: dejado un hueco en la arquitectura para enchufar avisos oficiales en el futuro. Mientras tanto se calculan en local con los mismos umbrales del prototipo.
- APK release firmable: en `app/build.gradle.kts` se reutiliza la `debug` signing config a propósito para que `assembleRelease` también produzca algo instalable; cámbialo cuando tengas un keystore propio.
