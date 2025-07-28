package com.telus.io.dto;

/**
 * Result of a vector store synchronization operation.
 */
public class SyncResult {
    
    private int duplicatesRemoved;
    private int missingAdded;
    private int orphansRemoved;
    
    public SyncResult(int duplicatesRemoved, int missingAdded, int orphansRemoved) {
        this.duplicatesRemoved = duplicatesRemoved;
        this.missingAdded = missingAdded;
        this.orphansRemoved = orphansRemoved;
    }
    
    public int getDuplicatesRemoved() {
        return duplicatesRemoved;
    }
    
    public int getMissingAdded() {
        return missingAdded;
    }
    
    public int getOrphansRemoved() {
        return orphansRemoved;
    }
    
    @Override
    public String toString() {
        return "SyncResult{" +
                "duplicatesRemoved=" + duplicatesRemoved +
                ", missingAdded=" + missingAdded +
                ", orphansRemoved=" + orphansRemoved +
                '}';
    }
}
