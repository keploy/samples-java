package com.example.potionsapi.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.potionsapi.exception.ResourceNotFoundException;
import com.example.potionsapi.model.Potion;
import com.example.potionsapi.repository.PotionRepository;

@Service
@Transactional
public class PotionServiceImpl implements PotionService {

    @Autowired
    private PotionRepository potionRepository;

    @Override
    public Potion createPotion(Potion potion) {
        return potionRepository.save(potion);
    }

    @Override
    public Potion updatePotion(UUID id, Potion potion) {
        Potion existingPotion = potionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        existingPotion.setName(potion.getName());
        existingPotion.setDescription(potion.getDescription());
        existingPotion.setBottle(potion.getBottle());
        existingPotion.setQuantity(potion.getQuantity());

        return potionRepository.save(existingPotion);
    }

    @Override
    public List<Potion> getAllPotion() {
        return potionRepository.findAll();
    }

    @Override
    public Potion getPotionById(UUID potionId) {
        return potionRepository.findById(potionId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + potionId));
    }

    // Optional: Uncomment and implement only if needed
    /*
    @Override
    public Potion getPotionByName(String potionName) {
        return potionRepository.findByName(potionName)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with name: " + potionName));
    }
    */

    @Override
    public void deletePotion(UUID potionId) {
        Potion potion = potionRepository.findById(potionId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + potionId));
        potionRepository.delete(potion);
    }
}
