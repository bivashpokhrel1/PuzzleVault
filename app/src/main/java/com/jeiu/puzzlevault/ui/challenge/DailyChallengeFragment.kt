package com.jeiu.puzzlevault.ui.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jeiu.puzzlevault.R
import com.jeiu.puzzlevault.databinding.FragmentDailyChallengeBinding
import com.jeiu.puzzlevault.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DailyChallengeFragment : Fragment() {

    private var _binding: FragmentDailyChallengeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadDailyChallenges()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailyChallenges.collectLatest { challenges ->
                val display = challenges.joinToString("\n") { puzzle ->
                    val time = viewModel.getAdjustedTime(puzzle, 0)
                    "${puzzle.type}: ${time}s (seed: ${puzzle.seedValue})"
                }
                binding.tvChallenges.text = "Today's Challenges:\n\n$display"
            }
        }

        binding.btnRefresh.setOnClickListener { viewModel.loadDailyChallenges() }
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
