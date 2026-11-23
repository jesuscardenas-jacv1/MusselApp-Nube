package cl.aldemar.musselapp

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.aldemar.musselapp.ui.componentes.BannerRevision
import cl.aldemar.musselapp.ui.componentes.LocalEnLinea
import cl.aldemar.musselapp.ui.navegacion.MusselNavHost
import cl.aldemar.musselapp.ui.theme.MusselAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Solo en release: la app no aparece en apps recientes ni en capturas o grabaciones de pantalla
        // (login, PIN, revisión). El debug lo permite para poder tomar las capturas del informe.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
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
                val avisos by MusselApplication.contenedor.avisos.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize()) {
                    // La luz de la barra superior avisa si no hay internet
                    CompositionLocalProvider(LocalEnLinea provides enLinea) { MusselNavHost() }
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
