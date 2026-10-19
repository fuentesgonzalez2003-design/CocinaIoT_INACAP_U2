# Cocina IoT — INACAP — Evaluación sumativa Unidad 2

**Proyecto Android Studio listo para importar, no un APK compilado.**

## Importante antes de empezar

- Esta es una **copia independiente**. No sobreescribe el proyecto existente «Evaluación 2 - Unidad 2».
- El código NO se ha compilado en este entorno; la compilación real debe hacerse en tu Android Studio para validar las dependencias y el SDK disponibles en Windows 11.
- Se incluyen fuentes, Gradle Kotlin DSL, cinco pantallas, autenticación local, SQLite, simulación de Arduino, conexión Wi-Fi directa entre dos emuladores y adaptador Bluetooth SPP para HC-06 físico.
- No incluye el `gradle-wrapper.jar` binario. Ejecuta **Preparar-Gradle.ps1** siguiendo el paso 2 para copiarlo desde tus proyectos locales o descargarlo de la fuente oficial.

## Requisitos verificados de diseño

- Android Studio Rabbit 1 | 2026.2.1
- Android SDK API 37 instalado
- Dos emuladores Android 17.0 API 37.0
- Android Emulator 37.2.12
- Windows 11 con 16 GB RAM; se aconseja cerrar otras apps pesadas durante la prueba

## Cómo abrir en Android Studio (Windows 11)

1. Descomprime `CocinaIoT_INACAP_U2.zip` dentro de `C:\Users\5202\AndroidStudioProjects\` o cualquier carpeta diferente de la de tu proyecto actual.
2. Abre **PowerShell** en la carpeta extraída y ejecuta `powershell -ExecutionPolicy Bypass -File .\Preparar-Gradle.ps1` para dejar el Gradle Wrapper preparado. Alternativamente, copia manualmente `gradle-wrapper.jar` desde la carpeta `gradle\wrapper` de tu proyecto anterior a la carpeta homónima de este proyecto. **No modifiques el proyecto anterior.**
3. En Android Studio: **File > Open** y selecciona la carpeta `CocinaIoT_INACAP_U2` (contiene `settings.gradle.kts`).
4. Selecciona **Trust Project**, si lo solicita; sincroniza Gradle mediante **Sync Project with Gradle Files**.
5. Espera a que Android Studio descargue AGP, Gradle, Kotlin y Compose necesarios. Debe disponer de internet la primera vez.
6. Selecciona `Medium Phone API 37.0` y ejecuta ▶ **app**. Repite desde el selector con `IoT_Simulador_02`. Como tienen el mismo applicationId, las instalaciones son independientes y almacenan datos separados.

> Si Gradle indica que falta Build-Tools 36.0.0, usa **Tools > SDK Manager > SDK Tools** y permite instalar la versión solicitada. No inventes otra.
>
> Si aparece un error de sincronización, copia el error COMPLETO y consúltalo antes de cambiar versiones.

## Prueba de dos emuladores con Wi-Fi virtual

1. En **ambos emuladores**, abre «Cocina IoT»; crea un usuario y una contraseña (no están predefinidos). Después inicia sesión.
2. En el emulador **IoT_Simulador_02**: Control > **Iniciar simulador**. Anota la IP que aparece (habitualmente `10.0.2.16` o `10.0.2.17`, sin asumir cuál).
3. En **Medium Phone API 37.0**: Control > escribe la IP del simulador > **Conectar por Wi-Fi**.
4. En el primer emulador: Monitoreo debe presentar lectura de humedad, temperatura y estado (NORMAL / CRÍTICO), con actualización de unos 2 segundos.
5. En el primer emulador: Control > **E · Encender** / **A · Apagar**. Revisa cómo cambia el estado del LED verde en ambos emuladores.
6. Comprueba que el historial SQLite se guarda en el dispositivo donde se reciben las lecturas.

**Nota:** no introduzcas `127.0.0.1` ni `10.0.2.2` como dirección del segundo emulador. Usa la IP real mostrada por él. La interconexión por Wi-Fi entre emuladores requiere versiones recientes (36.5+).

## Modo HC-06 real (cuando se disponga de hardware)

- Empareja HC-06 mediante Ajustes de Android.
- Permite `BLUETOOTH_CONNECT` en Android 12+.
- En Control pulsa «Ver dispositivos emparejados» y selecciona HC-06.
- El lector interpreta el formato del informe Unidad 1: `H: 48.00% | T: 23.00C | Estado: NORMAL`.
- Control remoto envía únicamente `E` o `A` al LED independiente del **pin 8**.
- Los LEDs pin 13 (NORMAL) y pin 12 (CRÍTICO) son automáticos y NO reciben comandos remotos.
- Emulador virtual no equivale a conexión Bluetooth SPP con hardware real. Se requieren ensayos físicos antes de declararlo compatible.

## Seguridad y límites

- Acceso local: nombre de usuario guardado en SharedPreferences privadas; contraseña derivada con PBKDF2-HMAC-SHA256, sal aleatoria y 150 000 iteraciones. No se almacenan contraseñas en claro ni se incluyen credenciales de demostración fijas.
- Historial en SQLite local, limitado a 150 registros. `allowBackup=false` para no migrar datos privados por copia de seguridad.
- Wi-Fi usa TCP sin TLS **solo para demostración dentro de emuladores**. No utilizarlo en redes compartidas o Internet. No es una implementación verificada de ISO 27400.
- Humedad crítica configurada en 50 %, según el código Arduino del informe. Es un umbral del prototipo, NO una determinación científica de condensación.
- El modo simulador produce datos **sintéticos**, no mediciones reales.
- El servicio simulador permanece activo mientras la aplicación siga en ejecución; no funciona como servicio Android de larga duración en segundo plano.
- PIN 8 indicado en el código final del informe; revisar físicamente cableado antes de usar hardware real.

## Estructura del proyecto

- `app/src/main/java/cl/inacap/cocinaiot/MainActivity.kt` — UI Compose (cinco pantallas)
- `viewmodel/CocinaViewModel.kt` — estado, navegación y lógica de control
- `data/Models.kt` — modelo de sensor, tramas y umbral
- `data/LocalAuth.kt` — acceso local sin contraseña fija
- `data/HistoryDb.kt` — historial SQLite
- `network/WifiTransport.kt` — servidor y cliente Wi-Fi TCP
- `network/BluetoothSpp.kt` — interfaz Bluetooth Classic RFCOMM SPP
- `app/src/test/java/cl/inacap/cocinaiot/TelemetryCodecTest.kt` — pruebas de umbral y parser

## Versions utilizadas

- Android Gradle Plugin **9.4.0**
- Gradle **9.6.0**
- Kotlin / Compose Compiler Gradle plugin **2.4.10**
- Android `compileSdk 37`, `targetSdk 35`, `minSdk 26`
- Compose BOM **2026.09.00**
- Activity Compose **1.13.0**
- Lifecycle Compose **2.11.0**

Estas versiones proceden de documentación pública de Android/Gradle revisada en octubre de 2026. `targetSdk 35` se eligió para el proyecto académico y no implica conformidad actual con Play Store para publicar.

## Relación con rúbrica de Unidad 2 (55 puntos)

1. Interfaces suficientes: inicio, acceso, información, monitor, control.
2. Título e integrantes originales del informe U1 en la pantalla inicial.
3. Acceso local en almacenamiento privado del emulador.
4. Usuario y contraseña solicitados en pantalla con validación.
5. Descripción de la problemática, Arduino, DHT11 y HC-06.
6. Visualización de humedad, temperatura, estado y origen.
7. Control remoto del LED independiente mediante E/A.
8. Estilo visual constante Material 3.
9. Navegación clara para explicar al docente.

## Entrega

1. Ejecuta la app en emuladores y haz capturas de las cinco pantallas y las pruebas de interconexión.
2. Utiliza **Build > Generate App Bundles or APKs > Generate APKs** para crear un APK (después de compilar correctamente).
3. Sube el código a un repositorio GitHub propio (sin contraseñas ni datos privados); entrega el enlace según la guía del curso.

## Fuentes técnicas

- https://developer.android.com/build/releases/agp-9-4-0-release-notes
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- https://developer.android.com/studio/run/emulator-networking-interconnect
