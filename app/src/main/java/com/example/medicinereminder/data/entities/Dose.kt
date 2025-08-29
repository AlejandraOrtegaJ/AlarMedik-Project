package com.example.medicinereminder.data.entities

// Modelo para representar una dosis individual de medicamento
data class Dose(
    var id: Long = 0,
    var medicationId: Long = 0,
    var time: String, // HH:MM formato 24h
    var isEnabled: Boolean = true,
    var doseNumber: Int = 1 // 1ra dosis, 2da dosis, etc.
)
