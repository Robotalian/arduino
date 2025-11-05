package udb.edu.sv

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var tvDistance: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnStop: Button
    private lateinit var btnSensorControl: Button
    private lateinit var btnManualControl: Button

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var connectedThread: ConnectedThread? = null

    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        // IMPORTANTE: Cambiar por la dirección MAC real de tu ESP32
        const val DEVICE_ADDRESS = "00:00:00:00:00:00"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBluetooth()
        setupClickListeners()
    }

    private fun initViews() {
        tvDistance = findViewById(R.id.tvDistance)
        tvStatus = findViewById(R.id.tvStatus)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnStop = findViewById(R.id.btnStop)
        btnSensorControl = findViewById(R.id.btnSensorControl)
        btnManualControl = findViewById(R.id.btnManualControl)
    }

    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            showToast("Bluetooth no disponible en este dispositivo")
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            showToast("Por favor active el Bluetooth")
            return
        }

        connectToDevice()
    }

    private fun setupClickListeners() {
        btnLeft.setOnClickListener {
            sendCommand('B') // Izquierda
            showToast("Motor: IZQUIERDA")
        }

        btnRight.setOnClickListener {
            sendCommand('F') // Derecha
            showToast("Motor: DERECHA")
        }

        btnStop.setOnClickListener {
            sendCommand('S') // Stop
            showToast("Motor: DETENIDO")
        }

        btnSensorControl.setOnClickListener {
            sendCommand('A') // Activar control automático
            showToast("Control por sensor ACTIVADO")
        }

        btnManualControl.setOnClickListener {
            sendCommand('M') // Volver a control manual
            showToast("Control manual ACTIVADO")
        }
    }

    private fun connectToDevice() {
        try {
            val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(DEVICE_ADDRESS)
            bluetoothSocket = device.createRfcommSocketToServiceRecord(bluetoothUUID)

            Thread {
                try {
                    bluetoothSocket!!.connect()
                    outputStream = bluetoothSocket!!.outputStream
                    inputStream = bluetoothSocket!!.inputStream

                    handler.post {
                        tvStatus.text = "Conectado a ESP32"
                        showToast("Conexión Bluetooth exitosa")
                    }

                    // Iniciar hilo para recibir datos
                    connectedThread = ConnectedThread()
                    connectedThread!!.start()

                } catch (e: IOException) {
                    handler.post {
                        tvStatus.text = "Error de conexión"
                        showToast("Error al conectar: ${e.message}")
                    }
                }
            }.start()

        } catch (e: Exception) {
            handler.post {
                tvStatus.text = "Error de configuración"
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun sendCommand(command: Char) {
        try {
            outputStream?.write(command.toInt())
        } catch (e: IOException) {
            handler.post {
                showToast("Error enviando comando")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDistance(distance: Int) {
        handler.post {
            tvDistance.text = "Distancia: $distance cm"

            // Cambiar color según la distancia
            when {
                distance in 100..200 -> tvDistance.setBackgroundColor(0x7D4CAF50.toInt())
                distance in 201..300 -> tvDistance.setBackgroundColor(0x7DFF9800.toInt())
                else -> tvDistance.setBackgroundColor(0x7DF44336.toInt())
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private inner class ConnectedThread : Thread() {
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    val data = String(buffer, 0, bytes)

                    if (data.startsWith("DIST:")) {
                        val distanceStr = data.substring(5).trim()
                        val distance = distanceStr.toIntOrNull()
                        distance?.let { updateDistance(it) }
                    }

                } catch (e: IOException) {
                    handler.post {
                        tvStatus.text = "Conexión perdida"
                    }
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
            connectedThread?.interrupt()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}