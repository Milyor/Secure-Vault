"""
Encryption and decryption utilities for secure file storage.
Uses AES-256 encryption in GCM mode for authenticated encryption.
"""

import os
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


class CryptoManager:
    """Manages encryption and decryption of files using AES-256-GCM."""
    
    def __init__(self, master_key: bytes = None):
        """
        Initialize the crypto manager with a master key.
        
        Args:
            master_key: The master encryption key (32 bytes for AES-256).
                       If not provided, generates a new one.
        """
        if master_key is None:
            master_key = AESGCM.generate_key(bit_length=256)
        elif len(master_key) != 32:
            raise ValueError("Master key must be 32 bytes for AES-256")
        
        self.master_key = master_key
        self.aesgcm = AESGCM(self.master_key)
    
    def encrypt_file(self, plaintext: bytes) -> bytes:
        """
        Encrypt file data using AES-256-GCM.
        
        Args:
            plaintext: The file data to encrypt
            
        Returns:
            Encrypted data with nonce prepended (nonce + ciphertext)
        """
        # Generate a random nonce (12 bytes for GCM)
        nonce = os.urandom(12)
        
        # Encrypt the data
        ciphertext = self.aesgcm.encrypt(nonce, plaintext, None)
        
        # Return nonce + ciphertext
        return nonce + ciphertext
    
    def decrypt_file(self, encrypted_data: bytes) -> bytes:
        """
        Decrypt file data using AES-256-GCM.
        
        Args:
            encrypted_data: The encrypted data (nonce + ciphertext)
            
        Returns:
            Decrypted file data
        """
        # Extract nonce (first 12 bytes) and ciphertext
        nonce = encrypted_data[:12]
        ciphertext = encrypted_data[12:]
        
        # Decrypt the data
        plaintext = self.aesgcm.decrypt(nonce, ciphertext, None)
        
        return plaintext
