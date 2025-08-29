package com.example.medicinereminder.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicinereminder.R
import com.example.medicinereminder.data.DatabaseHelper
import com.example.medicinereminder.data.entities.Dose
import com.example.medicinereminder.data.entities.Medication
import com.example.medicinereminder.receivers.AlarmReceiver
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

// Actividad para agregar nuevos medicamentos con recordatorios
class AddMedicationActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper
    private var userId: Long = 0
    private val doses = mutableListOf<Dose>()
    private var doseCounter = 1
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medication)
        db = DatabaseHelper(this)
        userId = intent.getLongExtra("userId", 0)

        setupViews()
        setupDoseManagement()
        setupDatePickers()
    }

    private fun setupViews() {
        val etName = findViewById<EditText>(R.id.etName)
        val etDose = findViewById<EditText>(R.id.etDose)
        val spFrequency = findViewById<Spinner>(R.id.spFrequency)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        // Configurar spinner de frecuencia
        val frequencies = arrayOf("Diario", "Semanal", "Mensual")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        spFrequency.adapter = adapter

        btnSave.setOnClickListener {
            saveMedication()
        }
    }

    private fun setupDatePickers() {
        val btnStartDate = findViewById<MaterialButton>(R.id.btnStartDate)
        val btnEndDate = findViewById<MaterialButton>(R.id.btnEndDate)
        val tvStartDate = findViewById<TextView>(R.id.tvStartDate)
        val tvEndDate = findViewById<TextView>(R.id.tvEndDate)

        btnStartDate.setOnClickListener {
            showDatePickerDialog { date ->
                startDate = date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
                tvStartDate.text = "Fecha inicio: ${dateFormat.format(date.time)}"
            }
        }

        btnEndDate.setOnClickListener {
            showDatePickerDialog { date ->
                endDate = date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
                tvEndDate.text = "Fecha fin: ${dateFormat.format(date.time)}"
            }
        }
    }

    private fun setupDoseManagement() {
        val btnAddDose = findViewById<MaterialButton>(R.id.btnAddDose)
        val llDoses = findViewById<LinearLayout>(R.id.llDoses)

        btnAddDose.setOnClickListener {
            showTimePickerDialog { time ->
                val dose = Dose(
                    time = time,
                    doseNumber = doseCounter++
                )
                doses.add(dose)
                addDoseView(dose, llDoses)
            }
        }
    }

    private fun showDatePickerDialog(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                onDateSelected(selectedDate)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTimePickerDialog(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(time)
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun addDoseView(dose: Dose, container: LinearLayout) {
        val doseView = LayoutInflater.from(this).inflate(R.layout.item_dose, container, false)
        
        val tvDoseTime = doseView.findViewById<TextView>(R.id.tvDoseTime)
        val btnRemoveDose = doseView.findViewById<ImageButton>(R.id.btnRemoveDose)
        
        tvDoseTime.text = "Dosis ${dose.doseNumber}: ${dose.time}"
        
        btnRemoveDose.setOnClickListener {
            doses.remove(dose)
            container.removeView(doseView)
            updateDoseNumbers()
        }
        
        container.addView(doseView)
    }

    private fun updateDoseNumbers() {
        doses.forEachIndexed { index, dose ->
            dose.doseNumber = index + 1
        }
        doseCounter = doses.size + 1
    }

    private fun saveMedication() {
        val etName = findViewById<EditText>(R.id.etName)
        val etDose = findViewById<EditText>(R.id.etDose)
        val spFrequency = findViewById<Spinner>(R.id.spFrequency)

        val name = etName.text.toString().trim()
        val dose = etDose.text.toString().trim()
        val frequency = spFrequency.selectedItem.toString()

        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre del medicamento es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (doses.isEmpty()) {
            Toast.makeText(this, "Debes agregar al menos una dosis", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateStr = startDate?.let { dateFormat.format(it.time) }
        val endDateStr = endDate?.let { dateFormat.format(it.time) }

        val med = Medication(
            name = name,
            dose = if (dose.isEmpty()) null else dose,
            frequency = frequency,
            duration = null, // Ya no usamos este campo
            time = doses.first().time, // Hora principal
            userId = userId,
            doses = doses.toList(),
            startDate = startDateStr,
            endDate = endDateStr
        )

        val id = db.addMedication(med)
        if (id > 0) {
            // Programar alarmas para cada dosis
            doses.forEach { dose ->
                scheduleAlarm(name, dose.time, id, dose.doseNumber)
            }
            Toast.makeText(this, "Medicación guardada y alarmas programadas", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleAlarm(name: String, time: String, medId: Long, doseNumber: Int) {
        // Parsear hora en formato HH:MM
        val parts = time.split(":")
        if (parts.size < 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        // Configurar calendario para la alarma
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)

        // Si la hora ya pasó hoy, programar para mañana
        if (cal.timeInMillis < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Configurar intent para la alarma
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("medName", name)
        intent.putExtra("medId", medId)
        intent.putExtra("doseNumber", doseNumber)

        val requestCode = ((medId * 100) + doseNumber).toInt()
        val pi = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar alarma recurrente diaria
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }
}