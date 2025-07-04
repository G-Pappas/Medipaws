package com.example.medipaws

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class PetsManagementFragment : Fragment() {
    private lateinit var petsAdapter: PetsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var addPetButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pets_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.petsRecyclerView)
        addPetButton = view.findViewById(R.id.addPetButton)

        setupRecyclerView()
        setupAddButton()
        loadPets()
    }

    private fun setupRecyclerView() {
        petsAdapter = PetsAdapter(emptyList()) { pet ->
            showEditPetDialog(pet)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = petsAdapter
        }
    }

    private fun setupAddButton() {
        addPetButton.setOnClickListener {
            showAddPetDialog()
        }
    }

    private fun loadPets() {
        lifecycleScope.launch {
            val petDao = AppDatabase.getDatabase(requireContext()).petDao()
            petDao.getAllPets().collect { pets ->
                petsAdapter.updatePets(pets)
            }
        }
    }

    private fun showAddPetDialog() {
        showPetDialog()
    }

    private fun showEditPetDialog(pet: Pet) {
        showPetDialog(pet)
    }

    private fun showPetDialog(existingPet: Pet? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.activity_pet_registration, null)

        val inputName = dialogView.findViewById<EditText>(R.id.inputPetName)
        val radioDog = dialogView.findViewById<RadioButton>(R.id.radioDog)
        val radioCat = dialogView.findViewById<RadioButton>(R.id.radioCat)
        val inputBreed = dialogView.findViewById<EditText>(R.id.inputPetBreed)
        val inputAge = dialogView.findViewById<EditText>(R.id.inputPetAge)
        val buttonSavePet = dialogView.findViewById<Button>(R.id.buttonSavePet)

        // Pre-fill if editing
        existingPet?.let { pet ->
            inputName.setText(pet.name)
            inputBreed.setText(pet.breed)
            pet.age?.let { inputAge.setText(it.toString()) }
            when (pet.species) {
                "Dog" -> radioDog.isChecked = true
                "Cat" -> radioCat.isChecked = true
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (existingPet == null) "Add New Pet" else "Edit Pet")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        if (existingPet != null) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Delete") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this pet?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            val petDao = AppDatabase.getDatabase(requireContext()).petDao()
                            petDao.delete(existingPet)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        buttonSavePet.setOnClickListener {
            val name = inputName.text.toString().trim()
            val breed = inputBreed.text.toString().trim().ifEmpty { null }
            val age = inputAge.text.toString().trim().toIntOrNull()
            val species = when {
                radioDog.isChecked -> "Dog"
                radioCat.isChecked -> "Cat"
                else -> "Unknown"
            }

            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    val petDao = AppDatabase.getDatabase(requireContext()).petDao()
                    if (existingPet == null) {
                        val newPet = Pet(
                            name = name,
                            species = species,
                            breed = breed,
                            age = age
                        )
                        petDao.insert(newPet)
                    } else {
                        val updatedPet = existingPet.copy(
                            name = name,
                            species = species,
                            breed = breed,
                            age = age
                        )
                        petDao.update(updatedPet)
                    }
                }
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a name for your pet", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
} 