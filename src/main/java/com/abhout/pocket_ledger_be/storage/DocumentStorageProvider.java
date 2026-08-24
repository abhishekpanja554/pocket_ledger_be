package com.abhout.pocket_ledger_be.storage;

public interface DocumentStorageProvider {
    void put(String key, byte[] content, String type);
    byte[] get(String key);
    void delete(String key);
}
