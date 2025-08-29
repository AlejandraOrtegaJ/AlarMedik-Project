package com.example.medicinereminder.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.medicinereminder.R
import java.text.SimpleDateFormat
import java.util.*

// Componente de calendario personalizado para mostrar medicamentos por fecha
class CalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private lateinit var tvMonthYear: TextView
    private lateinit var gridDays: GridLayout
    private lateinit var tvPrevMonth: TextView
    private lateinit var tvNextMonth: TextView
    
    private var currentDate = Calendar.getInstance()
    private var selectedDate = Calendar.getInstance()
    private var onDateSelectedListener: ((Calendar) -> Unit)? = null
    private var medicationDates: Map<String, Int> = emptyMap() // fecha -> cantidad de medicamentos

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_calendar, this, true)
        
        tvMonthYear = findViewById(R.id.tvMonthYear)
        gridDays = findViewById(R.id.gridDays)
        tvPrevMonth = findViewById(R.id.tvPrevMonth)
        tvNextMonth = findViewById(R.id.tvNextMonth)

        tvPrevMonth.setOnClickListener { 
            currentDate.add(Calendar.MONTH, -1)
            updateCalendar()
        }
        
        tvNextMonth.setOnClickListener { 
            currentDate.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        updateCalendar()
    }

    private fun updateCalendar() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es"))
        tvMonthYear.text = monthFormat.format(currentDate.time)
        
        gridDays.removeAllViews()
        
        // Agregar días de la semana
        val daysOfWeek = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        daysOfWeek.forEach { day ->
            val dayHeader = TextView(context).apply {
                text = day
                textSize = 12f
                setTextColor(context.getColor(R.color.text_secondary))
                setPadding(8, 8, 8, 8)
            }
            gridDays.addView(dayHeader)
        }

        // Configurar calendario para el primer día del mes
        val firstDay = currentDate.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)
        
        // Ajustar para mostrar desde el domingo de la semana que contiene el primer día
        val dayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK)
        firstDay.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1))

        // Generar días del calendario
        for (i in 0..41) { // 6 semanas x 7 días
            val dayDate = firstDay.clone() as Calendar
            dayDate.add(Calendar.DAY_OF_MONTH, i)
            
            val dayView = createDayView(dayDate)
            gridDays.addView(dayView)
        }
    }

    private fun createDayView(date: Calendar): TextView {
        val dayView = TextView(context).apply {
            text = date.get(Calendar.DAY_OF_MONTH).toString()
            textSize = 14f
            setPadding(8, 8, 8, 8)
            gravity = android.view.Gravity.CENTER
            
            // Verificar si es el mes actual
            if (date.get(Calendar.MONTH) != currentDate.get(Calendar.MONTH)) {
                setTextColor(context.getColor(R.color.text_secondary))
                alpha = 0.5f
            } else {
                setTextColor(context.getColor(R.color.text_primary))
            }
            
            // Verificar si es la fecha seleccionada
            if (isSameDay(date, selectedDate)) {
                setBackgroundColor(context.getColor(R.color.accent))
                setTextColor(context.getColor(android.R.color.white))
            }
            
            // Verificar si hay medicamentos en esta fecha
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
            val medicationCount = medicationDates[dateKey] ?: 0
            if (medicationCount > 0) {
                // Agregar indicador de medicamentos
                setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_medication_indicator, 0)
                compoundDrawablePadding = 4
            }
            
            setOnClickListener {
                selectedDate = date.clone() as Calendar
                onDateSelectedListener?.invoke(selectedDate)
                updateCalendar() // Refrescar para mostrar la selección
            }
        }
        
        return dayView
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun setOnDateSelectedListener(listener: (Calendar) -> Unit) {
        onDateSelectedListener = listener
    }

    fun setMedicationDates(dates: Map<String, Int>) {
        medicationDates = dates
        updateCalendar()
    }

    fun getSelectedDate(): Calendar = selectedDate
}
