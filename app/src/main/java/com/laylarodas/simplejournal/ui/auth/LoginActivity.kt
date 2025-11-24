package com.laylarodas.simplejournal.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.laylarodas.simplejournal.databinding.ActivityLoginBinding

/**
 * Pantalla de login/registro. De momento solo prepara la UI y ViewBinding,
 * luego conectaremos los botones con el AuthViewModel.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

