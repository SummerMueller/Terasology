// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.engine.identity;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Objects;

/**
 * A private certificate contains the key that only the certificate owner should know. Used for signing and decryption
 */
public class PrivateIdentityCertificate {
    private BigInteger modulus;   // The product of two large prime numbers
    private BigInteger exponent;   // Used to encrypt/decrypt data

    public PrivateIdentityCertificate(BigInteger modulus, BigInteger exponent) {
        this.modulus = modulus;
        this.exponent = exponent;
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public BigInteger getExponent() {
        return exponent;
    }

    /**
     * Produces a signature for data that can be verified as by the paired public certificate.
     *
     * @param dataToSign
     * @return The signature
     */
    public byte[] sign(byte[] dataToSign) {
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, exponent);

        Signature signer = null;
        try {
            // Creates a signature object using the algorithm defined in IdentityConstants
            signer = Signature.getInstance(IdentityConstants.SIGNATURE_ALGORITHM);
            KeyFactory keyFactory = KeyFactory.getInstance(IdentityConstants.CERTIFICATE_ALGORITHM);
            // Generates the private key
            PrivateKey key = keyFactory.generatePrivate(keySpec);
            // Creates the signature object and signs with the key
            signer.initSign(key, new SecureRandom());
            signer.update(dataToSign);
            return signer.sign();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException("Unexpected exception during signing", e);
        }
    }

    /**
     * Decrypts data encrypted by the paired public certificate
     *
     * @param data
     * @return The decrypted data
     * @throws BadEncryptedDataException If the data could not be decrypted due to an error with the data.
     */
    public byte[] decrypt(byte[] data) throws BadEncryptedDataException {
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, exponent);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(IdentityConstants.CERTIFICATE_ALGORITHM);
            PrivateKey key = keyFactory.generatePrivate(keySpec);
            Cipher cipher = Cipher.getInstance(IdentityConstants.CERTIFICATE_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Insufficient support for '" + IdentityConstants.CERTIFICATE_ALGORITHM
                    + "', required for identity management", e);
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            throw new RuntimeException("Unexpected error during encryption", e);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new BadEncryptedDataException("Invalid encrypted data", e);
        }
    }

    // Compares two private identity certificates
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PrivateIdentityCertificate) {
            PrivateIdentityCertificate other = (PrivateIdentityCertificate) obj;
            return Objects.equals(modulus, other.modulus) && Objects.equals(exponent, other.exponent);
        }
        return false;
    }

    // Generates a hash code based on the modulus and exponent
    @Override
    public int hashCode() {
        return Objects.hash(modulus.hashCode(), exponent.hashCode());
    }
}
