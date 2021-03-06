package com.amadeus.session;

import static com.amadeus.session.SessionConfiguration.DEFAULT_SESSION_ID_LENGTH;
import static com.amadeus.session.SessionConfiguration.SESSION_ID_LENGTH;

import java.security.SecureRandom;

/**
 * Generates id consisting of random character strings of a given length in bytes.
 * However, id's containing Luhn code (credit cards) are avoided, so every 6 
 * character next character can't be digit
 *
 * Characters in string are one of following:
 * <code>ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_</code>
 * 
 */
public class RandomIdNoLuhnProvider implements SessionIdProvider {

  private static final int FILLER_CHARACTER_INDEX = 63;

  private static final int MASK_6_BITS = 0x3F;

  private static final int DIVIDE_BY_64 = 6;

  private static final int MULTIPLY_BY_256 = 8;

  private static final int BYTES_IN_BLOCK = 3;

  private static final int DEFAULT_ID_LENGTH = 30;

  private static final int CHARACTERS_IN_BLOCK = 4;

  private static final char[] SESSION_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();
  private static final char[] SESSION_ID_ALPHABET_2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzNUMBRnumbr-_"
      .toCharArray();


  private final SecureRandom random = new SecureRandom();

  private int length;

  /**
   * Default id length is 30 bytes.
   */
  public RandomIdNoLuhnProvider() {
    this(DEFAULT_ID_LENGTH);
  }

  /**
   * Creates provider that generates id string encoding length bytes.
   *
   * @param length
   */
  RandomIdNoLuhnProvider(int length) {
    this.length = length;
  }

  /**
   * Encode the bytes into a String with a slightly modified Base64-algorithm
   * This code was written by Kevin Kelley <kelley@ruralnet.net> and adapted by
   * Thomas Peuss <jboss@peuss.de>
   *
   * @param data
   *          The bytes you want to encode
   * @return the encoded String
   */
  private char[] encode(byte[] data) {
    char[] out = new char[getLengthInCharacters(data.length)];
    //
    // 3 bytes encode to 4 chars. Output is always an even
    // multiple of 4 characters.
    //
    for (int i = 0, index = 0; i < data.length; i++, index += CHARACTERS_IN_BLOCK) {
      boolean quad = false;
      boolean trip = false;

      int val = byteValue(data[i]);
      val <<= MULTIPLY_BY_256;
      i++; // NOSONAR each loop is actually i+3, and we increment counter inside loop
      if (i < data.length) {
        val |= byteValue(data[i]);
        trip = true;
      }
      val <<= MULTIPLY_BY_256;
      i++; // NOSONAR each loop is actually i+3, and we increment counter inside loop
      if (i < data.length) {
        val |= byteValue(data[i]);
        quad = true;
      }
      out[index + 3] = SESSION_ID_ALPHABET[(quad ? (val & MASK_6_BITS) : FILLER_CHARACTER_INDEX)]; // NOSONAR 3 is not magic!
      val >>= DIVIDE_BY_64;
      out[index + 2] = SESSION_ID_ALPHABET[(trip ? (val & MASK_6_BITS) : FILLER_CHARACTER_INDEX)]; // NOSONAR 2 is not magic!
      val >>= DIVIDE_BY_64;
      out[index + 1] = SESSION_ID_ALPHABET[val & MASK_6_BITS];
      val >>= DIVIDE_BY_64;
      //
      // evry odd step use only alpha characters for last digit, so any number sequence will be at most 7 characters
      //
      if ((i & 0x01) != 0) { // NOSONAR we check last bit
        out[index] = SESSION_ID_ALPHABET[val & MASK_6_BITS];
      } else {
        out[index] = SESSION_ID_ALPHABET_2[val & MASK_6_BITS];
      }
    }
    return out;
  }

  private static int byteValue(byte data) {
    return 0xFF & data;
  }

  @Override
  public String newId() {
    final byte[] bytes = new byte[length];
    random.nextBytes(bytes);
    return new String(encode(bytes));
  }

  @Override
  public String readId(String value) {
    if (value == null) {
      return null;
    }
    String trimmedValue = value.trim();
    if (trimmedValue.isEmpty()) {
      return null;
    }
    if (trimmedValue.length() != getLengthInCharacters()) {
      return null;
    }
    return trimmedValue;
  }

  @Override
  public void configure(SessionConfiguration configuration) {
    String sessionIdLength = configuration.getAttribute(SESSION_ID_LENGTH, DEFAULT_SESSION_ID_LENGTH);
    length = Integer.parseInt(sessionIdLength);
  }

  int getLengthInCharacters() {
    return getLengthInCharacters(length);
  }

  static int getLengthInCharacters(int len) {
    return ((len + (BYTES_IN_BLOCK - 1)) / BYTES_IN_BLOCK) * CHARACTERS_IN_BLOCK;
  }
}
