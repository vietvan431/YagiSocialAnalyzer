package com.yagi.socialanalyzer.infrastructure.security;

import com.yagi.socialanalyzer.domain.exceptions.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

/**
 * Encrypted credential store using AES-256-GCM encryption.
 */
public class EncryptedCredentialStore {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptedCredentialStore.class);
    private static final String CREDENTIALS_FILE = "data/.credentials";
    private static final String KEY_FILE = "data/.key";
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    private final Path credentialsPath;
    private final Path keyPath;
    private final Map<String, String> credentials;
    private SecretKey secretKey;
    
    /**
     * Create credential store with default paths.
     */
    public EncryptedCredentialStore() {
        this(CREDENTIALS_FILE, KEY_FILE);
    }
    
    /**
     * Create credential store with custom paths.
     */
    public EncryptedCredentialStore(String credentialsFile, String keyFile) {
        this.credentialsPath = Paths.get(credentialsFile);
        this.keyPath = Paths.get(keyFile);
        this.credentials = new HashMap<>();
        
        try {
            initializeKey();
            loadCredentials();
        } catch (SecurityException e) {
            logger.warn("Failed to initialize credential store: {}", e.getMessage());
        }
    }
    
    /**
     * Store a credential.
     */
    public void store(String key, String value) throws SecurityException {
        Objects.requireNonNull(key, "Credential key cannot be null");
        Objects.requireNonNull(value, "Credential value cannot be null");
        
        credentials.put(key, value);
        saveCredentials();
        logger.debug("Stored credential: {}", key);
    }
    
    /**
     * Retrieve a credential.
     */
    public String retrieve(String key) {
        return credentials.get(key);
    }
    
    /**
     * Delete a credential.
     */
    public void delete(String key) throws SecurityException {
        if (credentials.remove(key) != null) {
            saveCredentials();
            logger.debug("Deleted credential: {}", key);
        }
    }
    
    /**
     * List all credential keys.
     */
    public Set<String> listKeys() {
        return new HashSet<>(credentials.keySet());
    }
    
    /**
     * Clear all credentials.
     */
    public void clear() throws SecurityException {
        credentials.clear();
        saveCredentials();
        logger.info("Cleared all credentials");
    }
    
    /**
     * Initialize or load encryption key.
     */
    private void initializeKey() throws SecurityException {
        try {
            if (Files.exists(keyPath)) {
                // Load existing key
                byte[] keyBytes = Files.readAllBytes(keyPath);
                secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
                logger.debug("Loaded encryption key from {}", keyPath);
            } else {
                // Generate new key
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(KEY_SIZE);
                secretKey = keyGen.generateKey();
                
                // Save key
                ensureDirectory(keyPath.getParent());
                Files.write(keyPath, secretKey.getEncoded());
                logger.info("Generated new encryption key: {}", keyPath);
            }
        } catch (Exception e) {
            throw new SecurityException("Failed to initialize encryption key: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load credentials from encrypted file.
     */
    private void loadCredentials() throws SecurityException {
        if (!Files.exists(credentialsPath)) {
            logger.debug("No credentials file found, starting with empty store");
            return;
        }
        
        try {
            byte[] encryptedData = Files.readAllBytes(credentialsPath);
            byte[] decryptedData = decrypt(encryptedData);
            
            // Deserialize credentials
            try (ByteArrayInputStream bis = new ByteArrayInputStream(decryptedData);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                @SuppressWarnings("unchecked")
                Map<String, String> loaded = (Map<String, String>) ois.readObject();
                credentials.putAll(loaded);
                logger.info("Loaded {} credentials", credentials.size());
            }
            
        } catch (Exception e) {
            throw new SecurityException("Failed to load credentials: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save credentials to encrypted file.
     */
    private void saveCredentials() throws SecurityException {
        try {
            // Serialize credentials
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(credentials);
            }
            
            byte[] data = bos.toByteArray();
            byte[] encryptedData = encrypt(data);
            
            // Write to file
            ensureDirectory(credentialsPath.getParent());
            Files.write(credentialsPath, encryptedData);
            logger.debug("Saved {} credentials", credentials.size());
            
        } catch (Exception e) {
            throw new SecurityException("Failed to save credentials: " + e.getMessage(), e);
        }
    }
    
    /**
     * Encrypt data using AES-GCM.
     */
    private byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        
        byte[] ciphertext = cipher.doFinal(data);
        
        // Combine IV + ciphertext
        byte[] encrypted = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, encrypted, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, encrypted, GCM_IV_LENGTH, ciphertext.length);
        
        return encrypted;
    }
    
    /**
     * Decrypt data using AES-GCM.
     */
    private byte[] decrypt(byte[] encryptedData) throws Exception {
        // Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        
        // Extract ciphertext
        byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
        
        // Decrypt
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        
        return cipher.doFinal(ciphertext);
    }
    
    /**
     * Ensure directory exists.
     */
    private void ensureDirectory(Path dir) throws IOException {
        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}
