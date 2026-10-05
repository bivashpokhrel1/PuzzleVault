package com.jeiu.puzzlevault.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.jeiu.puzzlevault.R
import com.jeiu.puzzlevault.databinding.FragmentPuzzleBinding

class PuzzleFragment : Fragment() {

    private var _binding: FragmentPuzzleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPuzzleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Wire Circuit
        binding.wireCircuitView.setSolution(
            listOf("A1" to "C3", "B2" to "D4", "A3" to "C1")
        )
        binding.wireCircuitView.setOnPuzzleSolved {
            Toast.makeText(requireContext(), "Circuit complete!", Toast.LENGTH_SHORT).show()
        }

        binding.btnWireReset.setOnClickListener {
            binding.wireCircuitView.reset()
        }

        // Keypad
        binding.keypadPuzzleView.setTargetSequence("4829")
        binding.keypadPuzzleView.setOnPuzzleSolved {
            Toast.makeText(requireContext(), "Code entered!", Toast.LENGTH_SHORT).show()
        }

        binding.btnKeypadReset.setOnClickListener {
            binding.keypadPuzzleView.reset()
        }

        // Grid Slide
        binding.gridSlideView.setOnPuzzleSolved {
            val moves = binding.gridSlideView.getMoveCount()
            Toast.makeText(requireContext(), "Solved in $moves moves!", Toast.LENGTH_SHORT).show()
        }

        binding.btnGridReset.setOnClickListener {
            binding.gridSlideView.reset()
        }

        // Rotatable Wire Circuit (3x3)
        binding.rotatableWireView.setOnPuzzleSolved {
            Toast.makeText(requireContext(), "Circuit complete!", Toast.LENGTH_SHORT).show()
        }
        binding.btnRotatableReset.setOnClickListener {
            binding.rotatableWireView.reset()
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
