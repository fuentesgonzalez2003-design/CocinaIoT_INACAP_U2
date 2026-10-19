package cl.inacap.cocinaiot

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.inacap.cocinaiot.data.HUMIDITY_LIMIT
import cl.inacap.cocinaiot.data.SensorReading
import cl.inacap.cocinaiot.viewmodel.AppState
import cl.inacap.cocinaiot.viewmodel.CocinaViewModel
import cl.inacap.cocinaiot.viewmodel.LinkMode
import cl.inacap.cocinaiot.viewmodel.Page
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Petrol = Color(0xFF087C80)
private val Deep = Color(0xFF183340)
private val Base = Color(0xFFF3F7F9)
private val Amber = Color(0xFFB85E1C)
private val Red = Color(0xFFBB3B36)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CocinaApp() }
    }
}

@Composable
private fun CocinaApp(vm: CocinaViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = if (isDark) darkColorScheme(
        primary = Color(0xFF76D2D2), secondary = Color(0xFFAACED1),
        background = Color(0xFF101A20), surface = Color(0xFF1A282E)
    ) else lightColorScheme(
        primary = Petrol, secondary = Deep, background = Base,
        surface = Color.White, onBackground = Deep
    )
    MaterialTheme(colorScheme = palette) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("COCINA IoT", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            Text("INACAP · Evaluación Unidad 2", style = MaterialTheme.typography.labelSmall)
                        }
                        if (state.authenticated) TextButton(onClick = vm::logout) { Text("Salir") }
                    }
                }
            },
            bottomBar = {
                if (state.authenticated) {
                    NavigationBar {
                        listOf(
                            Triple(Page.INFO, "Información", "i"),
                            Triple(Page.MONITOR, "Monitoreo", "%"),
                            Triple(Page.CONTROL, "Control", "↗")
                        ).forEach { (page, label, symbol) ->
                            NavigationBarItem(
                                selected = state.page == page,
                                onClick = { vm.open(page) },
                                icon = { Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (state.page) {
                    Page.START -> StartPage { vm.open(Page.LOGIN) }
                    Page.LOGIN -> LoginPage(state, vm)
                    Page.INFO -> InformationPage()
                    Page.MONITOR -> MonitorPage(state)
                    Page.CONTROL -> ControlPage(state, vm)
                }
            }
        }
    }
}

@Composable
private fun StartPage(onContinue: () -> Unit) {
    Spacer(Modifier.height(22.dp))
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MONITOREO INTELIGENTE", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Text("Sistema IoT de cocina", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
            Text("Temperatura · Humedad · Alertas ambientales · Control remoto")
            Text("Arduino Uno · DHT11 · Bluetooth HC-06",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    TitleText("Proyecto académico")
    CleanCard {
        Text("Asignatura: Aplicaciones Móviles para IoT (TI3042)")
        Text("Unidad 2 · Interfaces Android")
        HorizontalDivider()
        Text("Integrantes del grupo inicial", fontWeight = FontWeight.SemiBold)
        Text("Claudio Hidalgo")
        Text("Benjamín Fuentes")
    }
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Ingresar a la aplicación")
    }
    Text("Modo demostración disponible: dos emuladores conectados por Wi-Fi virtual.",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun LoginPage(state: AppState, vm: CocinaViewModel) {
    var user by remember(state.registered) { mutableStateOf(if (state.registered) state.username else "") }
    var pass by remember { mutableStateOf("") }
    TitleText(if (state.registered) "Control de acceso" else "Crear usuario local")
    CleanCard {
        Text(
            if (state.registered) "Ingresa tus credenciales para continuar."
            else "Primera ejecución: registra el usuario de este emulador.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = user, onValueChange = { user = it }, label = { Text("Usuario") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.authMessage.isNotBlank()) {
            Text(state.authMessage, color = MaterialTheme.colorScheme.primary)
        }
        Button(
            onClick = {
                if (state.registered) vm.login(user, pass)
                else { vm.register(user, pass); pass = "" }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.registered) "Iniciar sesión" else "Crear usuario") }
        Text("Contraseñas: mínimo 8 caracteres. Se guarda un hash con sal, no el texto de la contraseña.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InformationPage() {
    TitleText("Información del proyecto")
    CleanCard {
        Text("¿Qué problema resolvemos?", fontWeight = FontWeight.Bold)
        Text("El vapor generado al cocinar puede elevar la humedad y contribuir a la condensación. " +
            "El proyecto mide humedad y temperatura, presenta el estado ambiental y facilita una alerta temprana.")
    }
    CleanCard {
        Text("Componentes del prototipo", fontWeight = FontWeight.Bold)
        InfoRow("Arduino Uno", "Procesa lecturas y comandos")
        InfoRow("DHT11", "Humedad relativa y temperatura")
        InfoRow("HC-06", "Comunicación Bluetooth Classic")
        InfoRow("LED celeste · pin 13", "NORMAL")
        InfoRow("LED rojo · pin 12", "CRÍTICO")
        InfoRow("LED verde · pin 8", "Control remoto E / A")
    }
    CleanCard {
        Text("Regla del prototipo", fontWeight = FontWeight.Bold)
        Text("Humedad menor que 50 % → NORMAL")
        Text("Humedad desde 50 % → CRÍTICO")
        Text("Es un umbral académico configurable, no un cálculo físico de punto de rocío.",
            style = MaterialTheme.typography.bodySmall)
    }
    CleanCard {
        Text("Modos disponibles", fontWeight = FontWeight.Bold)
        Text("1. Segundo emulador como simulador de Arduino")
        Text("2. Primer emulador como monitor Wi-Fi")
        Text("3. HC-06 real, cuando haya teléfono Android físico")
    }
}

@Composable
private fun MonitorPage(state: AppState) {
    TitleText("Monitoreo ambiental")
    StateBanner(state)
    val reading = state.reading
    if (reading == null) {
        CleanCard {
            Text("Aún no hay lecturas", fontWeight = FontWeight.Bold)
            Text("Abre Control y elige «Iniciar simulador» o conecta por Wi-Fi al segundo emulador.")
        }
    } else {
        ReadingCards(reading)
        CleanCard {
            Text("Diagnóstico", fontWeight = FontWeight.Bold)
            Text(if (reading.condition == "NORMAL")
                "Ambiente normal según el umbral del prototipo."
            else "Humedad igual o superior al límite. Se recomienda ventilar la cocina.")
            Text("Origen: ${reading.source}", style = MaterialTheme.typography.bodySmall)
            Text("Recibido: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(reading.receivedAt))}",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    TitleText("Historial local (SQLite)")
    CleanCard {
        if (state.history.isEmpty()) Text("No hay registros guardados todavía.")
        state.history.take(10).forEach { item ->
            Text("${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.dateTime))}  " +
                "${String.format(Locale.US, "%.1f", item.humidity)} %  ·  " +
                "${String.format(Locale.US, "%.1f", item.temperature)} °C  ·  ${item.condition}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    Text("Las lecturas del simulador son artificiales; no deben interpretarse como mediciones reales.",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ReadingCards(reading: SensorReading) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SensorCard("HUMEDAD", reading.humidityText, Modifier.weight(1f))
        SensorCard("TEMPERATURA", reading.temperatureText, Modifier.weight(1f))
    }
    val critical = reading.condition != "NORMAL"
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (critical) Red.copy(alpha = 0.14f) else Petrol.copy(alpha = 0.13f)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("ESTADO AMBIENTAL", style = MaterialTheme.typography.labelMedium)
            Text(reading.condition, fontWeight = FontWeight.Bold,
                color = if (critical) Red else Petrol, fontSize = 27.sp)
            Text("Umbral de humedad: $HUMIDITY_LIMIT %", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SensorCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, color = MaterialTheme.colorScheme.primary,
                fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ControlPage(state: AppState, vm: CocinaViewModel) {
    TitleText("Control a distancia")
    StateBanner(state)
    CleanCard {
        Text("1 · Simulador IoT", fontWeight = FontWeight.Bold)
        Text("En IoT_Simulador_02, inicia este modo. Genera lecturas cada 2 segundos y recibe los comandos E / A.")
        Text("Dirección de este emulador: ${state.ipAddress}",
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Button(onClick = vm::startSimulator, modifier = Modifier.fillMaxWidth()) {
            Text("Iniciar simulador")
        }
    }
    CleanCard {
        Text("2 · Monitor Wi-Fi", fontWeight = FontWeight.Bold)
        Text("En Medium Phone, escribe la IP mostrada por el simulador y pulsa Conectar.")
        OutlinedTextField(
            value = state.wifiTarget, onValueChange = vm::setWifiTarget,
            label = { Text("IP del emulador simulador") },
            placeholder = { Text("Ej.: 10.0.2.17") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = vm::connectWifi, modifier = Modifier.fillMaxWidth()) {
            Text("Conectar por Wi-Fi")
        }
    }
    BluetoothPanel(state, vm)
    CleanCard {
        Text("3 · Control independiente del LED verde", fontWeight = FontWeight.Bold)
        Text("LED en pin 8: ${if (state.greenLedOn) "ENCENDIDO" else "APAGADO"}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { vm.sendLed(true) }, modifier = Modifier.weight(1f),
                enabled = state.mode != LinkMode.NONE) { Text("E · Encender") }
            OutlinedButton(onClick = { vm.sendLed(false) }, modifier = Modifier.weight(1f),
                enabled = state.mode != LinkMode.NONE) { Text("A · Apagar") }
        }
        Text("Los LEDs de estado (pines 13 y 12) nunca se controlan mediante E/A.",
            style = MaterialTheme.typography.bodySmall)
    }
    OutlinedButton(onClick = vm::disconnect, modifier = Modifier.fillMaxWidth()) {
        Text("Detener / desconectar")
    }
    Text("Wi-Fi entre emuladores utiliza TCP sin cifrado, solo en red virtual aislada para la evaluación.",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun BluetoothPanel(state: AppState, vm: CocinaViewModel) {
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.prepareBluetooth() }
    CleanCard {
        Text("Bluetooth HC-06 · conexión real", fontWeight = FontWeight.Bold)
        Text("Para usar más adelante con Arduino y teléfono Android físico. Requiere emparejar HC-06 en Ajustes.")
        OutlinedButton(onClick = {
            if (Build.VERSION.SDK_INT >= 31 && !vmBluetoothPermission(vm))
                requestPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            else vm.prepareBluetooth()
        }, modifier = Modifier.fillMaxWidth()) { Text("Ver dispositivos emparejados") }
        state.bluetoothDevices.forEach { (name, address) ->
            OutlinedButton(onClick = { vm.connectBluetooth(address) },
                modifier = Modifier.fillMaxWidth()) { Text("$name · $address") }
        }
    }
}

// Comprueba permiso sin solicitarlo automáticamente al entrar a una pantalla.
private fun vmBluetoothPermission(vm: CocinaViewModel): Boolean = vm.bluetoothPermissionGranted()

@Composable
private fun StateBanner(state: AppState) {
    Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text("MODO: ${when (state.mode) {
                LinkMode.NONE -> "SIN CONEXIÓN"
                LinkMode.SIMULATOR -> "SIMULADOR"
                LinkMode.WIFI -> "MONITOR WI-FI"
                LinkMode.BLUETOOTH -> "BLUETOOTH HC-06"
            }}", fontWeight = FontWeight.Bold)
            Text(state.connectionMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CleanCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun TitleText(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(left: String, right: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(left, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(right, style = MaterialTheme.typography.bodyMedium)
    }
}
