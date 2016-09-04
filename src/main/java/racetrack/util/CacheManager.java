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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Centralizes the caches across the application so that they can be cleared
 * more reliably.
 *
 *@author  D Trimm
 *@version 0.1
 */
public class CacheManager {
  /**
   * List of registered caches
   */
  private static Map<String,Map> caches = new HashMap<String,Map>();

  /**
   * Register a cache with the cache manager.  Once registered, clear operations will 
   * apply to the specified cache.
   *
   *@param description unique string describing cache
   *@param cache       cache to register
   */
  public static void registerCache(String description, Map cache) {
    if (caches.containsKey(description)) throw new RuntimeException("Cache \"" + description + "\" Already Registered");
    else caches.put(description, cache);
  }

  /**
   * De-register a cache from the cache manager.  Once de-registered, clear operations
   * will no alonger apply to the specified cache.
   *
   *@param description cache to deregister
   */
  public static void deRegisterCache(String description) {
    if (caches.containsKey(description)) caches.remove(description);
    else throw new RuntimeException("Cache \"" + description + "\" Not Registered With Cache Manager");
  }

  /**
   * Clear all the caches.
   */
  public static void clearCaches() {
    // System.err.println("CacheManager.clearCaches()...  statistics:");
    // printCacheStatistics(System.err);
    Iterator<String> it = caches.keySet().iterator();
    while (it.hasNext()) caches.get(it.next()).clear();
  }

  /**
   * Print out statistics about the caches.  Most importantly, print the memory footprint of each cache.
   *
   *@param out printstream for printing statistics to
   */
  public static void printCacheStatistics(PrintStream out) {
    out.println("**\n** CacheManager Statistics\n**");
    out.println("** Total Caches: " + caches.keySet().size());
    Iterator<String> it = caches.keySet().iterator();
    while (it.hasNext()) out.println(" - " + it.next());
  }
}

