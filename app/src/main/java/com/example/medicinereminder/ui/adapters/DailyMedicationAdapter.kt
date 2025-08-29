package com.example.medicinereminder.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.R
import com.example.medicinereminder.data.entities.Medication
import com.example.medicinereminder.data.entities.Dose

// Adaptador para mostrar medicamentos del día seleccionado
class DailyMedicationAdapter(
    private var medications: List<Medication> = emptyList(),
    private val onMedicationClick: (Medication) -> Unit
) : RecyclerView.Adapter<DailyMedicationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMedicationName: TextView = view.findViewById(R.id.tvMedicationName)
        val tvDose: TextView = view.findViewById(R.id.tvDose)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val cardView: View = view.findViewById(R.id.cardMedication)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_medication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medication = medications[position]
        
        holder.tvMedicationName.text = medication.name
        holder.tvDose.text = medication.dose ?: "Sin dosis especificada"
        holder.tvTime.text = medication.time
        
        // Determinar estado del medicamento
        val status = getMedicationStatus(medication)
        holder.tvStatus.text = status.first
        holder.tvStatus.setTextColor(holder.itemView.context.getColor(status.second))
        
        // Cambiar el color de fondo según el estado
        if (status.first == "Tomado") {
            holder.cardView.setBackgroundColor(holder.itemView.context.getColor(R.color.success_light))
        } else {
            holder.cardView.setBackgroundColor(holder.itemView.context.getColor(R.color.card))
        }
        
        holder.cardView.setOnClickListener {
            onMedicationClick(medication)
        }
    }

    override fun getItemCount() = medications.size

    fun updateMedications(newMedications: List<Medication>) {
        medications = newMedications
        notifyDataSetChanged()
    }

    private fun getMedicationStatus(medication: Medication): Pair<String, Int> {
        // Aquí implementaremos la lógica para verificar si el medicamento fue tomado
        // Por ahora retornamos "Pendiente" - esto se actualizará en HomeActivity
        return Pair("Pendiente", R.color.accent)
    }
}
