package com.jeiu.puzzlevault.ui.room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jeiu.puzzlevault.R
import com.jeiu.puzzlevault.databinding.FragmentRoomBinding
import com.jeiu.puzzlevault.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoomFragment : Fragment() {

    private var _binding: FragmentRoomBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateRoom.setOnClickListener { viewModel.createRoom() }

        binding.btnJoinRoom.setOnClickListener {
            val code = binding.etRoomCode.text.toString().trim()
            if (code.isNotEmpty()) {
                viewModel.joinRoom(code)
            }
        }

        binding.btnSyncPuzzle.setOnClickListener { viewModel.syncPuzzle() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.tvRoomStatus.text = state.currentRoomCode?.let {
                    "Room: $it — ${state.roomPlayerCount} players"
                } ?: "No active room"

                binding.tvPuzzleInfo.text = state.currentPuzzleId?.let {
                    "Puzzle: $it — Attempts: ${state.attempts}/${state.maxAttempts}"
                } ?: "No puzzle synced"

                if (state.isPuzzleSolved) {
                    binding.tvSolved.visibility = View.VISIBLE
                    binding.btnSyncPuzzle.isEnabled = true
                } else {
                    binding.tvSolved.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
