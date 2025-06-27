package com.example.medipaws

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.medipaws.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher

class PetRegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_registration)

        val inputName = findViewById<EditText>(R.id.inputPetName)
        val radioDog = findViewById<RadioButton>(R.id.radioDog)
        val radioCat = findViewById<RadioButton>(R.id.radioCat)
        val inputBreed = findViewById<EditText>(R.id.inputPetBreed)
        val inputAge = findViewById<EditText>(R.id.inputPetAge)
        val imagePet = findViewById<ImageView>(R.id.imagePet)
        val buttonSave = findViewById<Button>(R.id.buttonSavePet)

        // Set default image based on species
        val updateImage = {
            if (radioDog.isChecked) {
                imagePet.setImageResource(R.drawable.ic_dog_default)
            } else {
                imagePet.setImageResource(R.drawable.ic_cat_default)
            }
        }
        radioDog.setOnCheckedChangeListener { _, isChecked -> if (isChecked) updateImage() }
        radioCat.setOnCheckedChangeListener { _, isChecked -> if (isChecked) updateImage() }
        updateImage()

        // Enable save only if name and species are filled
        val updateSaveEnabled = {
            buttonSave.isEnabled = inputName.text.isNotBlank() && (radioDog.isChecked || radioCat.isChecked)
        }
        inputName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSaveEnabled()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        radioDog.setOnCheckedChangeListener { _, _ -> updateSaveEnabled() }
        radioCat.setOnCheckedChangeListener { _, _ -> updateSaveEnabled() }
        updateSaveEnabled()

        buttonSave.setOnClickListener {
            val name = inputName.text.toString().trim()
            val species = if (radioDog.isChecked) "Dog" else "Cat"
            val breed = inputBreed.text.toString().trim().ifEmpty { null }
            val age = inputAge.text.toString().toIntOrNull()
            val photoUri = null // For now, no photo picker
            val pet = Pet(name = name, species = species, breed = breed, age = age, photoUri = photoUri)
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(applicationContext).petDao().insert(pet)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
} 