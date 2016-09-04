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
package racetrack.gui;

import java.util.StringTokenizer;
import java.util.UUID;

import racetrack.util.Utils;

/**
 * Class representing a time marker.  These markers are created by the
 * user within the visualizations related to time.  They can then be
 * saved to local files or the database storage.
 *
 * Version 1.1 adds comparable interface
 *
 *@author  D. Trimm
 *@version 1.1
 */
public class TimeMarker implements Comparable<TimeMarker> {
  /**
   * Unique ID for the time marker.  Used to update the correct
   * entry within the local database.
   */
  UUID    uuid;
  /**
   * Description of event at the time marker.
   */
  String  description, 
  /**
   * Source of the time marker.  Usually the user...
   */
          source;
  /**
   * Beginning of time marker
   */
  long    t0, 
  /**
   * End (if set) of the time marker.  Same value as beginning means just
   * a timestamp.
   */
          t1, 
  /**
   * Creation time of the marker
   */
	  create_time;
  /**
   * Flag to indicate that the timemarker was parsed successfully from a file line.
   */
  boolean valid_flag   = true, 
  /**
   * Flag to indicate to update the marker in the local database.
   */
          updated_flag = true; // Does marker need to be saved to local db?

  /**
   * Construct a time marker from the specified parameters.
   *
   *@param desc      description of the event
   *@param time_at_0 begin time
   *@param time_at_1 end time.  If set to begin time, then marker is a timestamp
   */
  public TimeMarker(String desc, long time_at_0, long time_at_1) {
    this.description  = desc;
    this.t0           = time_at_0;
    this.t1           = time_at_1;
    this.create_time  = System.currentTimeMillis();
    this.source       = RT.getUserName();
    if (t0 == t1) this.uuid = Utils.getUUID(t0 ^ Double.doubleToLongBits(Math.random()),    create_time);
    else          this.uuid = Utils.getUUID(t0 ^ t1 ^ (t0 << 32) ^ (t1 << 31) ^ (t0 << 49), create_time);
    this.updated_flag = true;
  }

  /**
   * Construct a time marker that is just a timestamp.
   *
   *@param desc      description of the event
   *@param timestamp timestamp
   */
  public TimeMarker(String desc, long timestamp) { this(desc, timestamp, timestamp); }

  /**
   * Construct a time maker with all of the parameters.  Useful for the database pull.
   *
   *@param uuid_str UUID as a string
   *@param desc      description of the event
   *@param time_at_0 begin time
   *@param time_at_1 end time.  If set to begin time, then marker is a timestamp
   *@param createts creation time
   */
  public TimeMarker(String uuid_str, String desc, long ts0, long ts1, String source, long createts) { 
    this.uuid         = UUID.fromString(uuid_str);
    this.description  = desc;
    this.t0           = ts0;
    this.t1           = ts1;
    this.source       = source;
    this.create_time  = createts;
    this.updated_flag = false;
  }

  /**
   *
   */
  public int compareTo(TimeMarker other) {
    if      (t0 < other.t0) return -1;
    else if (t0 > other.t0) return  1;
    else                    return description.compareTo(other.description);
  }

  /**
   * Return the event description.
   *
   *@return event description
   */
  public String   getDescription()           { return description; }

  /**
   * Set the event description.
   *
   *@param str event description
   */
  public void     setDescription(String str) { description = str; updated_flag = true; }

  /**
   * Return the beginning timestamp
   *
   *@return begin timestamp
   */
  public long     ts0()              { return t0;            }

  /**
   * Return the ending timestamp
   *
   *@return end timestamp
   */
  public long     ts1()              { return t1;            }

  /**
   * Return the creation time of this time marker
   *
   *@return creation time
   */
  public long     getCreateTime()    { return create_time;   }

  /**
   * Return true if the time marker is just a timestamp.
   *
   *@return true if begin and end time are the same
   */
  public boolean  isTimeStamp()      { return t0 == t1;      }

  /**
   * Return the unique ID for this time marker.
   *
   *@return unique id
   */
  public UUID     getUUID()          { return uuid;          }

  /**
   * Return true if the time marker is valid.  Only applies to time markers
   * parsed from file lines.
   *
   *@return false if file line not parsed correctly, true otherwise
   */
  public boolean  valid()            { return valid_flag;    }

  /**
   * Return the source for this time marker.  Usually just the username.
   *
   *@return source of time marker
   */
  public String   getSource()        { return source;        }

  /**
   * Return true if the time marker has been updated.  Signifies that the
   * time marker needs to be re-stored in the local database.
   *
   *@return true if the marker needs to be re-saved (or saved) to the
   *        local database.
   */
  public boolean  isUpdated()        { return updated_flag;  }

  /**
   * Clear the update flag.  For instance, when it has just been saved
   * to the local database.
   */
  public void     clearUpdatedFlag() { updated_flag = false; }

  /**
   * Return the file header for storing time markers as CSV events.
   *
   *@return file header
   */
  public static String getFileHeader() { return "uuid,description,beg,end,create,source"; }

  /**
   * Return the time marker as a single line in a file.  Encode the parameters
   * so that they can be parsed directly.
   *
   *@return string representing this time marker
   */
  public        String asFileLine()    { return uuid + "," + Utils.encToURL(description) + "," + Utils.exactDate(t0) + "," + Utils.exactDate(t1) + "," + Utils.exactDate(create_time) + "," + Utils.encToURL(source); }

  /**
   * Construct a time marker from a file line.
   *
   *@param line string representing time marker
   */
  public               TimeMarker(String line) {
    StringTokenizer st = new StringTokenizer(line, ",");
    try {
      uuid         = UUID.fromString(st.nextToken());
      description  = Utils.decFmURL(st.nextToken());
      t0           = Utils.parseTimeStamp(st.nextToken());
      t1           = Utils.parseTimeStamp(st.nextToken());
      create_time  = Utils.parseTimeStamp(st.nextToken());
      source       = Utils.decFmURL(st.nextToken());
      updated_flag = true; // If it came from a file, it may not be in local db
    } catch (Throwable t) {
      System.err.println("Malformed Time Marker Line \"" + line + "\""); 
      valid_flag = false;
    }
  }
}

