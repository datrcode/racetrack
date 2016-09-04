/* 

Copyright 2013 David Trimm

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package racetrack.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.UUID;


/**
 * Class implementing multiple stages of bloomfilters.  Multiple stages
 * use different size bit vectors in attempts to strenghthen the match
 * of the hash value.
 *
 * @author  D. Trimm
 * @version 1.0
 */
class BloomFilterMultiple implements BloomFilter {
  /**
   * Multiple bloom filter is composed of an array of simple bloom filters.
   */
  BloomFilterSimple stages[];

  /**
   * Constructor used to specify which sizes bitmaps shouuld be used for
   * multiple stages.  Initially, all of the bitmaps will be empty.
   *
   * @param sizes array representing the number (length) and sizes
   *              of each simple bloom filter.
   */
  public BloomFilterMultiple(int sizes[]) {
    stages = new BloomFilterSimple[sizes.length];
    for (int i=0;i<sizes.length;i++) stages[i] = new BloomFilterSimple(sizes[i]);
  }

  /**
   * Constructor used to load a multi-stage bloom filter from a previously
   * create file.
   *
   * @param raf random access file where the multi-stage bloom filter is stored.
   */
  public BloomFilterMultiple(RandomAccessFile raf) throws IOException {
    stages = new BloomFilterSimple[raf.readInt()];
    for (int i=0;i<stages.length;i++) stages[i] = new BloomFilterSimple(raf);
  }

  public void    add     (UUID uuid) { for (int i=0;i<stages.length;i++) stages[i].add(uuid); }

  public boolean contains(UUID uuid) {
    for (int i=0;i<stages.length;i++) if (stages[i].contains(uuid) == false) return false;
    return true;
  }

  public void    writeToFile(RandomAccessFile raf) throws IOException {
    raf.writeInt(stages.length);
    for (int i=0;i<stages.length;i++) stages[i].writeToFile(raf);
  }
}

/**
 * Simple implementation of the bloom filter interface.  Uses a byte array that
 * is sub-indexed by bit value.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class BloomFilterSimple implements BloomFilter {
  /**
   * Number of bits in the bloom filter
   */
  protected int    num_of_bits;

  /**
   * Byte array used to hold the individual bits.  Will be sub-indexed using
   * modulo and divide operations.
   */
  protected byte[] bit_array;

  /**
   * Constructor used for createing a bloom filter based on the number
   * of needed bits.  The bloom filter itself will be empty upon
   * creation.
   *
   * @param num_of_bits number of bits to use in the bloom filter
   */
  public BloomFilterSimple(int num_of_bits) { 
    this.num_of_bits = num_of_bits;
    this.bit_array   = new byte[(num_of_bits/8) + (((num_of_bits%8) != 0) ? 1 : 0)];
  }

  /**
   * Constructor used for creating a pre-initialized bloom filter.
   *
   * @param num_of_bits size of the bloom filter in bits
   * @param init        initial values for the bloom filter
   */
  public BloomFilterSimple(int num_of_bits, byte init[]) {
    this.num_of_bits = num_of_bits; 
    this.bit_array   = init;
  }

  /**
   * Constructore used to create a bloom filter by loading it from
   * a file.
   *
   * @param raf random access file containing the bloom filter
   */
  public BloomFilterSimple(RandomAccessFile raf) throws IOException {
    num_of_bits = raf.readInt();
    bit_array   = new byte[(num_of_bits/8) + (((num_of_bits%8) != 0) ? 1 : 0)];
    raf.read(bit_array);
  }

  public void writeToFile(RandomAccessFile raf) throws IOException {
    raf.writeInt(num_of_bits);
    raf.write(bit_array);
  }

  /**
   * Calculate the bit index for the specified UUID.  This method makes
   * use of the {@link BigInteger} class to compute the modulo function.
   *
   * @param  uuid UUID to calculate the bit index for
   * @return      The specific bit index based on the bloom filter size 
   *              for this uuid
   */
  public int bitIndex(UUID uuid) {
    long hi = uuid.getMostSignificantBits(), lo = uuid.getLeastSignificantBits();
    byte as_array[] = new byte[17];
    for (int i=0;i<8;i++) { as_array[1  +i] = (byte) (hi>>(8*(7-i))); as_array[1+8+i] = (byte) (lo>>(8*(7-i))); }
    BigInteger bi    = new BigInteger(as_array);
    return (bi.mod(new BigInteger("" + num_of_bits))).intValue();
  }

  public boolean contains(UUID uuid) {
    int index = bitIndex(uuid);
    return ((bit_array[index/8] >> (index%8)) & 0x01) == 0x01;
  }

  public void    add(UUID uuid) {
    int index = bitIndex(uuid);
    bit_array[index/8] = (byte) (bit_array[index/8] | (0x01 << (index%8)));
  }

  /**
   * Get the number of bits used to create this bloom filter.
   *
   * @return number of bits from initialization
   */
  public int     getNumberOfBits() { return num_of_bits; }

  /**
   * Return the bloom filter as an array of bytes.
   *
   * @return array of bytes that represent bits in bloom filter
   */
  public byte[]  asArray()         { return bit_array;   }

  /**
   * Test routine for BloomFilter implementations.
   *
   * @param args command line arguments, not used in this method.
   */
  public static void main(String args[]) {
    int def = 65700;
    BloomFilter bf  = new BloomFilterSimple(def);
    // int stages[] = { def/3 + 1, def/3, def/3 - 1 };
    // int stages[] = { def+1, def, def-1 };
    // int stages[] = {def/4+2,def/4+1,def/4,def/4-1};
    int stages[] = { def+4,def+3,def+2,def+1,def };
    BloomFilter bf3 = new BloomFilterMultiple(stages);

    System.out.println("bf.contains(\"test\") = " + bf.contains(Utils.getUUID("test")) + " ... " + bf3.contains(Utils.getUUID("test")));
    System.out.println("adding \"test\"...");       bf.add(Utils.getUUID("test"));                 bf3.add(Utils.getUUID("test"));
    System.out.println("bf.contains(\"test\") = " + bf.contains(Utils.getUUID("test")) + " ... " + bf3.contains(Utils.getUUID("test")));

    String str = "" + Math.random() + " - " + Math.random(); int simple_collision = 2, multiple_collision = 2;
    boolean simple_correct = true, multiple_correct = true;
    while (simple_correct || multiple_correct) {
      if (simple_correct)   { if (bf.contains(Utils.getUUID(str)))  simple_correct   = false; else simple_collision++;   }
      if (multiple_correct) { if (bf3.contains(Utils.getUUID(str))) multiple_correct = false; else multiple_collision++; }
      // 
      bf.add(Utils.getUUID(str)); bf3.add(Utils.getUUID(str));
      str = "" + Math.random() + " - " + Math.random();
    }

    System.out.println("Simple Collision = " + simple_collision + " , Multiple Collision = " + multiple_collision);
  }
}


