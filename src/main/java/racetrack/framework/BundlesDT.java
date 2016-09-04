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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;

import racetrack.transform.GeoData;
import racetrack.transform.MACAddress;
import racetrack.transform.UserAgents;
import racetrack.util.Utils;
import racetrack.util.CacheManager;

/**
 * Bundles DataType class.  Identifies strings by their corresponding datatypes.  Handles
 * other functions related to datatype manipulation including post processors {@link PostProc}
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class BundlesDT {
  /**
   * Global delimiter for strings.  It goes without saying that the string should not
   * occur in data elements or fields.  Bad things would ensue...
   */
  public static final String DELIM  = "|", 

  /**
   * Count by the records themselves
   */
                             COUNT_BY_BUNS    = DELIM + "bundles" + DELIM,

  /**
   * Don't count by anything -- good for secondary fields
   */
                             COUNT_BY_NONE    = DELIM + "none" + DELIM,

  /**
   * Count by the global setting...
   */
                             COUNT_BY_DEFAULT = DELIM + "default" + DELIM,

  /**
   * Specially crafted string to denote that this conversion would be applied to multiple
   * fields
   */
                             MULTI  = DELIM + "Mx",
  /**
   * Field denoting a delimited tag value set.  Needs to be expanded to any field that has
   * tag-like data...
   */
                             TAGS   = "tags",
  /**
   * Global string representing that data is not set in this specific field.
   */
			     NOTSET = "[notset]";

  /**
   * Enumeration of the different datatypes handled by the application.
   */
  public static enum  DT { URL, IPv4CIDR, IPv4, IPv6, DOMAIN, INTEGER, FLOAT, FLAG, TAGS, TIMESTAMP, USERAGENT, NOTSET, UNKNOWN, MACADDRESS, EMAIL, MD5 };

  /**
   * Datastructure containing list of datatypes
   */
  private static List<BundlesDT.DT>       dt_al    = new ArrayList<BundlesDT.DT>();

  /**
   * Datastructure containing regex for each data - note that index paired with the dt_al
   * structure.  Probably should combined into an inner class so that the pairing can't
   * be messed up...
   */
  private static List<String>             regex_al = new ArrayList<String>();

  /**
   * Lookup to convert a datatype into its corresponding regex string.
   */
  private static Map<BundlesDT.DT,Pattern> regex_lu = new HashMap<BundlesDT.DT,Pattern>();

  /**
   * Static directive that preloads the static datastructure for lookup, iterating over datatypes
   * - 2013-06-01:  Placed domain regex at lowest precedence...
   */
  static {
    dt_al.add(DT.URL);        regex_al.add(Utils.getURLRegex());       regex_lu.put(DT.URL,        Pattern.compile(Utils.getURLRegex()));
    dt_al.add(DT.IPv4CIDR);   regex_al.add(Utils.getIPv4CIDRRegex());  regex_lu.put(DT.IPv4CIDR,   Pattern.compile(Utils.getIPv4CIDRRegex()));
    dt_al.add(DT.IPv4);       regex_al.add(Utils.getIPv4Regex());      regex_lu.put(DT.IPv4,       Pattern.compile(Utils.getIPv4Regex()));
    dt_al.add(DT.IPv6);       regex_al.add(Utils.getIPv6Regex());      regex_lu.put(DT.IPv6,       Pattern.compile(Utils.getIPv6Regex()));
    dt_al.add(DT.EMAIL);      regex_al.add(Utils.getEmailRegex());     regex_lu.put(DT.EMAIL,      Pattern.compile(Utils.getEmailRegex()));
    dt_al.add(DT.USERAGENT);  regex_al.add(Utils.getUserAgentRegex()); regex_lu.put(DT.USERAGENT,  Pattern.compile(Utils.getUserAgentRegex()));
    dt_al.add(DT.INTEGER);    regex_al.add(Utils.getIntegerRegex());   regex_lu.put(DT.INTEGER,    Pattern.compile(Utils.getIntegerRegex()));
    dt_al.add(DT.FLOAT);      regex_al.add(Utils.getFloatRegex());     regex_lu.put(DT.FLOAT,      Pattern.compile(Utils.getFloatRegex()));
    dt_al.add(DT.FLAG);       regex_al.add(null);                      regex_lu.put(DT.FLAG,       null);
    dt_al.add(DT.TAGS);       regex_al.add(null);                      regex_lu.put(DT.TAGS,       null);
    dt_al.add(DT.TIMESTAMP);  regex_al.add(Utils.getTimeStampRegex()); regex_lu.put(DT.TIMESTAMP,  Pattern.compile(Utils.getTimeStampRegex()));
    dt_al.add(DT.DOMAIN);     regex_al.add(Utils.getDomainRegex());    regex_lu.put(DT.DOMAIN,     Pattern.compile(Utils.getDomainRegex()));
    dt_al.add(DT.MACADDRESS); regex_al.add(Utils.getMACRegex());       regex_lu.put(DT.MACADDRESS, Pattern.compile(Utils.getMACRegex()));
    dt_al.add(DT.MD5);        regex_al.add(Utils.getMD5Regex());       regex_lu.put(DT.MD5,        Pattern.compile(Utils.getMD5Regex()));
    // dt_al.add(DT.abc);       regex_al.add(Utils.getabcRegex());     regex_lu.put(DT.abc, Pattern.compile(Utils.getabcRegex()));
  }

  /**
   * Return the number of datatypes that the application understands.
   *
   * @return number of datatypes
   */
  public static int                    getNumberOfDataTypes()    { return dt_al.size();     }

  /**
   * Returns an iterator over the datatypes
   *
   * @return iterator over datatypes
   */
  public static Iterator<BundlesDT.DT> dataTypesIterator()       { return dt_al.iterator(); }

  /**
   * Return the datatype at the corresponding index in the static preload.
   *
   * @param   i index needed
   * @return    corresponding datatype
   */
  public static BundlesDT.DT           getDataType(int i)        { return dt_al.get(i);     }

  /**
   * Return the datatype's regex for a specific index
   *
   * @param  i index needed
   * @return   corresponding datatype regex
   */
  public static String                 getDataTypeRegex(int i)   { return regex_al.get(i);  }

  /**
   * Parse a string into its corresponding datatype.  Note that this method is
   * for parsing a datatype's string representation...  not for parsing data into
   * a datatype.  For example the string "IPv4" would be parsed to DT.IPv4.
   *
   * @param  str string representation of the datatype enumeration
   * @return     corresponding datatype for that string
   */
  public static BundlesDT.DT           parseDataType(String str) {
    Iterator<BundlesDT.DT> it = dataTypesIterator();
    while (it.hasNext()) { BundlesDT.DT dt = it.next(); if (str.equals("" + dt)) { return dt; } }
    return null;
  }

  /**
   * Determine if a string matches a datatype's regex.
   *
   * @param  str  string to check
   * @param  dt   datatype to check against
   * @return      true if the string matches the datatypes regex
   */
  public static boolean      stringIsType(String str, BundlesDT.DT dt) { 
    if (regex_lu.containsKey(dt) == false || regex_lu.get(dt) == null) return false;
    // System.err.println("Checking \"" + str + "\" Against " + dt + "...  regex = \"" + regex_lu.get(dt) + "\""); // DEBUG
    boolean result = regex_lu.get(dt).matcher(str).matches();
    // System.err.println(" ==> " + result); // DEBUG
    return result;
  }

  /**
   * Convert an entity string into its corresponding datatype.  Note that
   * the ordering of regex has to be constructed so that the strictest
   * method is used first.  For instance, domain prior to ipv4. Note for
   * performance reasons, the results are cached (and need to be cleared
   * when data is removed from the application.)
   *
   * @param  entity entity to parse
   * @return        corresponding datatype
   */
  public static BundlesDT.DT getEntityDataType(String entity) {
    if      (entity.equals(BundlesDT.NOTSET)) return BundlesDT.DT.NOTSET;
    else if (dt_cache.containsKey(entity))    return dt_cache.get(entity);
    else {
      for (int i=0;i<dt_al.size();i++) {
        // System.err.println("Checking Against Data Type \"" + dt_al.get(i) + "\""); // DEBUG
        if (stringIsType(entity, dt_al.get(i))) {
          dt_cache.put(entity,dt_al.get(i));
          return dt_al.get(i);
        }
      }
    }
    dt_cache.put(entity, BundlesDT.DT.UNKNOWN);
    return BundlesDT.DT.UNKNOWN;
  }

  /**
   * Cache lookup for a string into its corresponding datatype
   */
  private static Map<String,BundlesDT.DT> dt_cache;
  static {
    dt_cache = new HashMap<String,BundlesDT.DT>();
    CacheManager.registerCache("Bundles DT Cache", dt_cache);
  }

  /**
   * (Built in) Convert an IPv4 object into the CIDR/8 representation
   */
  static final String IPV4_CIDR_08         = "C/08",       

  /**
   * (Built in) Convert an IPv4 object into the CIDR/16 representation
   */
                      IPV4_CIDR_16         = "C/16",    

  /**
   * (Built in) Convert an IPv4 object into the CIDR/24 representation
   */
		      IPV4_CIDR_24         = "C/24",

  /**
   * (Built in) Convert an IPv4 object into just the high octet
   */
                      IPV4_OCT_00          = "Oct 0 (Hi)", 

  /**
   * (Built in) Convert an IPv4 object into just the second highest octet
   */
		      IPV4_OCT_01          = "Oct 1",   

  /**
   * (Built in) Convert an IPv4 object into third octet
   */
		      IPV4_OCT_02          = "Oct 2",   

  /**
   * (Built in) Convert an IPv4 object into lowest octet
   */
		      IPV4_OCT_03          = "Oct 3 (Lo)",

  /**
   * (Built in) Convert an IPv4 object into its country code
   */
		      IPV4_CC              = "API_CC",     

  /**
   * (Built in) Convert an IPv4 object into its ASN
   */
		      IPV4_ASN             = "API_ASN", 

  /**
   * (Built in) Convert an IPv4 object into its Organization designator
   */
		      IPV4_ORG             = "API_ORG",

  /**
   * (Built in) Convert an integer into its logarithmic bin
   */
		      INTEGER_LOG_BIN      = "Log Bins",
  /**
   * (Built in) Convert an integer to it's hex format
   */
                      INTEGER_HEX          = "Hex",
  /**
   * (Built in) Convert an integer to it's bit format
   */
                      INTEGER_BITS         = "Bits",
  /**
   * (Built in) Convert a domain into its top-level component
   */
		      DOMAIN_TLD           = "TLD",
  /**
   * (Built in) Convert an email to its name
   */
                      EMAIL_NAME           = "EmailName",
  /**
   * (Built in) Convert an email to its domain
   */
                      EMAIL_DOMAIN         = "EmailDom",
  /**
   * (Built in) Convert an User Agent string into its type
   */
		      USERAGENT_TYPE       = "UAType",

  /**
   * (Built in) Convert an User Agent string into its name
   */
		      USERAGENT_NAME       = "UAName",

  /**
   * (Built in) Convert an User Agent string into its family
   */
		      USERAGENT_FAMILY     = "UAFam",

  /**
   * (Built in) Convert an User Agent string into its company
   */
		      USERAGENT_COMPANY    = "UAComp",

  /**
   * (Built in) Convert an User Agent string into its osname
   */
		      USERAGENT_OSNAME     = "UAOSName",

  /**
   * (Built in) Convert an User Agent string into its osfamily
   */
		      USERAGENT_OSFAMILY   = "UAOSFam",

  /**
   * (Built in) Convert an User Agent string into its os company
   */
		      USERAGENT_OSCOMPANY  = "UAOSComp",

  /**
   * (Built in) Convert a URL into the underlying domain
   * Would be nice to have this go to recursive decomposition...
   */
		      URL_DOMAIN           = "Domain",     

  /**
   * (Built in) Convert a URL into the page
   */
		      URL_PAGE             = "Page",  

  /**
   * (Built in) Convert a URL into its path
   */
		      URL_PATH             = "Path",
  /**
   * (Built in) MAC Address Organization
   */
                      MAC_ORG              = "MAC Org";

  /**
   * List of built-in post processors.
   */
  static final String post_processors[] = { IPV4_CIDR_08,    IPV4_CIDR_16,   IPV4_CIDR_24,
                                            IPV4_OCT_00,     IPV4_OCT_01,    IPV4_OCT_02,   IPV4_OCT_03,
					    IPV4_CC,         IPV4_ASN,       IPV4_ORG,
					    INTEGER_LOG_BIN, INTEGER_HEX,    INTEGER_BITS,
		                            DOMAIN_TLD,
                                            EMAIL_NAME,      EMAIL_DOMAIN,
		                            USERAGENT_TYPE,  USERAGENT_NAME, USERAGENT_FAMILY, USERAGENT_COMPANY, USERAGENT_OSNAME, USERAGENT_OSFAMILY, USERAGENT_OSCOMPANY,
		                            URL_DOMAIN,      URL_PAGE,       URL_PATH,
                                            MAC_ORG };
  /**
   * Set of the enabled post processors
   */
  private static Set<String> enabled_post_procs = new HashSet<String>();

  /**
   * Enable a specific post processor.  Reset transforms will then be called to add the necessary lookup tables.
   *
   */
  public static void enablePostProcessor(Bundles bundles, String post_processor) {
    Set<Bundles> set = new HashSet<Bundles>(); set.add(bundles);
    enabled_post_procs.add(post_processor); bundles.getGlobals().cleanse(set);
  }

  /**
   * Disable a specific post processor.  Reset transforms will then be called to reset the necessary lookup tables.
   */
  public static void disablePostProcessor(Bundles bundles, String post_processor) {
    Set<Bundles> set = new HashSet<Bundles>(); set.add(bundles);
    enabled_post_procs.remove(post_processor); bundles.getGlobals().cleanse(set);
  }

  /**
   * List the post processors available in the application.  Includes both
   * built-in post processors and dynamically available ones.
   *
   * @param  globals application global data used to identify dynamic post processors
   * @return         string array of post processors by name.  Note that the name
   *                 itself can be used to instantiate a post processor.
   */
  public static String[] listAvailablePostProcessors() {
    List<String> all = new ArrayList<String>();
    // Add the built in post processors
    for (int i=0;i<post_processors.length;i++) all.add(post_processors[i]);
    // Add the dynamic types
    // String transforms[] = globals.getTransforms();
    // for (int i=0;i<transforms.length;i++) all.add(transforms[i]);
    // convert back to strings and return
    String as_strs[] = new String[all.size()]; for (int i=0;i<as_strs.length;i++) as_strs[i] = all.get(i);
    return as_strs;
  }

  /**
   * List the post processors enabled in the application.  Includes both
   * built-in post processors and dynamically available ones.
   *
   * @param  globals application global data used to identify dynamic post processors
   * @return         string array of post processors by name.  Note that the name
   *                 itself can be used to instantiate a post processor.
   */
  public static String[] listEnabledPostProcessors() {
    // Add the built in post processors that are enabled
    List<String> enabled = new ArrayList<String>(); enabled.addAll(enabled_post_procs);
    // Add the dynamic types
    // String transforms[] = globals.getTransforms();
    // for (int i=0;i<transforms.length;i++) enabled.add(transforms[i]);
    // convert back to strings and return
    String as_strs[] = new String[enabled.size()]; for (int i=0;i<as_strs.length;i++) as_strs[i] = enabled.get(i);
    return as_strs;
  }
  
  /**
   * Convert a single string into an array of strings (with one element).
   * Convenience method.
   *
   * @param  str string to embed in array
   * @return     array with one element
   */
  public static String[] toArray(String str) { String arr[] = new String[1]; arr[0] = str; return arr; }

  /**
   * Create the post process {@link PostProc} for the specified string
   * describing the post processor.
   *
   * @param  post    string description of the post processor
   * @param  globals global data structure
   * @return         Actual post processor based on the string
   */
  public static PostProc createPostProcessor(String post, BundlesG globals) {
    if      (post.equals(IPV4_CIDR_08))    return new PostProc() { public DT type() { return DT.IPv4; }   
                                                                public String[] postProcess(String str) { if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
                                                                                                          else return toArray(Utils.ipv4CIDR08(str)); } };
    else if (post.equals(IPV4_CIDR_16))    return new PostProc() { public DT type() { return DT.IPv4; }
                                                                public String[] postProcess(String str) { if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
                                                                                                          else return toArray(Utils.ipv4CIDR16(str)); } };
    else if (post.equals(IPV4_CIDR_24))    return new PostProc() { public DT type() { return DT.IPv4; }   
                                                                public String[] postProcess(String str) { if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
                                                                                                          else return toArray(Utils.ipv4CIDR24(str)); } };
    else if (post.equals(IPV4_OCT_00))     return new PostProc() { public DT type() { return DT.IPv4; }   
                                                                public String[] postProcess(String str) { if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
                                                                                                          else return toArray(Utils.ipv4Octet00(str)); } };
    else if (post.equals(IPV4_OCT_01))     return new PostProc() { public DT type() { return DT.IPv4; }
                                                                public String[] postProcess(String str) { if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
                                                                                                          else return toArray(Utils.ipv4Octet01(str)); } };
    else if (post.equals(IPV4_OCT_02))     return new PostProc() { public DT type() { return DT.IPv4; }
                                                                public String[] postProcess(String str) { if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
                                                                                                          else return toArray(Utils.ipv4Octet02(str)); } };
    else if (post.equals(IPV4_OCT_03))     return new PostProc() { public DT type() { return DT.IPv4; }
                                                                public String[] postProcess(String str) { if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
                                                                                                          else return toArray(Utils.ipv4Octet03(str)); } };
    else if (post.equals(IPV4_CC))         return new PostProc() { public DT type() { return DT.IPv4; }
                                                                public String[] postProcess(String str) {
                                                                  if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
								  String post = GeoData.getInstance().geoCC(DT.IPv4,str);
								  if (post == null) post = BundlesDT.NOTSET;
								  return toArray(post); } };
    else if (post.equals(IPV4_ASN))        return new PostProc() { public DT type() { return DT.IPv4; }
                                                                public String[] postProcess(String str) {
                                                                  if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
								  String post = GeoData.getInstance().geoASN(DT.IPv4,str);
								  if (post == null) post = BundlesDT.NOTSET;
								  return toArray(post); } };
    else if (post.equals(IPV4_ORG))            return new PostProc() { public DT type() { return DT.IPv4; }
                                                                       public String[] postProcess(String str) {
                                                                         if (str.equals(BundlesDT.NOTSET)) return toArray(BundlesDT.NOTSET);
							    	         String post = GeoData.getInstance().geoOrg(DT.IPv4,str);
								         if (post == null) post = BundlesDT.NOTSET;
								         return toArray(post); } };
    else if (post.equals(INTEGER_LOG_BIN))     return new PostProc() { public DT type() { return DT.INTEGER; }
                                                                       public String[] postProcess(String str) { return toArray(Utils.integerLogBins(str)); } };
    else if (post.equals(INTEGER_HEX))         return new PostProc() { public DT type() { return DT.INTEGER; }
                                                                       public String[] postProcess(String str) { str = Integer.toString(Integer.parseInt(str),16);
                                                                                                                 while (str.length() < 8)  str = "0" + str;
                                                                                                                 return toArray("0x" + str); } };
    else if (post.equals(INTEGER_BITS))        return new PostProc() { public DT type() { return DT.INTEGER; }
                                                                       public String[] postProcess(String str) { str = Integer.toString(Integer.parseInt(str),2); 
								                                                 while (str.length() < 32) str = "0" + str;
														 return toArray("0b" + str); } };
    else if (post.equals(DOMAIN_TLD))          return new PostProc() { public DT type() { return DT.DOMAIN; }
                                                                       public String[] postProcess(String str) { return toArray(Utils.domainTLD(str)); } };
    else if (post.equals(EMAIL_NAME))          return new PostProc() { public DT type() { return DT.EMAIL; }
                                                                       public String[] postProcess(String str) { return toArray(str.substring(0,str.indexOf("@"))); } };
    else if (post.equals(EMAIL_DOMAIN))        return new PostProc() { public DT type() { return DT.EMAIL; }
                                                                       public String[] postProcess(String str) { return toArray(str.substring(str.indexOf("@")+1,str.length())); } };
    else if (post.equals(USERAGENT_TYPE))      return new PostProc() { public DT       type() { return DT.USERAGENT; }
                                                                       public String[] postProcess(String str) { return toArray(UserAgents.getInstance().lookup(str, UserAgents.Component.TYPE)); } };
    else if (post.equals(USERAGENT_NAME))      return new PostProc() { public DT       type() { return DT.USERAGENT; }
                                                                       public String[] postProcess(String str) { return toArray(UserAgents.getInstance().lookup(str, UserAgents.Component.NAME)); } };
    else if (post.equals(USERAGENT_FAMILY))    return new PostProc() { public DT       type() { return DT.USERAGENT; }
                                                                       public String[] postProcess(String str) { return toArray(UserAgents.getInstance().lookup(str, UserAgents.Component.FAMILY)); } };
    else if (post.equals(USERAGENT_COMPANY))   return new PostProc() { public DT       type() { return DT.USERAGENT; }
                                                                       public String[] postProcess(String str) { return toArray(UserAgents.getInstance().lookup(str, UserAgents.Component.COMPANY)); } };
    else if (post.equals(USERAGENT_OSNAME))    return new PostProc() { public DT       type() { return DT.USERAGENT; }
                                                                       public String[] postProcess(String str) { return toArray(UserAgents.getInstance().lookup(str, UserAgents.Component.OSNAME)); } };
    else if (post.equals(USERAGENT_OSFAMILY))  return new PostProc() { public DT       type() { return DT.USERAGENT; }
                                                                       public String[] postProcess(String str) { return toArray(UserAgents.getInstance().lookup(str, UserAgents.Component.OSFAMILY)); } };
    else if (post.equals(USERAGENT_OSCOMPANY)) return new PostProc() { public DT       type() { return DT.USERAGENT; }
                                                                       public String[] postProcess(String str) { return toArray(UserAgents.getInstance().lookup(str, UserAgents.Component.OSCOMPANY)); } };
    else if (post.equals(URL_DOMAIN))          return new PostProc() { public DT type() { return DT.URL; }
                                                                       public String[] postProcess(String str) { return toArray(Utils.urlDomain(str)); } };
    else if (post.equals(URL_PAGE))            return new PostProc() { public DT type() { return DT.URL; }
                                                                       public String[] postProcess(String str) { return toArray(Utils.urlPage(str)); } };
    else if (post.equals(URL_PATH))            return new PostProc() { public DT type() { return DT.URL; }
                                                                       public String[] postProcess(String str) { return toArray(Utils.urlPath(str)); } };
    else if (post.equals(MAC_ORG))             return new PostProc() { public DT type() { return DT.MACADDRESS; }
                                                                       public String[] postProcess(String str) { return toArray(MACAddress.getInstance().getOrg(str)); } };
    else if (post.indexOf(BundlesDT.DELIM) >= 0) return new DynamicPostProcessor(post, globals);
    else throw new RuntimeException("Unknown Post Proc \"" + post + "\"");
    // else                                   return new PostProc() { public DT type() { return null; } public String postProcess(String str) { return str; } };
  }

  /**
   * Return the multiple types based on the datatype.
   *
   * @param  dt      specified datatype
   * @param  globals global data structure
   * @return         an array of the multi representation
   */
  public static String[] getMultis(DT dt, BundlesG globals) {
    String vars[] = dataTypeVariations(dt,globals);
    for (int i=0;i<vars.length;i++) { vars[i] = dt + MULTI + BundlesDT.DELIM + vars[i]; }
    String all[]  = new String[vars.length+1];
    all[0]        = dt + MULTI;
    System.arraycopy(vars, 0, all, 1, vars.length);
    return all;
  }

  /**
   * Calculate the datatype variations for the specified datatype.  Pulls
   * the post processors {@link PostProc) and identifies which apply to this
   * specific datatype.
   *
   * @param  dt      specified datatype
   * @param  globals global data structure
   * @return the post processors that correspond to the specified datatype
   */
  public static String[] dataTypeVariations(DT dt, BundlesG globals) {
    List<String> al = new ArrayList<String>();
    String post_procs_strs[] = listEnabledPostProcessors();
    for (int i=0;i<post_procs_strs.length;i++) {
      PostProc post_proc = createPostProcessor(post_procs_strs[i], globals);
      if (post_proc.type() == dt) al.add(post_procs_strs[i]);
    }
    String strs[] = new String[al.size()]; for (int i=0;i<strs.length;i++) strs[i] = al.get(i);
    return strs;
  }
}
