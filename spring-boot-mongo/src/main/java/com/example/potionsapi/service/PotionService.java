package com.example.potionsapi.service;

import java.util.List;
import java.util.UUID;

import com.example.potionsapi.model.Potion;

public interface PotionService {

    Potion createPotion( Potion potion );

    Potion updatePotion( UUID id, Potion potion );

    List<Potion> getAllPotion();

    Potion getPotionById( UUID potionId );

//    Potion getPotionByName( String potion );

    void deletePotion( UUID id );
}