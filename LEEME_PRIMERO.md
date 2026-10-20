# Cocina IoT — INACAP — Evaluación sumativa Unidad 2

**Aplicación Android desarrollada con Kotlin y Jetpack Compose para la evaluación de la Unidad 2.**

## Estado del proyecto (pruebas realizadas)

- **Compilación:** Android Studio mostró `BUILD SUCCESSFUL` después de la corrección del error `NetworkOnMainThreadException`.
- **Instalación:** se ejecutó la aplicación en `IoT_Simulador_02` y `Medium Phone API 37.0` (ambos Android 17, API 37.0).
- **Interconexión:** intercambio TCP de datos entre los dos emuladores, con redirección ADB del puerto `8765` a través de Windows 11.
- **Control remoto:** los comandos `E` (encender) y `A` (apagar) cambiaron el estado del LED virtual.
- **Monitoreo:** se observaron registros de humedad y temperatura, estados NORMAL / CRÍTICO e historial local SQLite.
- **APK:** se generó localmente `app-debug.apk` con la opción **Generate APKs** de Android Studio. Al ser un archivo de compilación, no se guarda en GitHub.
- **Alcance de la evidencia:** estas son pruebas manuales comunicadas por el desarrollador. No se ha validado aquí la conexión a hardware Arduino/HC-06 ni se dispone de una ejecución documentada de las pruebas unitarias automatizadas.

## Importante antes de empezar

- El repositorio contiene los archivos fuente, el Gradle Wrapper completo (`gradlew.bat` y `gradle/wrapper/gradle-wrapper.jar`) y los recursos para abrirlo en Android Studio.
- La aplicación incluye cinco pantallas, autenticación local, almacenamiento SQLite, simulador IoT, transporte TCP para emuladores y adaptador Bluetooth Classic SPP para un futuro dispositivo físico.
- Las lecturas del modo simulador son **sintéticas** y no equivalen a mediciones del sensor DHT11 real.
- Este repositorio se usa con fines académicos. No representa una implementación de seguridad industrial certificada.

## Requisitos verificados de diseño

- Android Studio Rabbit 1 | 2026.2.1
- Android SDK API 37 instalado
- Dos emuladores Android 17.0 API 37.0
- Android Emulator 37.2.12
- Windows 11 con 16 GB RAM; se aconseja cerrar otras apps pesadas durante la prueba

## Cómo abrir en Android Studio (Windows 11)

1. En Windows 11, descarga/clona el repositorio y abre la carpeta `CocinaIoT_INACAP_U2` desde **Android Studio > File > Open**. La carpeta debe contener `settings.gradle.kts`.
2. Si Android Studio pregunta **Trust Project**, confírmalo y espera **Gradle Sync**. Se requiere internet para descargar dependencias que todavía no estén en caché.
3. El **Gradle Wrapper ya está incluido**; no hace falta `Preparar-Gradle.ps1` ni copiar archivos desde otro proyecto.
4. Selecciona la configuración **app** y ejecuta la aplicación en `IoT_Simulador_02`. Repite la instalación en `Medium Phone API 37.0`. Cada emulador conserva sus datos locales por separado.
5. Si falta una plataforma o Build-Tools, revisa **Tools > SDK Manager** e instala solo el componente solicitado. No cambies las versiones del proyecto sin revisar el error.

## Comunicación entre dos emuladores: procedimiento probado con ADB

Se probó en un computador Windows 11 con dos emuladores Android 17. El Wi-Fi virtual directo (`AndroidWifi`) presentó **Connection failure**, por lo que la demostración se realizó con una **redirección de puertos ADB a través del computador anfitrión**. Es una comunicación TCP entre aplicaciones Android simuladas; **no es una conexión Wi-Fi directa entre teléfonos físicos**.

### Dispositivos usados en esta prueba

| Función | Nombre del emulador | Identificador ADB observado |
|---|---|---|
| Servidor IoT | `IoT_Simulador_02` | `emulator-5554` |
| Cliente / monitor | `Medium Phone API 37.0` | `emulator-5556` |

Los identificadores `emulator-5554` y `emulator-5556` son ejemplos **de esta sesión** y pueden cambiar al reiniciar los emuladores. Verifícalos antes de aplicar la redirección.

### Configuración paso a paso en PowerShell (Windows)

1. Inicia ambos emuladores y abre **Cocina IoT** en los dos. Registra un usuario local en cada instalación si es necesario.
2. En `IoT_Simulador_02`, entra a **Control > Iniciar simulador**. La aplicación debe indicar `MODO: SIMULADOR` y `Servidor disponible en puerto 8765`.
3. En PowerShell, comprueba qué emuladores están conectados:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
   ```

   Para verificar el nombre de cada dispositivo, sustituye el identificador que corresponda:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 emu avd name
   ```

4. Con el identificador correcto del **servidor IoT**, configura el puerto TCP `8765`:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 forward tcp:8765 tcp:8765
   ```

   La respuesta observada fue `8765`. Esta orden redirige conexiones del puerto `8765` de **Windows** hacia el puerto `8765` de `IoT_Simulador_02`.

5. En el **cliente** `Medium Phone API 37.0`, abre **Control > Monitor Wi-Fi**, escribe `10.0.2.2` y pulsa **Conectar por Wi-Fi**. `10.0.2.2` es la dirección especial que utiliza el emulador para acceder al computador anfitrión; desde allí ADB reenvía la conexión al servidor IoT.
6. Abre **Monitoreo** en Medium Phone. Debe mostrar lecturas aproximadamente cada dos segundos, cambiar entre `NORMAL` y `CRÍTICO` según el umbral de humedad del 50 % y registrar el historial en SQLite.
7. En **Control**, prueba `E` para encender y `A` para apagar el LED virtual independiente. Verifica el cambio en `IoT_Simulador_02`.

### ¿Por qué aparecen las IP 10.0.2.16 y 10.0.2.2?

- `10.0.2.16`: IP virtual mostrada por la interfaz Wi-Fi del **emulador servidor** durante esta demostración. Su valor puede variar.
- `10.0.2.2`: alias del **computador anfitrión** accesible desde el emulador. Es la dirección que debe usar Medium Phone en **este procedimiento con redirección ADB**.
- `8765`: puerto TCP del servicio del simulador.
- `127.0.0.1` dentro de Medium Phone se refiere **al propio Medium Phone**, no a Windows ni al otro emulador. Por eso no se usa para conectar el cliente en esta prueba.

Si `AndroidWifi` funciona correctamente y existe conectividad directa entre emuladores, podría utilizarse una IP virtual accesible del servidor sin redirección; **ese modo directo no fue el que se consiguió validar en esta evaluación**.

**Importante:** la redirección ADB es una configuración de prueba. Puede ser necesario repetirla al reiniciar emuladores o ADB; no requiere cambiar el código Kotlin. Mantén abierto el simulador durante la conexión del monitor.

### Resolución de problemas de la demostración

- Si el monitor muestra `EHOSTUNREACH`, revisa la IP utilizada y confirma que existe la redirección `forward` hacia el emulador correcto.
- En Windows puedes verificar el puerto local con `Test-NetConnection 127.0.0.1 -Port 8765`. Un resultado `True` confirma accesibilidad del puerto, **pero la prueba de extremo a extremo** consiste en recibir lecturas y controlar el LED.
- Si el simulador se cierra, revisa **Logcat**. El error detectado durante el desarrollo fue `NetworkOnMainThreadException`; se corrigió moviendo las operaciones de envío de red fuera del hilo principal. Esta corrección fue compilada y verificada manualmente.

## Modo HC-06 real (cuando se disponga de hardware)

- Empareja HC-06 mediante Ajustes de Android.
- Permite `BLUETOOTH_CONNECT` en Android 12+.
- En Control pulsa «Ver dispositivos emparejados» y selecciona HC-06.
- El lector interpreta el formato del informe Unidad 1: `H: 48.00% | T: 23.00C | Estado: NORMAL`.
- Control remoto envía únicamente `E` o `A` al LED independiente del **pin 8**.
- Los LEDs pin 13 (NORMAL) y pin 12 (CRÍTICO) son automáticos y NO reciben comandos remotos.
- Emulador virtual no equivale a conexión Bluetooth SPP con hardware real. Se requieren ensayos físicos antes de declararlo compatible.

## Seguridad y límites

- **Inicio de sesión local:** usuario almacenado en preferencias privadas; contraseña verificada mediante un derivado PBKDF2-HMAC-SHA256 con sal aleatoria y 150 000 iteraciones. Esto evita guardar contraseñas en texto plano; **no equivale a cifrar todo el almacenamiento**.
- **Historial:** SQLite local, con un límite de 150 registros. `allowBackup=false` evita la copia de seguridad automática de los datos de esta aplicación.
- **Transporte del simulador:** TCP **sin TLS, sin autenticación mutua y sin cifrado del canal**. Únicamente es apto para una demostración controlada con emuladores y redirección ADB. **No utilizar en redes de producción, compartidas o abiertas a Internet**.
- **Norma ISO/IEC 27400:** se tomaron en consideración principios de acceso restringido y protección de credenciales, pero **no se declara cumplimiento ni certificación de seguridad IoT**. Para una implementación real faltarían protección criptográfica de la comunicación, autenticación de dispositivos y pruebas de seguridad.
- **Sensores:** el umbral del 50 % corresponde al prototipo Arduino documentado. No significa que exista condensación real o un diagnóstico ambiental científicamente validado.
- **Simulación:** las lecturas del emulador son artificiales; no son mediciones del DHT11 físico.
- **Bluetooth HC-06:** hay un adaptador SPP en código, pero **la conexión con módulo HC-06 y Arduino físicos no se ha probado**.
- **Ejecución:** el simulador opera mientras la aplicación permanece en ejecución; no se implementó un servicio Android persistente de larga duración.
- **Hardware:** el LED controlado mediante `E`/`A` corresponde al pin 8 del código final del informe. Los LEDs de estado en pines 13 y 12 son automáticos; verificar el cableado real antes de usarlo.

## Estructura del proyecto

- `app/src/main/java/cl/inacap/cocinaiot/MainActivity.kt` — UI Compose (cinco pantallas)
- `viewmodel/CocinaViewModel.kt` — estado, navegación y lógica de control
- `data/Models.kt` — modelo de sensor, tramas y umbral
- `data/LocalAuth.kt` — acceso local sin contraseña fija
- `data/HistoryDb.kt` — historial SQLite
- `network/WifiTransport.kt` — servidor y cliente Wi-Fi TCP
- `network/BluetoothSpp.kt` — interfaz Bluetooth Classic RFCOMM SPP
- `app/src/test/java/cl/inacap/cocinaiot/TelemetryCodecTest.kt` — pruebas de umbral y parser

## Versiones utilizadas

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

## Entrega de la Unidad 2 (INACAP)

1. **Repositorio con código fuente:** https://github.com/fuentesgonzalez2003-design/CocinaIoT_INACAP_U2
2. **APK de depuración:** generado localmente en `app/build/outputs/apk/debug/app-debug.apk` mediante Android Studio. Al estar dentro de `build/`, se excluye de GitHub por `.gitignore`.
3. **Demostración:** mostrar las pantallas, el acceso local, el estado `SIMULADOR`, la conexión `MONITOR WI-FI`, cambios NORMAL/CRÍTICO, el historial SQLite y los comandos `E`/`A`.
4. **Limitaciones por explicar:** se usaron dos emuladores y la redirección ADB, no Wi-Fi directo entre teléfonos ni una conexión Bluetooth HC-06 real. El canal TCP del simulador no usa TLS.
5. **Entrega AAI:** según la guía, subir un archivo de texto con el enlace al repositorio dentro del plazo establecido. El correo electrónico no sustituye automáticamente la entrega por la plataforma.

## Fuentes técnicas

- https://developer.android.com/build/releases/agp-9-4-0-release-notes
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- https://developer.android.com/studio/run/emulator-networking-interconnect
