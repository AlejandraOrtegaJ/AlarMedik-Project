package com.example.medicinereminder.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicinereminder.R
import com.example.medicinereminder.data.DatabaseHelper

class HistoryActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        db = DatabaseHelper(this)

        val userId = intent.getLongExtra("userId", 0)
        val lv = findViewById<ListView>(R.id.lvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvAdherence = findViewById<TextView>(R.id.tvAdherence)

        // Obtener medicamentos del usuario
        val meds = db.getMedicationsForUser(userId)

        if (meds.isEmpty()) {
            tvEmpty.text = "Aún no hay historial"
            tvAdherence.text = "Adherencia: 0%"
        } else {
            tvEmpty.text = ""

            // Crear lista de medicamentos para mostrar
            val medList = meds.map {
                "${it.name} - ${it.dose ?: "Sin dosis"} - ${it.time} - ${it.frequency}"
            }

            // SOLUCIÓN: Adapter con texto negro
            val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, medList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setTextColor(Color.BLACK) // Texto negro
                    return view
                }
            }
            lv.adapter = adapter

            // Calcular y mostrar porcentaje de adherencia
            val adherenceRate = db.getAdherenceRate(userId)
            tvAdherence.text = "Adherencia: ${"%.2f".format(adherenceRate)}%"
        }

        // Configurar clics en la lista
        lv.setOnItemClickListener { parent, view, position, id ->
            val med = meds[position]
            // Aquí podrías implementar edición o eliminación
        }
    }
}