package cl.aldemar.musselapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.aldemar.musselapp.ui.componentes.BannerRevision
import cl.aldemar.musselapp.ui.navegacion.MusselNavHost
import cl.aldemar.musselapp.ui.theme.MusselAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras del sistema con íconos oscuros: todas las pantallas tienen fondo claro
        // (la cámara a pantalla completa las oculta)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
        )
        // Android 13+: pide permiso para avisar cuando el supervisor revise una muestra
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MusselAppTheme {
                val enLinea by MusselApplication.contenedor.conectividad.enLinea.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize()) {
                    if (!enLinea) {
                        Text(
                            text = "Sin conexión: las muestras se guardan en el teléfono y se envían al volver internet",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    val avisos by MusselApplication.contenedor.avisos.collectAsStateWithLifecycle()
                    Box(
                        Modifier
                            .weight(1f)
                            .then(if (!enLinea) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
                    ) {
                        MusselNavHost()
                        if (avisos.isNotEmpty()) {
                            BannerRevision(
                                avisos = avisos,
                                alCerrar = { MusselApplication.contenedor.avisos.value = emptyList() },
                                alVerHistorial = {
                                    MusselApplication.contenedor.avisos.value = emptyList()
                                    MusselApplication.contenedor.irAHistorial.value = true
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
