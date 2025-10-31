package udb.edu.evaluado3_kotlin_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import udb.edu.evaluado3_kotlin_project.ui.theme.Evaluado3_kotlin_projectTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class MainActivity : ComponentActivity() {

    private var socket: Socket? = null
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null
    private var connected = false
    private var autoMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Evaluado3_kotlin_projectTheme {
                var distancia by remember { mutableStateOf("0") }
                var status by remember { mutableStateOf("Desconectado") }

                AppUI(
                    status = status,
                    distancia = distancia,
                    onConnect = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                socket = Socket(Constants.ESP_IP, Constants.ESP_PORT)
                                output = PrintWriter(socket!!.getOutputStream(), true)
                                input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                                connected = true
                                status = "Conectado a ${Constants.ESP_IP}"

                                // Recibir datos del sensor
                                while (connected) {
                                    val data = input?.readLine()
                                    if (data != null && data.isNotEmpty()) {
                                        distancia = data
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                status = "Error de conexión"
                                connected = false
                            }
                        }
                    },
                    onCommand = { cmd ->
                        if (connected) {
                            output?.println(cmd)
                        }
                    },
                    onToggleAuto = {
                        if (connected) {
                            autoMode = !autoMode
                            output?.println(if (autoMode) "A" else "M")
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connected = false
        socket?.close()
    }
}

@Composable
fun AppUI(
    status: String,
    distancia: String,
    onConnect: () -> Unit,
    onCommand: (String) -> Unit,
    onToggleAuto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.barcelona),
            contentDescription = "Escudo del Barcelona",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Estado: $status")
        Text(text = "Distancia: $distancia cm")

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onConnect) { Text("Conectar Wi-Fi") }

        Spacer(modifier = Modifier.height(20.dp))

        Row {
            Button(onClick = { onCommand("R") }, modifier = Modifier.padding(4.dp)) {
                Text("Derecha")
            }
            Button(onClick = { onCommand("L") }, modifier = Modifier.padding(4.dp)) {
                Text("Izquierda")
            }
            Button(onClick = { onCommand("S") }, modifier = Modifier.padding(4.dp)) {
                Text("Detener")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { onToggleAuto() }) {
            Text("Modo Automático")
        }
    }
}