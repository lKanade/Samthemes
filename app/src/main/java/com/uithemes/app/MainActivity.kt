package com.uithemes.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val SHIZUKU_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnActivar = findViewById<Button>(R.id.btnActivar)
        val btnDesactivar = findViewById<Button>(R.id.btnDesactivar)
        val btnReiniciarUI = findViewById<Button>(R.id.btnReiniciarUI)

        verificarPermisoShizuku()

        btnActivar.setOnClickListener {
            val cmd = "settings put secure theme_customization_overlay_packages '{\"android.theme.customization.icon_pack.systemui\":\"com.uithemes.app\"}' && am crash com.android.systemui"
            ejecutarComandoShizuku(cmd)
            Toast.makeText(this, "Aplicando tema...", Toast.LENGTH_SHORT).show()
        }

        btnDesactivar.setOnClickListener {
            val cmd = "settings put secure theme_customization_overlay_packages '{}' && am crash com.android.systemui"
            ejecutarComandoShizuku(cmd)
            Toast.makeText(this, "Desactivando tema...", Toast.LENGTH_SHORT).show()
        }

        btnReiniciarUI.setOnClickListener {
            ejecutarComandoShizuku("am crash com.android.systemui")
            Toast.makeText(this, "Reiniciando interfaz...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarPermisoShizuku() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_CODE)
            }
        } else {
            Toast.makeText(this, "Shizuku no está iniciado", Toast.LENGTH_LONG).show()
        }
    }

    private fun ejecutarComandoShizuku(comando: String) {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Asegúrate de que Shizuku esté activo", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Shizuku.newProcess(arrayOf("sh", "-c", comando), null, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
