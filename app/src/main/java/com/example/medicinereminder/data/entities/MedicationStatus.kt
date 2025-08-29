package com.example.medicinereminder.data.entities

import java.util.*

// Modelo para el estado de un medicamento en una fecha específica
data class MedicationStatus(
    var id: Long = 0,
    var medicationId: Long = 0,
    var date: String, // YYYY-MM-DD
    var isTaken: Boolean = false,
    var takenTime: String? = null, // HH:MM cuando fue tomado
    var doseNumber: Int = 1
)
