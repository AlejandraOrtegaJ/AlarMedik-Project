package com.example.medicinereminder.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.R
import com.example.medicinereminder.data.DatabaseHelper
import com.example.medicinereminder.services.MedicationReminderService
import com.example.medicinereminder.ui.adapters.DailyMedicationAdapter
import com.example.medicinereminder.ui.components.CalendarView
import com.example.medicinereminder.data.entities.Medication
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {
    private var userId: Long = 0
    private lateinit var db: DatabaseHelper
    private lateinit var tvHello: TextView
    private lateinit var tvMedicationCount: TextView
    private lateinit var tvAdherence: TextView
    private lateinit var tvDailyMedicationsTitle: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var rvDailyMedications: RecyclerView
    private lateinit var dailyMedicationAdapter: DailyMedicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        userId = intent.getLongExtra("userId", 0)
        Log.d("HomeActivity", "User ID recibido: $userId")

        db = DatabaseHelper(this)

        // Iniciar el servicio de recordatorios
        MedicationReminderService.startService(this, userId)

        setupViews()
        setupCalendar()
        setupRecyclerView()
        loadUserData()
        loadMedicationDates()
    }

    private fun setupViews() {
        tvHello = findViewById(R.id.tvHello)
        tvMedicationCount = findViewById(R.id.tvMedicationCount)
        tvAdherence = findViewById(R.id.tvAdherence)
        tvDailyMedicationsTitle = findViewById(R.id.tvDailyMedicationsTitle)
        calendarView = findViewById(R.id.calendarView)
        rvDailyMedications = findViewById(R.id.rvDailyMedications)

        // Botón de historial
        val btnHistory = findViewById<MaterialButton>(R.id.btnHistory)
        btnHistory.setOnClickListener {
            val i = Intent(this, HistoryActivity::class.java)
            i.putExtra("userId", userId)
            startActivity(i)
        }

        // Botón de agregar medicamento
        val btnAddMedication = findViewById<MaterialButton>(R.id.btnAddMedication)
        btnAddMedication.setOnClickListener {
            val i = Intent(this, AddMedicationActivity::class.java)
            i.putExtra("userId", userId)
            startActivity(i)
        }
    }

    private fun setupCalendar() {
        // Configurar listener para cuando se selecciona una fecha
        calendarView.setOnDateSelectedListener { selectedDate ->
            loadMedicationsForDate(selectedDate)
            updateDailyMedicationsTitle(selectedDate)
            showMedicationSummary()
        }
    }

    private fun setupRecyclerView() {
        dailyMedicationAdapter = DailyMedicationAdapter(
            medications = emptyList(),
            onMedicationClick = { medication ->
                showMedicationTakenDialog(medication)
            }
        )

        rvDailyMedications.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = dailyMedicationAdapter
        }
    }

    private fun showMedicationTakenDialog(medication: Medication) {
        val options = arrayOf("Marcar como tomado", "Ver detalles", "Cancelar")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Medicamento: ${medication.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> markMedicationAsTaken(medication)
                    1 -> showMedicationDetails(medication)
                    2 -> { /* Cancelar */ }
                }
            }
            .show()
    }

    private fun markMedicationAsTaken(medication: Medication) {
        try {
            val selectedDate = calendarView.getSelectedDate()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = dateFormat.format(selectedDate.time)
            
            val success = db.markMedicationAsTaken(medication.id, dateStr)
            
            if (success) {
                Toast.makeText(this, "${medication.name} marcado como tomado", Toast.LENGTH_SHORT).show()
                
                // Actualizar la lista de medicamentos
                loadMedicationsForDate(selectedDate)
                loadMedicationDates()
                showMedicationSummary()
            } else {
                Toast.makeText(this, "Error al marcar como tomado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error al marcar medicamento como tomado", e)
            Toast.makeText(this, "Error al marcar como tomado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMedicationDetails(medication: Medication) {
        val details = StringBuilder()
        details.append("Nombre: ${medication.name}\n")
        details.append("Dosis: ${medication.dose ?: "No especificada"}\n")
        details.append("Frecuencia: ${medication.frequency}\n")
        details.append("Hora principal: ${medication.time}\n")
        
        if (medication.doses.isNotEmpty()) {
            details.append("Dosis programadas:\n")
            medication.doses.forEach { dose ->
                details.append("• Dosis ${dose.doseNumber}: ${dose.time}\n")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalles del medicamento")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadUserData() {
        try {
            // Obtener información del usuario para mostrar en el dashboard
            val user = db.findUserById(userId)
            Log.d("HomeActivity", "Usuario encontrado: $user")

            user?.let {
                tvHello.text = "Hola, ${it.name}"
            } ?: run {
                tvHello.text = "Hola"
                Log.w("HomeActivity", "Usuario no encontrado con ID: $userId")
            }

            // Mostrar resumen de medicamentos
            showMedicationSummary()
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error en loadUserData", e)
            Toast.makeText(this, "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMedicationSummary() {
        try {
            val selectedDate = calendarView.getSelectedDate()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = dateFormat.format(selectedDate.time)
            
            val medications = db.getMedicationsForDate(userId, dateStr)
            val adherenceRate = db.getAdherenceRate(userId, dateStr)
            val medicationCount = medications.size

            // Actualizar la UI con el resumen
            tvMedicationCount.text = medicationCount.toString()
            tvAdherence.text = "${"%.1f".format(adherenceRate)}%"

            Log.d("HomeActivity", "Medicamentos encontrados: $medicationCount, Adherencia: $adherenceRate%")
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error en showMedicationSummary", e)
            Toast.makeText(this, "Error al cargar resumen de medicamentos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMedicationDates() {
        try {
            val medications = db.getMedicationsForUser(userId)
            val dateMap = mutableMapOf<String, Int>()
            
            // Por ahora, asumimos que todos los medicamentos son diarios
            // En una implementación completa, esto debería considerar la frecuencia
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dateMap[today] = medications.size
            
            calendarView.setMedicationDates(dateMap)
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error al cargar fechas de medicamentos", e)
        }
    }

    private fun loadMedicationsForDate(date: Calendar) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = dateFormat.format(date.time)
            val medications = db.getMedicationsForDate(userId, dateStr)
            
            Log.d("HomeActivity", "Cargando medicamentos para $dateStr: ${medications.size} encontrados")
            dailyMedicationAdapter.updateMedications(medications)
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error al cargar medicamentos para la fecha", e)
            Toast.makeText(this, "Error al cargar medicamentos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDailyMedicationsTitle(date: Calendar) {
        val dateFormat = SimpleDateFormat("dd 'de' MMMM", Locale("es"))
        tvDailyMedicationsTitle.text = "Medicamentos del ${dateFormat.format(date.time)}"
    }

    override fun onResume() {
        super.onResume()
        // Actualizar datos cuando la actividad se reanude
        loadUserData()
        loadMedicationDates()
        loadMedicationsForDate(calendarView.getSelectedDate())
    }

    override fun onDestroy() {
        super.onDestroy()
        // No detenemos el servicio aquí para que continúe en segundo plano
        // El servicio se detendrá cuando la app sea cerrada completamente
    }
}