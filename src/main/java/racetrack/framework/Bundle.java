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

package racetrack.framework;

import java.io.PrintStream;

/**
 * Base class for encapsulating a row from a CSV/database/spreadsheet value.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public abstract class Bundle { 
  /**
   * Return the string within the specified field index of this bundle.
   *
   * @param  fld_i field index
   * @return       corresponding string
   */
  public abstract String toString(int fld_i);

  /**
   * Return the value of this field.
   *
   * @param  fld_i field index
   * @return       corresponding integer value
   */
  public abstract int toValue(int fld_i);

  /**
   * Return the begin timestamp for this record.
   *
   * @return begin timestamp
   */
  public long    ts0()                 { return 0L;     } 

  /**
   * Indicates if this record has a timestamp.
   *
   *@return false unless subclassed
   */
  public boolean hasTime() { return false; }
  
  /**
   * Set the begin timestamp for this record. Limited to just this package for access.
   *
   * @param new_ts0 new begin timestamp
   */
  void setTS0(long new_ts0) { throw new RuntimeException("No Begin Time In Basic Bundle"); }

  /**
   * Return the ending timestamp for this record.
   *
   * @return end timestamp
   */
  public long    ts1()                 { return 0L;     } 

  /**
   * Indicates if this record has a duration.
   *
   *@return false unless subclassed
   */
  public boolean hasDuration() { return false; }
  
  /**
   * Set the end timestamp for this record. Limited to just this package for access.
   *
   * @param new_ts1 new end timestamp
   */
  void setTS1(long new_ts1) { throw new RuntimeException("No End Time In Basic Bundle");   }

  /**
   * Get the {@link Tablet} for this bundle.  Useful when you only have a reference to the bundle
   * but need to know about the table or other structured data.
   *
   * @return bundle's tablet
   */
  public abstract Tablet getTablet();

  /**
   * Save the bundle to a printstream so that it can be re-parsed.
   *
   * @param out print stream to save the bundle to
   */
  protected abstract void save(PrintStream out);
}
