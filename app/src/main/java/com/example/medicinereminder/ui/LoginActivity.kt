package com.example.medicinereminder.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.medicinereminder.data.DatabaseHelper
import com.example.medicinereminder.data.entities.User
import com.example.medicinereminder.R
import com.example.medicinereminder.utils.HashUtil
import java.util.concurrent.Executor

// Actividad de inicio de sesión con autenticación biométrica opcional
class LoginActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        db = DatabaseHelper(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Configurar autenticación biométrica
        setupBiometricAuth()

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = db.findUserByEmail(email)
            if (user == null) {
                Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hashed = HashUtil.sha256(pass)
            if (user.password == hashed) {
                // Autenticación exitosa
                Log.d("LoginActivity", "Login exitoso. User ID: ${user.id}")
                val i = Intent(this, HomeActivity::class.java)
                i.putExtra("userId", user.id)
                startActivity(i)
                finish()
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Botón para autenticación biométrica
        findViewById<Button>(R.id.btnBiometric).setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Error de autenticación: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Aquí podrías implementar lógica para autenticar automáticamente
                    Toast.makeText(applicationContext, "Autenticación biométrica exitosa", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación biométrica")
            .setSubtitle("Inicia sesión usando tu huella digital")
            .setNegativeButtonText("Usar contraseña")
            .build()
    }
}