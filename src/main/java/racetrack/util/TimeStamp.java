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

/**
 * Implementation of a {@link SubText} for a timestamp.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class TimeStamp    extends SubText {
  /**
   * Timestamp value
   */
  long ts;

  /**
   * Construct the TimeStamp with the specified timestamp string
   * and indices within the  overall text.
   *
   *@param timestamp_str timestamp string
   *@param i0            index of start of string in text
   *@param i1            index of end of string in text
   */
  public TimeStamp(String full, String timestamp_str, int i0, int i1) { 
    super(full, timestamp_str, i0, i1); 
    ts = Utils.parseTimeStamp(toString());
  }

  /**
   * Return the type of subtext
   *
   *@return subtext type, i.e., TIMESTAMP
   */
  @Override
  public String getType()      { return "TIMESTAMP"; }

  /**
   * Return the timestamp value as a long milliseconds since the epoch.
   *
   *@return timestamp in millis
   */
  public long   getTimeStamp() { return ts; }
}

