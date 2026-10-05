package com.jeiu.puzzlevault.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jeiu.puzzlevault.R
import com.jeiu.puzzlevault.databinding.FragmentHomeBinding
import com.jeiu.puzzlevault.ui.challenge.DailyChallengeFragment
import com.jeiu.puzzlevault.ui.editor.LevelEditorFragment
import com.jeiu.puzzlevault.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isNotEmpty()) {
                viewModel.login(username)
            }
        }

        binding.btnCreateRoom.setOnClickListener { viewModel.createRoom() }
        binding.btnDailyChallenge.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DailyChallengeFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnLevelEditor.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LevelEditorFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnPuzzle.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PuzzleFragment())
                .addToBackStack(null)
                .commit()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.tvStatus.text = when {
                    state.isLoading -> "Loading..."
                    state.isLoggedIn -> "Logged in: ${state.username}"
                    state.errorMessage != null -> "Error: ${state.errorMessage}"
                    else -> "Welcome to PuzzleVault"
                }

                state.currentRoomCode?.let { code ->
                    binding.tvRoomInfo.text = "Room: $code (${state.roomPlayerCount} players)"
                    binding.tvRoomInfo.visibility = View.VISIBLE
                }

                state.errorMessage?.let { msg ->
                    if (msg.isNotEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
