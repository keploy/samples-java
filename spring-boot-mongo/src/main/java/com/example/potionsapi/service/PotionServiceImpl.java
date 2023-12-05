package com.example.potionsapi.service;

import java.sql.SQLOutput;
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
    public Potion createPotion( Potion potion ){
        return potionRepository.save(potion);
    }

    @Override
    public Potion updatePotion( UUID id, Potion potion ) {
        Optional < Potion > PotionsDB = this.potionRepository.findById(id);

        if (PotionsDB.isPresent()) {
            Potion potionUpdate = PotionsDB.get();
            potionUpdate.setId(id);
            potionUpdate.setName(potion.getName());
            potionUpdate.setDescription(potion.getDescription());
            potionUpdate.setBottle(potion.getBottle());
            potionUpdate.setQuantity(potion.getQuantity());
            return potionRepository.save(potionUpdate);
        }
        else {
            throw new ResourceNotFoundException("Record not found with id: " + potion.getId());
        }
    }

    @Override
    public List <Potion> getAllPotion() {
        return this.potionRepository.findAll();
    }

    @Override
    public Potion getPotionById(UUID potionId ) {

        Optional < Potion > PotionsDB = this.potionRepository.findById(potionId);

        if (PotionsDB.isPresent()) {
            return PotionsDB.get();
        }
        else{
            throw new ResourceNotFoundException("Record not found with id: " + potionId);
        }
    }

//    @Override
//    public Potion getPotionByName( String potionName ) {
//
//        Optional < Potion > PotionsDB = this.potionRepository.findByName(potionName);
//
//        if (PotionsDB.isPresent()) {
//            return PotionsDB.get();
//        }
//        else{
//            throw new ResourceNotFoundException("Record not found with name: " + potionName);
//        }
//    }

    @Override
    public void deletePotion( UUID potionId ) {

        Optional < Potion > PotionsDB = this.potionRepository.findById(potionId);

        if (PotionsDB.isPresent()) {
            this.potionRepository.delete(PotionsDB.get());
        }
        else {
            throw new ResourceNotFoundException("Record not found with id: " + potionId);
        }
    }
}