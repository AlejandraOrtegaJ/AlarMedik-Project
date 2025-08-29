package com.example.medicinereminder.data.entities


// Modelo de datos para usuarios
data class User(
    var id: Long = 0,
    var name: String,
    var email: String,
    var password: String // Se almacenará con hash
)