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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import racetrack.util.CacheManager;
import racetrack.util.Entity;
import racetrack.util.SubText;
import racetrack.util.Utils;

/**
 * Global values that represent lookups and field indices for the overall
 * data set.  This class is updated when new data elements are loaded but, for
 * the most part, remains static throughout the life of the applications.
 *
 * Version 1.1 - added methods to add and remove fields; add record tags.
 *
 * @author  D. Trimm
 * @version 1.1
 */
public class BundlesG {
  /**
   * Maximum number of fields for this application
   */
  static final int MAX_FIELDS = 256;

  /**
   * Current number of fields consumed
   */
  private int                                    fld_count = 0;

  /**
   * Field names for each index
   */
  private String                                 flds[]    = new String  [MAX_FIELDS];

  /**
   * Flag to indicate if the value is a scalar
   */
  private boolean                                sclr[]    = new boolean [MAX_FIELDS];

  /**
   * Lookup table to calculate the field index from a field name
   */
  private Map<String,Integer>                flds_lu   = new HashMap<String,Integer>();

  /**
   * Lookup table that converts a field index into the set of data types
   */
  private Map<Integer,Set<BundlesDT.DT>> fld_dts   = new HashMap<Integer,Set<BundlesDT.DT>>();

  /**
   * Lookup table to convert a string to a representative integer value
   */
  private Map<String,Integer>                ent_2_i   = new HashMap<String,Integer>();

  /**
   * Adds the default values prior to loading data.
   */
  public BundlesG() {
    // Put the default conversions
    addRangeIntegers();
  }

  /**
   * Add the integer range value strings to the lookup map.
   */
  private void addRangeIntegers() {
    ent_2_i.put("< 0",    -1);   ent_2_i.put("== 0",    0);    ent_2_i.put("== 1",    1);     ent_2_i.put("< 10",    10);
    ent_2_i.put("< 100",   100); ent_2_i.put("< 1K",    1000); ent_2_i.put("< 10K",   10000); ent_2_i.put("< 100K",  100000);
    ent_2_i.put("> 100K",  1000000);
  }

  /**
   * Add a new field (if not already in existence) to the specified bundles and set the value.
   *
   *@param bundles      individual records to apply the field and setting
   *@param root_bundles reference to the root of the dataset -- needed because if a tablet has to change, all the records need to be modified
   *@param fld          name of new/existing field
   *@param val          value for the field
   */
  public void setField(Bundles bundles, Bundles root_bundles, String fld, String val) {
    // Do some sanity checking on the values...  Mirrors what is done for the dialog box to create a new field
    boolean scalar        = Utils.isAllUpper(fld),
            parseable_int = false; try { Integer.parseInt(val); parseable_int = true; } catch (NumberFormatException nfe) { };
    if (fld.equals("") || fld.toLowerCase().equals("tags")
                       || fld.toLowerCase().equals("timestamp")
                       || fld.toLowerCase().equals("timestamp_end")
                       || fld.toLowerCase().equals("beg")
                       || fld.toLowerCase().equals("end")) { System.err.println("Trying To Use A Reserved Field \"" + fld + "\"..."); return; }
    if (scalar) {
      if (parseable_int == false) { System.err.println("Scalar Field \"" + fld + "\" but not a scalar valuable \"" + val + "\""); return; }
    } else      {
      if (val == null || val.equals("")) val = BundlesDT.NOTSET;
    }

    // Go through the tablets
    Iterator<Tablet> it_tab = bundles.tabletIterator();
    while (it_tab.hasNext()) {
      Tablet tablet = it_tab.next(); int fld_i = getOrCreateField(fld, Utils.isAllUpper(fld));

      // Add the field to the tablet if it doesn't already exist
      if (tablet.hasField(fld_i) == false) {
        Iterator<Tablet> it_tab_root = root_bundles.tabletIterator(); while (it_tab_root.hasNext()) {
	  Tablet root_tablet = it_tab_root.next(); 
	  // Ugly edge case... it's possible that identical tablet headers exist in two (or more separate) tablets...
	  // ... in this case, we may be creating a field in a tablet that doesn't need it...
	  if (root_tablet.fileHeader().equals(tablet.fileHeader())) root_tablet.addField(fld);
	}
      }

      // Set the values for the specified bundles
      tablet.setField(fld, val);
    }

    // Run a cleanse to make sure everything is updated properly
    Set<Bundles> as_set = new HashSet<Bundles>(); as_set.add(root_bundles); cleanse(as_set);
  }

  /**
   * Remove the specified fields from all of the tablets.
   *
   *@param bundles   should be the root bundles
   *@param to_remove fields to remove
   */
  public void removeFields(Bundles bundles, Set<String> to_remove) {
    // First, get rid of the the field from tablets
    Iterator<Tablet> it_tab = bundles.tabletIterator();
    while (it_tab.hasNext()) { it_tab.next().removeFields(to_remove); }
    // Fix up the global state
    /* // The problem with the following code is that it leaves a gap in the field names because of the array that is used to hold the field names (flds)...
       // Removing this code fixes that problem... but will continue to show the user those field names in the gui...
    Iterator<String> it_fld = to_remove.iterator(); while (it_fld.hasNext()) {
      String fld = it_fld.next(); int fld_i = flds_lu.get(fld);
      flds_lu.remove(fld);
      fld_dts.remove(fld_i);
    }
    */

    // - Run a cleanse to get rid of more info
    Set<Bundles> as_set = new HashSet<Bundles>(); as_set.add(bundles); cleanse(as_set);
  }

  /**
   * Clean up lookup tables to prevent memory leakage as data is shed.
   *
   * @param active Active set of Bundles element still in the application
   *
   */
  public void cleanse(Set<Bundles> active) {
    // Clear the ent_2_i's
    ent_2_i.clear(); post_processors = null;

    // Initialize by creating a lookup for the post processors
    Map<BundlesDT.DT, Set<PostProc>> pp_lu = new HashMap<BundlesDT.DT, Set<PostProc>>();
    String pp_strs[] = BundlesDT.listEnabledPostProcessors();
    for (int i=0;i<pp_strs.length;i++) {
      PostProc     pp = BundlesDT.createPostProcessor(pp_strs[i], this);
      BundlesDT.DT dt = pp.type();
      if (pp_lu.containsKey(dt) == false) pp_lu.put(dt, new HashSet<PostProc>());
      pp_lu.get(dt).add(pp);
    }

    // First, accumulate all of the active entities
    Set<String> active_entities = new HashSet<String>();
    Iterator<Bundles> it_bs = active.iterator();
    while (it_bs.hasNext()) {
      Bundles bundles = it_bs.next();
      Iterator<Tablet> it_tab = bundles.tabletIterator();
      while (it_tab.hasNext()) {
        Tablet tablet = it_tab.next();
        // Figure out which fields are in the tablet
	List<Integer> fld_is = new ArrayList<Integer>();
	for (int fld_i=0;fld_i<bundles.getGlobals().numberOfFields();fld_i++) {
	  if (tablet.hasField(fld_i)) fld_is.add(fld_i);
        }
	int fields[] = new int[fld_is.size()]; for (int i=0;i<fields.length;i++) fields[i] = fld_is.get(i);
	// Go through the individual bundle elements
	Iterator<Bundle> it = tablet.bundleIterator();
	while (it.hasNext()) {
	  Bundle bundle = it.next();
	  for (int i=0;i<fields.length;i++) {
            // System.err.println("cleanse:fields[" + i + "] = " + fields[i]); // abc DEBUG
	    String ent = bundle.toString(fields[i]);
	    active_entities.add(ent); 
            addFieldEntity(fields[i], ent);
	    // Add the post processor versions...
            BundlesDT.DT datatype = BundlesDT.getEntityDataType(ent);
            if (pp_lu.containsKey(datatype)) {
	      Iterator<PostProc> it_pp = pp_lu.get(datatype).iterator();
	      while (it_pp.hasNext()) {
	        String converts[] = it_pp.next().postProcess(ent);
		for (int j=0;j<converts.length;j++) {
                  active_entities.add(converts[j]);
                }
              }
	    }
	  }
	}
      }
    }
    // Re-add the range values
    addRangeIntegers();
    // Clear the caches
    CacheManager.clearCaches();
    // Lastly, clear transforms
    resetTransforms();
  }

  /**
   * Lookup table to determine how many of a certain datatype exist in the application.
   * Used to incrementally create integer lookups for entity strings.
   */
  private Map<BundlesDT.DT,Integer> dt_count_lu       = new HashMap<BundlesDT.DT,Integer>();

  /**
   * Set of entities that could not be associated with a particular datatype.  Used
   * to create integer lookups for entity strings.
   */
  private Set<String>               not_assoc         = new HashSet<String>();

  /**
   * List of post processors.  Post processors are used to convert one datatype into
   * another.  Some post processors do this by calculation/algorithm.  Others use
   * lookup tables from dataset loads.
   */
  private PostProc                      post_processors[] = null; 

  /**
   * Get or create the field index for a specified field.
   *
   * @param  field        field name
   * @param  scalar_field flag to mark the field as scalar which determines
   *                      how the files are added together during sum operations
   * @return              index of the existing, or already created, field
   */
  public synchronized int   getOrCreateField(String field, boolean scalar_field) {
    if (flds_lu.containsKey(field) == false) {
      flds[fld_count] = field; sclr[fld_count] = scalar_field; flds_lu.put(field, fld_count); fld_dts.put(fld_count,new HashSet<BundlesDT.DT>()); fld_count++;
    } return flds_lu.get(field);
  }

  /**
   * Method to force an entity to point to a specific index.  Used by models that fall outside
   * of the scope of data management -- in the first case, counting and organizing data by the
   * tablet header.
   *
   *@param entity entity to set
   *@param index  look up value for entity
   */
  protected synchronized void overrideEntityIndex(String entity, int index) { ent_2_i.put(entity,index); }

  /**
   * Add an entity to a specific field.  This creates the appropriate lookup
   * tables for fast lookup/access.
   *
   * @param fld_i  index of the field
   * @param entity string of the entity to add
   */
  protected synchronized void  addFieldEntity(int fld_i, String entity) {
    // System.err.println("addFieldEntity(" + fld_i + ",\"" + entity + "\")");
    //
    // fld_i is used to indicate if this is a second iteration of addFieldEntity() to prevent
    // infinite recursion...  not sure if it's correct...  for example, what happens when
    // a post-processor transform converts a domain to an ip address - shouldn't that IP
    // address then be further decomposed?
    //
    if (fld_i != -1 && entity.equals(BundlesDT.NOTSET)) {
      ent_2_i.put(BundlesDT.NOTSET, -1); 
      fld_dts.get(fld_i).add(BundlesDT.DT.NOTSET);

    //
    // Decompose a tag into it's basic elements
    //
    } else if (fld_i != -1 && fieldHeader(fld_i).equals(BundlesDT.TAGS)) {
      addFieldEntity(-1,entity); // Add the tag itself
      Iterator<String> it = Utils.tokenizeTags(entity).iterator();
      while (it.hasNext()) {
        String tag = it.next(); addFieldEntity(-1, tag);
	if        (Utils.tagIsHierarchical(tag)) {
          String sep[] = Utils.tagDecomposeHierarchical(tag);
	  for (int i=0;i<sep.length;i++) addFieldEntity(-1, sep[i]);
	} else if (Utils.tagIsTypeValue(tag)) {
          String sep[] = Utils.separateTypeValueTag(tag);
	  tag_types.add(sep[0]);
	  addFieldEntity(-1,sep[1]);
	}
      }

    //
    // Otherwise, keep track of the datatype to field correlation and run post
    // processors on the item to have those lookups handy.
    //
    } else {
      // System.err.println("determining datatype for \"" + entity + "\""); // DEBUG
      BundlesDT.DT datatype = BundlesDT.getEntityDataType(entity); if (dt_count_lu.containsKey(datatype) == false) dt_count_lu.put(datatype,0);
      // System.err.println("datatype for \"" + entity + "\" ==> " + datatype); // DEBUG
      if (datatype != null) {
// System.err.println("390:fld_i = " + fld_i + " (" + fld_dts.containsKey(fld_i) + ")"); // abc DEBUG
// try { System.err.println("            " + fieldHeader(fld_i)); } catch (Throwable t) { }                              // abc DEBUG
        if (fld_i != -1) fld_dts.get(fld_i).add(datatype);
        if (ent_2_i.containsKey(entity) == false) {
	  // Use special rules to set integer correspondance
	  switch (datatype) {
	    case IPv4:     ent_2_i.put(entity, Utils.ipAddrToInt(entity));                                         break;
	    case IPv4CIDR: ent_2_i.put(entity, Utils.ipAddrToInt((new StringTokenizer(entity, "/")).nextToken())); break;
	    case INTEGER:  ent_2_i.put(entity, Integer.parseInt(entity));                                          break;
            case FLOAT:    ent_2_i.put(entity, Float.floatToIntBits(Float.parseFloat(entity)));                    break;
	    case DOMAIN:   ent_2_i.put(entity, Utils.ipAddrToInt("127.0.0.2") + dt_count_lu.get(datatype)); // Put Domains In Unused IPv4 Space
	                   dt_count_lu.put(datatype, dt_count_lu.get(datatype) + 1);
			   break;

	    // Pray that these don't collide - otherwise x/y scatters will be off...
	    default:       ent_2_i.put(entity, dt_count_lu.get(datatype));
	                   dt_count_lu.put(datatype, dt_count_lu.get(datatype) + 1);
			   break;
          }
	}
	// Map out the derivatives so that they will have values int the lookups
	// - Create the post processors
	if (post_processors == null) {
	  String post_proc_strs[] = BundlesDT.listEnabledPostProcessors();
	  post_processors = new PostProc[post_proc_strs.length];
	  for (int i=0;i<post_processors.length;i++) post_processors[i] = BundlesDT.createPostProcessor(post_proc_strs[i], this);
	}
	// - Run all of the post procs against their correct types
        if (fld_i != -1) for (int i=0;i<post_processors.length;i++) {
          if (post_processors[i].type() == datatype) {
	    String strs[] = post_processors[i].postProcess(entity);
	    for (int j=0;j<strs.length;j++) {
              if (entity.equals(strs[j]) == false) addFieldEntity(-1, strs[j]);
	    }
          }
	}
      } else if (ent_2_i.containsKey(entity) == false) {
        ent_2_i.put(entity, not_assoc.size());
	not_assoc.add(entity);
      }
    }
  }

  /**
   * Convert a string to a corresponding entity.
   *
   * @param  entity entity of the string to lookup
   * @return        corresponding integer for the entity
   */
  public int toInt(String entity) { return ent_2_i.get(entity); }

  /**
   * Transform Table Members.  First represents the original transform value.
   * - Example:  datatype | trans | var            | result
   * - Example:  IPv4CIDR | SPACE | 192.168.0.0/16 | PRIVATE
   */
  Map<BundlesDT.DT,Map<String,Map<String,String>>> transforms  = new HashMap<BundlesDT.DT,Map<String,Map<String,String>>>(),
  /**
   * Transform Table Members.  Second represents derivative transform values.
   */
                                                               transforms2 = new HashMap<BundlesDT.DT,Map<String,Map<String,String>>>();

  /**
   * Add a new transform for the overall data set.  Transforms are simple lookups based on
   * external data files that convert one entity string into another.
   *
   * @param datatype transform datatype
   * @param trans    transformation name
   * @param var      variable
   * @param result   transformed variable
   */
  protected void addTransform(BundlesDT.DT datatype, String trans, String var, String result) {
    // System.err.println("" + datatype + " : " + trans + " : " + var + " => " + result);
    if (transforms.containsKey(datatype)            == false) transforms.put(datatype, new HashMap<String,Map<String,String>>());
    if (transforms.get(datatype).containsKey(trans) == false) transforms.get(datatype).put(trans,new HashMap<String,String>());
    transforms.get(datatype).get(trans).put(var,result);
    // By default, an IPv4CIDR transform is also an IPv4 transform
    if (datatype == BundlesDT.DT.IPv4CIDR) addTransform(BundlesDT.DT.IPv4,trans,var,result);
  }

  /**
   * Clear out the existing transforms and recreate them based on the existing
   * data set.
   */
  public void resetTransforms() { 
    transforms2 = new HashMap<BundlesDT.DT,Map<String,Map<String,String>>>(); 
    transforms2.put(BundlesDT.DT.IPv4,     new HashMap<String,Map<String,String>>());
    transforms2.put(BundlesDT.DT.IPv4CIDR, new HashMap<String,Map<String,String>>());
    if (transforms.containsKey(BundlesDT.DT.IPv4)) {
      Iterator<String> it = transforms.get(BundlesDT.DT.IPv4).keySet().iterator();
      while (it.hasNext()) transforms2.get(BundlesDT.DT.IPv4).put(it.next(), new HashMap<String,String>());
    }
    if (transforms.containsKey(BundlesDT.DT.IPv4CIDR)) {
      sorted_cidr_trans = new HashMap<String,CIDRRec[]>();
      Iterator<String> it = transforms.get(BundlesDT.DT.IPv4CIDR).keySet().iterator();
      while (it.hasNext()) {
        String trans = it.next();
        transforms2.get(BundlesDT.DT.IPv4).put(trans, new HashMap<String,String>());
        transforms2.get(BundlesDT.DT.IPv4CIDR).put(trans, new HashMap<String,String>());
	CIDRRec recs[] = new CIDRRec[transforms.get(BundlesDT.DT.IPv4CIDR).get(trans).keySet().size()];
	Iterator<String> it_cidr = transforms.get(BundlesDT.DT.IPv4CIDR).get(trans).keySet().iterator();
	for (int i=0;i<recs.length;i++) {
	  String cidr = it_cidr.next();
	  recs[i] = new CIDRRec(cidr,transforms.get(BundlesDT.DT.IPv4CIDR).get(trans).get(cidr));
        }
	Arrays.sort(recs);
	sorted_cidr_trans.put(trans,recs);
      }
    }
  }

  /**
   * Get a list of transforms that are currently in the application.
   *
   * @return list of the transforms
   */
  public String[] getTransforms() {
    List<String>      al    = new ArrayList<String>();
    // Iterate through the transforms
    Iterator<BundlesDT.DT> it_dt = transforms.keySet().iterator();
    while (it_dt.hasNext()) {
      BundlesDT.DT datatype = it_dt.next();
      Iterator<String> it_trans = transforms.get(datatype).keySet().iterator();
      while (it_trans.hasNext()) {
        String trans = it_trans.next();
	al.add("" + datatype + BundlesDT.DELIM + trans);
      }
    }
    // Convert back to strings
    String strs[] = new String[al.size()]; for (int i=0;i<strs.length;i++) strs[i] = al.get(i);
    return strs;
  }

  /**
   * Sorted list of CIDR transformation records.  The sorting ensures that the
   * most specific match for a transformation occurs before more general CIDR
   * strings.
   */
  Map<String,CIDRRec[]> sorted_cidr_trans = new HashMap<String,CIDRRec[]>();

  /**
   * Class to contains a sortable version of a CIDR string.
   */
  class CIDRRec implements Comparable<CIDRRec> {
    String cidr,result; int cidr_mask, cidr_bits; int num;
    public CIDRRec(String cidr, String result) {
      this.cidr = cidr; this.result = result; cidr_mask = Utils.cidrMask(cidr); cidr_bits = Utils.cidrBits(cidr);
      StringTokenizer st = new StringTokenizer(cidr,"/"); st.nextToken(); num = Integer.parseInt(st.nextToken());
    }
    public boolean matches(int ip)          { return Utils.cidrMatch(ip, cidr_bits, cidr_mask); }
    public String  getResult()              { return result; }
    public int     compareTo(CIDRRec other) { return other.num - num; }
  }

  /**
   * Transform a specific variable into its corresponding lookup values.
   *
   * @param  datatype datatype for the to-be-transformed value
   * @param  trans    transform name
   * @param  var      variable to transform
   * @return          transformed strings.  Note that a single value can 
   *                  map to multiple transforms
   */
  public String[] transform(BundlesDT.DT datatype, String trans, String var) {
    // System.err.println("transform(" + datatype + "," + trans + "," + var + ")");
    // Handle IPv4 and IPv4CIDR specially...
    if (datatype == BundlesDT.DT.IPv4 || datatype == BundlesDT.DT.IPv4CIDR) {
      // datatype (IPv4) => Transform (longitude) => Variable (192.168.0.1)
      String val = transforms.get(datatype).get(trans).get(var);
      // If the transform function doesn't extist for the variable, add the value
      if (val == null) {
        // Check the secondary transform...
        // System.err.println("dt:" + datatype + " => trans:" + trans + " => var:" + var);
        // System.err.println("transforms2.containsKey("+datatype+") = "                + transforms2.containsKey(datatype));
        // System.err.println("transforms2.get("+datatype+").containsKey("+trans+") = " + transforms2.get(datatype).containsKey(trans));
	if (transforms2.get(datatype).get(trans).containsKey(var)) return toArray(transforms2.get(datatype).get(trans).get(var)); else {
	  // Look for a transform
          if (transforms.containsKey(BundlesDT.DT.IPv4CIDR) && transforms.get(BundlesDT.DT.IPv4CIDR).containsKey(trans) == false) {
	    transforms2.get(datatype).get(trans).put(var,BundlesDT.NOTSET);
	    return toArray(BundlesDT.NOTSET);
	  } else {
	    // Make the integer version of the ip address
	    int ip = (datatype == BundlesDT.DT.IPv4) ? Utils.ipAddrToInt(var) : Utils.ipAddrToInt((new StringTokenizer(var,"/")).nextToken());
	    // Get the sorted cidr recs
            CIDRRec recs[] = sorted_cidr_trans.get(trans);
	    // If they exist, try for a match
	    if (recs != null) {
	      for (int i=0;i<recs.length;i++) {
                if (recs[i].matches(ip)) {
	          transforms2.get(datatype).get(trans).put(var,recs[i].getResult());
                  return toArray(recs[i].getResult());
                }
	      }
	    }
	    transforms2.get(datatype).get(trans).put(var,BundlesDT.NOTSET);
	    return toArray(BundlesDT.NOTSET);
	  }
	}
      } else return toArray(val);
    } else {
      String val = transforms.get(datatype).get(trans).get(var);
      if (val == null) return toArray(BundlesDT.NOTSET); else return toArray(val);
    }
  }

  /**
   * Private method to convert a single string into a single element array.  Really
   * just a convenience method to keep single return values consistent with data
   * model.
   *
   * @param  str string to place into the array
   * @return     string containing single element
   */
  private String[] toArray(String str) { String arr[] = new String[1]; arr[0] = str; return arr; }

  /**
   * Set of the type-value type tags.
   */
  Set<String> tag_types = new HashSet<String>();

  /**
   * Return an iterator over the types in the tag type-value pairs.
   *
   * @return Iterator over the types
   */
  public Iterator<String> tagTypeIterator() { return tag_types.iterator(); }

  /**
   * Return the scalar flag for a specific field index.
   *
   * @param  fld_i field index
   * @return       flag indicating if the field is a scalar
   */
  public boolean               isScalar(int fld_i)                          { if (fld_i < 0) return false; else return sclr[fld_i]; }

  /** 
   * Return the field header name for a specific field index.
   *
   * @param  fld_i field index
   * @return       string for the header corresponding to the field index
   */
  public String                fieldHeader(int fld_i)                       { return flds[fld_i];      }

  /**
   * Return the field index for a specific header string.
   *
   * @paran  fld field name
   * @return     corresponding field index
   */
  public int                   fieldIndex(String fld)                       { if (flds_lu.containsKey(fld)) return flds_lu.get(fld); else return -1; }

  /**
   * Return the number of fields in the application.
   *
   * @return number of fields
   */
  public int                   numberOfFields()                             { return fld_count;                   }

  /**
   * Return an iterator that returns the field headers for the application.
   *
   * @return Iterator over the field headers
   */
  public Iterator<String>      fieldIterator()                              { return flds_lu.keySet().iterator(); }

  /**
   * Return the field data associated with this field index.  If more than one
   * type exists, then return null.  Note that notsets will not count towards
   * the data type return value.
   *
   * @param  fld_i field index
   * @return       the corresponding field data type as long as the set is one
   */
  public BundlesDT.DT          getFieldDataType(int fld_i)                  { 
    if      (fld_dts.containsKey(fld_i) == false) return null;
    else if (fld_dts.get(fld_i).size()  == 1)     return fld_dts.get(fld_i).iterator().next();
    else if (fld_dts.get(fld_i).size()  == 2) {
      Iterator<BundlesDT.DT> it = fld_dts.get(fld_i).iterator();
      BundlesDT.DT dt0 = it.next(), dt1 = it.next();
      if      (dt0 == BundlesDT.DT.NOTSET) return dt1;
      else if (dt1 == BundlesDT.DT.NOTSET) return dt0;
      else                                 return null; 
    } else return null;
  }

  /**
   * Return the set of data types associated with this field index.
   *
   * @param  fld_i field index
   * @return       set of data types
   */
  public Set<BundlesDT.DT> getFieldDataTypes(int fld_i)                 { return fld_dts.get(fld_i);          }

  /**
   * Determines if an entity exists in the overall dataset.  May not
   * be needed anymore...
   *
   * @param  str entity to lookup
   * @return     true if entity exists in the data set
   */
  public boolean               containsEntity(String str)                   { return ent_2_i.containsKey(str);    }

  /**
   * Return the entities matching a CIDR string that exist in the dataset.
   *
   * @param  cidr cidr string to use
   * @return      set of strings matching cidr representation
   */
  public Set<String>       getCIDRMatches(String cidr)                  { 
    Set<String>  set = new HashSet<String>();  
    int cidr_mask = Utils.cidrMask(cidr), cidr_bits = Utils.cidrBits(cidr);
    Iterator<String> it  = ent_2_i.keySet().iterator();
    while (it.hasNext()) {
      String entity = it.next();
      if (BundlesDT.getEntityDataType(entity) == BundlesDT.DT.IPv4) {
        int ip = ent_2_i.get(entity);
	if (Utils.cidrMatch(ip, cidr_bits, cidr_mask)) set.add(entity);
      }
    }
    return set;
  }


  /**
   * Return the entities matching a CIDR string that exist in the dataset.
   *
   * @param  subtext a partial match from a text that contains a cidr string
   * @param  cidr    cidr string to use
   * @return         set of strings matching cidr representation
   */
  public Set<SubText>      getCIDRMatches(SubText subtext, String cidr) { 
    Set<SubText> set = new HashSet<SubText>(); 
    int cidr_mask = Utils.cidrMask(subtext.toString()), cidr_bits = Utils.cidrBits(subtext.toString());
    Iterator<String> it  = ent_2_i.keySet().iterator();
    while (it.hasNext()) {
      String entity = it.next();
      if (BundlesDT.getEntityDataType(entity) == BundlesDT.DT.IPv4) {
        int ip = ent_2_i.get(entity);
	if (Utils.cidrMatch(ip, cidr_bits, cidr_mask)) set.add(new Entity(subtext.getFullText(), entity, BundlesDT.DT.IPv4, subtext.getIndex0(), subtext.getIndex1()));
      }
    }
    return set;
  }
}
