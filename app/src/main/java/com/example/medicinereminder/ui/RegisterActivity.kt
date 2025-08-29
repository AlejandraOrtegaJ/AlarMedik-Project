package com.example.medicinereminder.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicinereminder.data.DatabaseHelper
import com.example.medicinereminder.data.entities.User
import com.example.medicinereminder.R
import com.example.medicinereminder.utils.HashUtil

// Actividad de registro de nuevos usuarios
class RegisterActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        db = DatabaseHelper(this)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnReg = findViewById<Button>(R.id.btnRegister)

        btnReg.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val existing = db.findUserByEmail(email)
            if (existing != null) {
                Toast.makeText(this, "Email ya registrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hashed = HashUtil.sha256(pass)
            val id = db.addUser(User(name = name, email = email, password = hashed))

            if (id > 0) {
                Toast.makeText(this, "Registrado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}