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
import java.util.UUID;

/**
 * Interface for a BloomFilter implementation.  THis one is relatively
 * specific to UUIDs as a method to quickly calculate if a source
 * has the object related to a UUID.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public interface BloomFilter { 
  /**
   * Add a UUID to this bloomfilter.
   *
   * @param uuid uuid to add to the bloom filter
   */
  public void    add(UUID uuid);

  /**
   * Determine if the specified uuid matches the bit pattern in this
   * bloomfilter.
   *
   * @param uuid uuid used to determine if appropriate bit(s) are
   *             set in the bloom filter
   * @return     boolean indicating appropriate bit(s) set
   */
  public boolean contains(UUID uuid); 

  /**
   * Writes the bloom filter data to a file.  Random access file used
   * so that the bloom filter could manipulate the file at random
   * locations (i.e., not sequentially).
   *
   * @param  raf         random access to write the bloomfilter to
   * @throws IOException possible exception thrown for IO methods
   */
  public void    writeToFile(RandomAccessFile raf) throws IOException; 
}
