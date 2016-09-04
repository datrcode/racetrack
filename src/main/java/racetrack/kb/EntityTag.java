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
package racetrack.kb;

import java.util.StringTokenizer;
import java.util.UUID;

import racetrack.framework.BundlesDT;
import racetrack.gui.RT;
import racetrack.util.Utils;

/**
 * Class for tagging entities within the main class.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class EntityTag {
  /**
   * Unique ID for this entity-tag pairing
   */
  UUID    uuid;

  /**
   * Entity tagged
   */
  String  entity, 

  /**
   * Tag to apply
   */
          tag, 

  /**
   * Source of the entity tag
   */
	  source;

  /**
   * First heard for this tag-entity pairing
   */
  long    t0, 

  /**
   * Last heard for this tag-entity pairing
   */
          t1, 

  /**
   * Creation time for this tag-entity pairing
   */
	  create_time;

  /**
   * Indicates that the tag loaded successfully from the file.
   */
  boolean valid_flag   = true, // Indicates parsed correctly

  /**
   * Indicates that the tag has been updated and needs to be stored in the
   * local database.
   */
          updated_flag = true; // Indicates needs to stored in local db

  /**
   * Construct a new entity tag with automatic creation of source, create time, UUID.
   *
   * @param entity entity to tag
   * @param tag    tag to apply to the entity
   * @param t0     first heard for the tag of that entity
   * @param t1     last heard or the tag of that entity
   */
  public EntityTag(String entity, String tag, long t0, long t1) {
    this.entity       = entity;
    this.tag          = tag;
    if (t0 > t1) { long tmp = t0; t0 = t1; t1 = tmp; } // Swap so that earlier is t0
    this.t0           = t0;
    this.t1           = t1;
    this.source       = RT.getUserName();
    this.create_time  = System.currentTimeMillis();
    if (t0 == t1) this.uuid = Utils.getUUID(t0      ^ Double.doubleToLongBits(Math.random()), create_time);
    else          this.uuid = Utils.getUUID(t0 ^ t1 ^ Double.doubleToLongBits(Math.random()), create_time);
    this.updated_flag = true;
  }

  /**
   * Construct a new entity tag with specified fields for everything.
   *
   * @param entity   entity to tag
   * @param uuid_str uuid as a string
   * @param tag      tag to apply to the entity
   * @param t0       first heard for the tag of that entity
   * @param t1       last heard or the tag of that entity
   * @param source   source of tag
   * @param tscreate creation time of tag
   */
  public EntityTag(String uuid_str, String entity, String tag, long t0, long t1, String source, long tscreate) {
    this.entity       = entity;
    this.tag          = tag;
    if (t0 > t1) { long tmp = t0; t0 = t1; t1 = tmp; }
    this.t0           = t0;
    this.t1           = t1;
    this.source       = source;
    this.create_time  = tscreate;
    this.uuid         = UUID.fromString(uuid_str);
    this.updated_flag = false;
  }

  /**
   * Get the entity associated with this tag.
   *
   * @return associated entity
   */
  public String  getEntity()        { return entity;      }

  /**
   * Get the initial timestamp for this tag.
   *
   * @return initial timestamp
   */
  public long    ts0()              { return t0;          }

  /**
   * Set the initial timestamp.  Ensure the updated flag is set.
   *
   *@param t new timestamp 0
   */
  public void ts0(long t) { t0 = t; updated_flag = true; }

  /**
   * Set the final timestamp.  Ensure the updated flag is set.
   *
   *@param t new timestamp 1
   */
  public void ts1(long t) { t1 = t; updated_flag = true; }

  /**
   * Get the last timestamp for this tag.
   *
   * @return last timestamp
   */
  public long    ts1()              { return t1;          }

  /**
   * Get the creation time for this tag/entity pairing.
   *
   * @return creation time
   */
  public long    getCreateTime()    { return create_time; }

  /**
   * Get the actual tag.
   *
   * @return tag
   */
  public String  getTag()           { return tag;         }

  /**
   * Get the unique ID for this tag-entity pairing.
   *
   * @return unique ID
   */
  public UUID    getUUID()          { return uuid;        }

  /**
   * Return if this take was parsed in a valid format.
   *
   * @return validity flag
   */
  public boolean valid()            { return valid_flag;  }

  /**
   * Get the source for the tag.
   *
   * @return tag's source
   */
  public String  getSource()        { return source;      }

  /**
   * Static representations of "forever"...  not really forever for pre-1970 datasets...
   */
  public static final long t0_forever = Utils.parseTimeStamp("1970-01-01 00:00:00Z"),
                           t1_forever = Utils.parseTimeStamp("9999-01-01 00:00:00Z");

  /**
   * Set the tag timestamps to forever so that it applies forever.
   */
  public void    setToForever()     { t0 = t0_forever; t1 = t1_forever; updated_flag = true; }

  /**
   * Determine if the tag lasts forever.
   *
   *@return true if the tag is forever
   */
  public boolean isForever() { return (t0 == t0_forever) && (t1 == t1_forever); }

  /**
   * has the tag been updated?
   *
   * @return true if the tag has been updated and needs to be stored
   */
  public boolean isUpdated()        { return updated_flag;  }

  /**
   * Clear the updated flag after the tag has been written to the local db.
   */
  public void    clearUpdatedFlag() { updated_flag = false; }

  /**
   * Return the file header if this tag is to be written to a csv file
   *
   * @return file header line
   */
  public static String getFileHeader() { return "uuid,entity," + BundlesDT.TAGS + ",beg,end,create,source"; }

  /**
   * Return the entity tag as a csv-file line.
   *
   * @return entity tag as a file line
   */
  public        String asFileLine()    { return uuid + "," + Utils.encToURL(entity) + "," + Utils.encToURL(tag) + "," + Utils.exactDate(t0) + "," + Utils.exactDate(t1) + "," + Utils.exactDate(create_time) + "," + Utils.encToURL(getSource()); }

  /**
   * Parse the line and create the entity tag from the tokens on the line.
   *
   * @param line tag as it appears on the file line
   */
  public               EntityTag(String line) {
    StringTokenizer st = new StringTokenizer(line, ",");
    try {
      uuid         = UUID.fromString(st.nextToken());
      entity       = Utils.decFmURL(st.nextToken());
      tag          = Utils.decFmURL(st.nextToken());
      t0           = Utils.parseTimeStamp(st.nextToken());
      t1           = Utils.parseTimeStamp(st.nextToken());
      create_time  = Utils.parseTimeStamp(st.nextToken());
      source       = Utils.decFmURL(st.nextToken());
      updated_flag = true; // If it came from a file, it may not be in the local db...
    } catch (Throwable t) {
      System.err.println("Malformed Entity Tag Line \"" + line + "\""); 
      valid_flag = false;
    }
  }
}

