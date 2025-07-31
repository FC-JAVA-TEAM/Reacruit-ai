package com.telus.io.util;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for entity comparison operations.
 * Provides methods to compare collections and determine if entities have changed.
 */
public class EntityComparisonUtils {
    
    /**
     * Checks if two collections are equal.
     * 
     * @param collection1 The first collection
     * @param collection2 The second collection
     * @return true if the collections are equal, false otherwise
     */
    public static boolean areCollectionsEqual(List<String> collection1, List<String> collection2) {
        if (collection1 == null && collection2 == null) {
            return true;
        }
        
        if (collection1 == null || collection2 == null) {
            return false;
        }
        
        if (collection1.size() != collection2.size()) {
            return false;
        }
        
        Set<String> set1 = new HashSet<>(collection1);
        Set<String> set2 = new HashSet<>(collection2);
        
        return set1.equals(set2);
    }
    
    /**
     * Handles changes to collections by adding/removing items instead of replacing the entire collection.
     * 
     * @param existingCollection The existing collection from the database
     * @param updatedCollection The updated collection from the client
     */
    public static void handleCollectionChanges(List<String> existingCollection, List<String> updatedCollection) {
        if (existingCollection == null || updatedCollection == null) {
            return;
        }
        
        // Create sets for easier comparison
        Set<String> existingSet = new HashSet<>(existingCollection);
        Set<String> updatedSet = new HashSet<>(updatedCollection);
        
        // Find items to remove (in existing but not in updated)
        Set<String> toRemove = new HashSet<>(existingSet);
        toRemove.removeAll(updatedSet);
        
        // Find items to add (in updated but not in existing)
        Set<String> toAdd = new HashSet<>(updatedSet);
        toAdd.removeAll(existingSet);
        
        // Remove items
        existingCollection.removeIf(toRemove::contains);
        
        // Add items
        existingCollection.addAll(toAdd);
    }
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private EntityComparisonUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }
}
