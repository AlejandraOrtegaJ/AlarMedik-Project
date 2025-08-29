package com.example.medicinereminder.data.entities

// Modelo de datos para medicamentos
data class Medication(
    var id: Long = 0,
    var name: String,
    var dose: String?,
    var frequency: String, // Diario, Semanal, etc.
    var duration: String?, // Duración del tratamiento
    var time: String, // HH:MM formato 24h (hora principal)
    var userId: Long = 0,
    var doses: List<Dose> = emptyList(), // Lista de dosis múltiples
    var startDate: String? = null, // Fecha de inicio (YYYY-MM-DD)
    var endDate: String? = null // Fecha de fin (YYYY-MM-DD)
)