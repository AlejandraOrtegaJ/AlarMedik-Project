package com.example.medicinereminder.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.medicinereminder.data.entities.Medication
import com.example.medicinereminder.data.entities.User
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "meds.db"
        private const val DATABASE_VERSION = 2 // Incrementado para forzar actualización

        // Tabla de usuarios
        private const val TABLE_USERS = "users"
        private const val U_ID = "id"
        private const val U_NAME = "name"
        private const val U_EMAIL = "email"
        private const val U_PASSWORD = "password"

        // Tabla de medicamentos
        private const val TABLE_MED = "medications"
        private const val M_ID = "id"
        private const val M_NAME = "name"
        private const val M_DOSE = "dose"
        private const val M_FREQUENCY = "frequency"
        private const val M_DURATION = "duration"
        private const val M_TIME = "time"
        private const val M_USER = "user_id"
        private const val M_START_DATE = "start_date"
        private const val M_END_DATE = "end_date"

        // Tabla de estado de medicamentos
        private const val TABLE_MEDICATION_STATUS = "medication_status"
        private const val MS_ID = "id"
        private const val MS_MED_ID = "medication_id"
        private const val MS_DATE = "date"
        private const val MS_TAKEN = "taken"
        private const val MS_TAKEN_TIME = "taken_time"
        private const val MS_DOSE_NUMBER = "dose_number"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            // Crear tabla de usuarios
            val createUsers = """CREATE TABLE $TABLE_USERS (
                $U_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $U_NAME TEXT NOT NULL,
                $U_EMAIL TEXT UNIQUE NOT NULL,
                $U_PASSWORD TEXT NOT NULL
            )"""
            db.execSQL(createUsers)

            // Crear tabla de medicamentos (actualizada)
            val createMed = """CREATE TABLE $TABLE_MED (
                $M_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $M_NAME TEXT NOT NULL,
                $M_DOSE TEXT,
                $M_FREQUENCY TEXT NOT NULL,
                $M_DURATION TEXT,
                $M_TIME TEXT NOT NULL,
                $M_USER INTEGER NOT NULL,
                $M_START_DATE TEXT,
                $M_END_DATE TEXT,
                FOREIGN KEY ($M_USER) REFERENCES $TABLE_USERS($U_ID) ON DELETE CASCADE
            )"""
            db.execSQL(createMed)

            // Crear tabla de estado de medicamentos
            val createMedicationStatus = """CREATE TABLE $TABLE_MEDICATION_STATUS (
                $MS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $MS_MED_ID INTEGER NOT NULL,
                $MS_DATE TEXT NOT NULL,
                $MS_TAKEN INTEGER DEFAULT 0,
                $MS_TAKEN_TIME TEXT,
                $MS_DOSE_NUMBER INTEGER DEFAULT 1,
                FOREIGN KEY ($MS_MED_ID) REFERENCES $TABLE_MED($M_ID) ON DELETE CASCADE
            )"""
            db.execSQL(createMedicationStatus)

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error creating tables", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            // Eliminar tablas antiguas y recrear
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICATION_STATUS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MED")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error upgrading database", e)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    // CRUD de usuarios
    fun addUser(user: User): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(U_NAME, user.name)
            put(U_EMAIL, user.email)
            put(U_PASSWORD, user.password)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun findUserByEmail(email: String): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$U_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )

        return try {
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(U_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(U_NAME)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(U_EMAIL)),
                    password = cursor.getString(cursor.getColumnIndexOrThrow(U_PASSWORD))
                )
            } else null
        } finally {
            cursor.close()
        }
    }

    fun findUserById(id: Long): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$U_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        return try {
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(U_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(U_NAME)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(U_EMAIL)),
                    password = cursor.getString(cursor.getColumnIndexOrThrow(U_PASSWORD))
                )
            } else null
        } finally {
            cursor.close()
        }
    }

    // CRUD de medicamentos
    fun addMedication(medication: Medication): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(M_NAME, medication.name)
            put(M_DOSE, medication.dose)
            put(M_FREQUENCY, medication.frequency)
            put(M_DURATION, medication.duration)
            put(M_TIME, medication.time)
            put(M_USER, medication.userId)
            put(M_START_DATE, medication.startDate)
            put(M_END_DATE, medication.endDate)
        }
        return db.insert(TABLE_MED, null, values)
    }

    fun getMedicationsForUser(userId: Long): List<Medication> {
        val medications = mutableListOf<Medication>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_MED,
            null,
            "$M_USER = ?",
            arrayOf(userId.toString()),
            null, null, "$M_TIME ASC"
        )

        try {
            while (cursor.moveToNext()) {
                val medication = Medication(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(M_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(M_NAME)),
                    dose = cursor.getString(cursor.getColumnIndexOrThrow(M_DOSE)),
                    frequency = cursor.getString(cursor.getColumnIndexOrThrow(M_FREQUENCY)),
                    duration = cursor.getString(cursor.getColumnIndexOrThrow(M_DURATION)),
                    time = cursor.getString(cursor.getColumnIndexOrThrow(M_TIME)),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow(M_USER)),
                    startDate = cursor.getString(cursor.getColumnIndexOrThrow(M_START_DATE)),
                    endDate = cursor.getString(cursor.getColumnIndexOrThrow(M_END_DATE))
                )
                medications.add(medication)
            }
        } finally {
            cursor.close()
        }
        return medications
    }

    fun getMedicationsForDate(userId: Long, date: String): List<Medication> {
        val medications = mutableListOf<Medication>()
        val db = this.readableDatabase
        
        // Query simplificado para obtener medicamentos del usuario
        val query = """
            SELECT m.*, 
                   CASE WHEN ms.$MS_TAKEN = 1 THEN 1 ELSE 0 END as is_taken,
                   ms.$MS_TAKEN_TIME as taken_time
            FROM $TABLE_MED m
            LEFT JOIN $TABLE_MEDICATION_STATUS ms ON m.$M_ID = ms.$MS_MED_ID 
                AND ms.$MS_DATE = ? AND ms.$MS_DOSE_NUMBER = 1
            WHERE m.$M_USER = ?
            ORDER BY m.$M_TIME
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf(date, userId.toString()))
        
        try {
            while (cursor.moveToNext()) {
                val medication = Medication(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(M_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(M_NAME)),
                    dose = cursor.getString(cursor.getColumnIndexOrThrow(M_DOSE)),
                    frequency = cursor.getString(cursor.getColumnIndexOrThrow(M_FREQUENCY)),
                    duration = cursor.getString(cursor.getColumnIndexOrThrow(M_DURATION)),
                    time = cursor.getString(cursor.getColumnIndexOrThrow(M_TIME)),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow(M_USER)),
                    startDate = cursor.getString(cursor.getColumnIndexOrThrow(M_START_DATE)),
                    endDate = cursor.getString(cursor.getColumnIndexOrThrow(M_END_DATE))
                )
                medications.add(medication)
            }
        } finally {
            cursor.close()
        }
        
        return medications
    }

    fun markMedicationAsTaken(medicationId: Long, date: String, doseNumber: Int = 1): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(MS_MED_ID, medicationId)
            put(MS_DATE, date)
            put(MS_TAKEN, 1)
            put(MS_TAKEN_TIME, SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
            put(MS_DOSE_NUMBER, doseNumber)
        }
        
        return try {
            db.insertWithOnConflict(TABLE_MEDICATION_STATUS, null, values, SQLiteDatabase.CONFLICT_REPLACE) > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error marking medication as taken", e)
            false
        }
    }

    fun isMedicationTaken(medicationId: Long, date: String, doseNumber: Int = 1): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_MEDICATION_STATUS,
            arrayOf(MS_TAKEN),
            "$MS_MED_ID = ? AND $MS_DATE = ? AND $MS_DOSE_NUMBER = ?",
            arrayOf(medicationId.toString(), date, doseNumber.toString()),
            null, null, null
        )
        
        return try {
            cursor.moveToFirst() && cursor.getInt(0) == 1
        } finally {
            cursor.close()
        }
    }

    fun getAdherenceRate(userId: Long, date: String): Double {
        val db = this.readableDatabase
        
        // Obtener total de medicamentos del usuario
        val totalQuery = "SELECT COUNT(*) FROM $TABLE_MED WHERE $M_USER = ?"
        val totalCursor = db.rawQuery(totalQuery, arrayOf(userId.toString()))
        val total = if (totalCursor.moveToFirst()) totalCursor.getInt(0) else 0
        totalCursor.close()
        
        if (total == 0) return 0.0
        
        // Obtener medicamentos tomados para la fecha
        val takenQuery = """
            SELECT COUNT(*) FROM $TABLE_MEDICATION_STATUS ms
            INNER JOIN $TABLE_MED m ON ms.$MS_MED_ID = m.$M_ID
            WHERE m.$M_USER = ? AND ms.$MS_DATE = ? AND ms.$MS_TAKEN = 1
        """.trimIndent()
        
        val takenCursor = db.rawQuery(takenQuery, arrayOf(userId.toString(), date))
        val taken = if (takenCursor.moveToFirst()) takenCursor.getInt(0) else 0
        takenCursor.close()
        
        return if (total > 0) (taken.toDouble() / total.toDouble()) * 100.0 else 0.0
    }

    fun getAdherenceRate(userId: Long): Double {
        return try {
            val db = readableDatabase
            val query = """
                SELECT 
                    (SELECT COUNT(*) FROM $TABLE_MEDICATION_STATUS ms 
                     JOIN $TABLE_MED m ON ms.$MS_MED_ID = m.$M_ID 
                     WHERE m.$M_USER = ? AND ms.$MS_TAKEN = 1) * 100.0 / 
                    NULLIF((SELECT COUNT(*) FROM $TABLE_MED m 
                     WHERE m.$M_USER = ?), 0)
            """
            val cursor = db.rawQuery(query, arrayOf(userId.toString(), userId.toString()))

            var rate = 0.0
            if (cursor.moveToFirst()) {
                rate = cursor.getDouble(0)
                if (rate.isNaN()) rate = 0.0
            }
            cursor.close()
            rate
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error calculating adherence rate", e)
            0.0
        }
    }
}