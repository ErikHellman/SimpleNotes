package se.hellsoft.simplenotes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import se.hellsoft.simplenotes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
    }
}