package com.jeiu.puzzlevault.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeiu.puzzlevault.R
import com.jeiu.puzzlevault.databinding.FragmentLevelEditorBinding
import com.jeiu.puzzlevault.model.LevelModel
import com.jeiu.puzzlevault.model.PuzzleItem
import com.jeiu.puzzlevault.viewmodel.GameViewModel

class LevelEditorFragment : Fragment() {

    private var _binding: FragmentLevelEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameViewModel by viewModels({ requireActivity() })

    private val itemsAdapter = PuzzleItemAdapter { item ->
        // Remove item on delete tap
        itemsAdapter.removeItem(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = itemsAdapter

        binding.btnAddItem.setOnClickListener {
            val type = binding.etItemType.text.toString().trim()
            val xGrid = binding.etXGrid.text.toString().trim()
            val yGrid = binding.etYGrid.text.toString().trim()

            if (type.isEmpty() || xGrid.isEmpty() || yGrid.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all item fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val item = PuzzleItem(
                id = "item_${System.currentTimeMillis()}",
                type = type,
                xGrid = xGrid.toIntOrNull() ?: 0,
                yGrid = yGrid.toIntOrNull() ?: 0
            )
            itemsAdapter.addItem(item)
            binding.etItemType.text?.clear()
            binding.etXGrid.text?.clear()
            binding.etYGrid.text?.clear()
        }

        binding.btnSave.setOnClickListener {
            val levelId = binding.etLevelId.text.toString().trim()
            val title = binding.etLevelTitle.text.toString().trim()
            val difficulty = binding.spDifficulty.selectedItem?.toString() ?: "Medium"

            if (levelId.isEmpty() || title.isEmpty()) {
                Toast.makeText(requireContext(), "Level ID and Title are required", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (itemsAdapter.currentItems.isEmpty()) {
                Toast.makeText(requireContext(), "Add at least one puzzle item", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val level = LevelModel(
                levelId = levelId,
                title = title,
                difficulty = difficulty,
                items = itemsAdapter.currentItems
            )
            viewModel.saveLevel(level)
            Toast.makeText(requireContext(), "Level '$levelId' saved!", Toast.LENGTH_SHORT).show()
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