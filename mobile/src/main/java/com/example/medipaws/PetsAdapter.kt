package com.example.medipaws

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PetsAdapter(
    private var pets: List<Pet>,
    private val onPetClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetsAdapter.PetViewHolder>() {

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petIcon: ImageView = itemView.findViewById(R.id.petIcon)
        val petName: TextView = itemView.findViewById(R.id.petName)
        val petDetails: TextView = itemView.findViewById(R.id.petDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pet_item, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        holder.petName.text = pet.name
        
        // Set pet details
        val details = buildString {
            append(pet.species ?: "Unknown Species")
            pet.breed?.let { append(" - $it") }
            pet.age?.let { append(" - $it years old") }
        }
        holder.petDetails.text = details

        // Set pet icon based on species
        holder.petIcon.setImageResource(
            when (pet.species) {
                "Dog" -> R.drawable.ic_dog_default
                "Cat" -> R.drawable.ic_cat_default
                else -> R.drawable.ic_pets
            }
        )

        holder.itemView.setOnClickListener {
            onPetClick(pet)
        }
    }

    override fun getItemCount(): Int = pets.size

    fun updatePets(newPets: List<Pet>) {
        pets = newPets
        notifyDataSetChanged()
    }
} 