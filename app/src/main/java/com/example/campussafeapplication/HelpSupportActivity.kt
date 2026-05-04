package com.example.campussafeapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HelpSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSend = findViewById<TextView>(R.id.btnSend)
        val etSubject = findViewById<EditText>(R.id.etSubject)
        val etMessage = findViewById<EditText>(R.id.etMessage)

        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            val subject = etSubject.text.toString()
            val message = etMessage.text.toString()

            if (subject.isNotEmpty() && message.isNotEmpty()) {
                sendEmail(subject, message)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmail(subject: String, message: String) {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("it.maintenance@sti.edu.ph"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}
