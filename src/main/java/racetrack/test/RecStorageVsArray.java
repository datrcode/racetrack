/* 

Copyright 2014 David Trimm

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
package racetrack.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to test difference between storing information in a class (and set-based) versus in a two dimensional array (and list-based).
 */
public class RecStorageVsArray {
  /**
   *
   */
  public static void main(String args[]) {
    try {
      if (args.length == 1 || args.length == 2 || args.length == 3) {
        if (args.length == 2 || args.length == 3) RECS = Integer.parseInt(args[1]);
        if (                    args.length == 3) COLS = Integer.parseInt(args[2]);
        new RecStorageVsArray(args);
      } else { System.err.println("Usage:  java RecStorageVsArray recs|array [reccount] [colcount]"); System.exit(0); }
    } catch (Throwable t) { System.err.println("Throwable: " + t); }
  }

  /**
   *
   */
  public RecStorageVsArray(String args[]) {
      long t0 = System.currentTimeMillis();

      // Create storage
      Storage storage = null; String type = "notset";
      if        (args.length == 0 || args[0].equals("recs"))  { storage = new RecStorage();   type = "recs";
      } else if (                    args[0].equals("array")) { storage = new ArrayStorage(); type = "array";
      } else throw new RuntimeException("type not specified...");

      long t1   = System.currentTimeMillis();
      long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Test 1
      Iterator<Rec> it = storage.recIterator(); int beg = 0; int end = 0;
      while (it.hasNext()) { Rec rec = it.next(); String str = rec.getCol(0); if (str.charAt(0) <= 'm') beg++; if (str.charAt(str.length()-1) <= 'm') end++; }

      long t2 = System.currentTimeMillis();

      // Test 2
      it = storage.recIterator(); int str0 = COLS/2, str1 = COLS-1, str2 = 3*COLS/4;
      while (it.hasNext()) { 
        Rec rec = it.next(); String str;
	str = rec.getCol(str0); if (str.charAt(0) <= 'm') beg++; if (str.charAt(str.length()-1) <= 'm') end++; 
	str = rec.getCol(str1); if (str.charAt(0) <= 'm') beg++; if (str.charAt(str.length()-1) <= 'm') end++; 
	str = rec.getCol(str2); if (str.charAt(0) <= 'm') beg++; if (str.charAt(str.length()-1) <= 'm') end++; 
      }

      long t3 = System.currentTimeMillis();
      long mem3 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Print results
      System.out.println("type,INIT,TEST1,TEST2,OVERALL,RECS,COLS,INITK,TEST2K");
      System.out.println(type + "," + (t1 - t0) + "," + (t2 - t1) + "," + (t3 - t2) + "," + (t3 - t0) + "," + RECS + "," + COLS + "," + mem1/1000 + "," + mem3/1000);
  }

  public String randomString() { StringBuffer sb = new StringBuffer(); for (int i=0;i<10;i++) { char c = (char) ('a' + (int) (Math.random() * 25)); sb.append(c); } return sb.toString(); }


  static int RECS = 1000000,
             COLS = 20;

  /**
   * Simplified storage and record interfaces
   */
  interface Storage { Iterator<Rec> recIterator();     }
  interface Rec     { String        getCol(int col_i); }

  /**
   * Implementation of record storage.
   */
  class RecStorage implements Storage {
    class StorageRec implements Rec {
      String strs[] = new String[COLS]; public StorageRec() { for (int i=0;i<strs.length;i++) strs[i] = randomString(); }
      public String getCol(int col_i) { return strs[col_i]; }
    }
    Set<Rec> recs = new HashSet<Rec>();
    public RecStorage() { for (int i=0;i<RECS;i++) recs.add(new StorageRec()); }
    public Iterator<Rec> recIterator() { return recs.iterator(); }
  }

  /**
   * Implementation of 2d array storage.
   */
  class ArrayStorage implements Storage {
    String array[][] = new String[RECS][COLS];
    class ArrayRec implements Rec { 
      int rec_i; public ArrayRec(int rec_i) { this.rec_i = rec_i; for (int i=0;i<array[rec_i].length;i++) array[rec_i][i] = randomString(); }
      public String getCol(int col_i) { return array[rec_i][col_i]; }
    }
    List<Rec> recs = new ArrayList<Rec>();
    public ArrayStorage() { for (int i=0;i<RECS;i++) recs.add(new ArrayRec(i)); }
    public Iterator<Rec> recIterator() { return recs.iterator(); }
  }
} 

