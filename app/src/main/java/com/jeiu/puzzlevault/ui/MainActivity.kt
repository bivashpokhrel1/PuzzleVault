package com.jeiu.puzzlevault.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jeiu.puzzlevault.R
import com.jeiu.puzzlevault.databinding.ActivityMainBinding
import com.jeiu.puzzlevault.ui.challenge.DailyChallengeFragment
import com.jeiu.puzzlevault.ui.editor.LevelEditorFragment
import com.jeiu.puzzlevault.ui.home.HomeFragment
import com.jeiu.puzzlevault.ui.room.RoomFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // ── Main menu card buttons ────────────────────────────────────────

        binding.cardDailyChallenge.setOnClickListener {
            loadFragment(DailyChallengeFragment())
        }

        binding.cardLevelEditor.setOnClickListener {
            loadFragment(LevelEditorFragment())
        }

        binding.cardVaultRoom.setOnClickListener {
            loadFragment(RoomFragment())
        }

        // ── Bottom nav ────────────────────────────────────────────────────

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> loadFragment(HomeFragment())
                R.id.navRoom -> loadFragment(RoomFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}