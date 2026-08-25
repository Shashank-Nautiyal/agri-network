package com.nullpointer.agri_backend.service;

import com.nullpointer.agri_backend.model.Farmer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PHASE 1: in-memory store.
 * PHASE 2: swap for Firestore (see FirestoreConfig) - keep this same
 *          method signature so controllers don't need to change.
 */
@Service
public class FarmerService {

    private final Map<String, Farmer> farmers = new ConcurrentHashMap<>();

    public Farmer create(Farmer farmer) {
        String id = UUID.randomUUID().toString();
        farmer.setId(id);
        farmers.put(id, farmer);
        return farmer;
    }

    public Optional<Farmer> findById(String id) {
        return Optional.ofNullable(farmers.get(id));
    }
}
