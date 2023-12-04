package com.example.potionsapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.potionsapi.model.Potion;
import java.util.UUID;

public interface PotionRepository extends MongoRepository < Potion, UUID > {


}