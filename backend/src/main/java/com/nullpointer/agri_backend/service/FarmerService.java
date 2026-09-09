package com.nullpointer.agri_backend.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.nullpointer.agri_backend.model.Farmer;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * PHASE 1: in-memory store.
 * PHASE 2: swap for Firestore (see FirestoreConfig) - keep this same
 *          method signature so controllers don't need to change.
 */
@Service
public class FarmerService {

    private static final String COLLECTION = "farmers";
    private final Firestore firestore;

    public FarmerService(Firestore firestore) {
        this.firestore = firestore;
    }

    public Farmer create(Farmer farmer) {
        String id = UUID.randomUUID().toString();
        farmer.setId(id);
        try {
            firestore.collection(COLLECTION).document(id).set(farmer).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save farmer", e);
        }
        return farmer;
    }

    public Optional<Farmer> findById(String id) {

        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            if (snapshot.exists()) {
                return Optional.ofNullable(snapshot.toObject(Farmer.class));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch farmer", e);
        }
    }
}
