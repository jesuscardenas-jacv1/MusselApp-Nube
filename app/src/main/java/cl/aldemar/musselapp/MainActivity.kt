package cl.aldemar.musselapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import cl.aldemar.musselapp.ui.navegacion.MusselNavHost
import cl.aldemar.musselapp.ui.theme.MusselAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    Box(
                        Modifier
                            .weight(1f)
                            .then(if (!enLinea) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
                    ) { MusselNavHost() }
                }
            }
        }
    }
}
