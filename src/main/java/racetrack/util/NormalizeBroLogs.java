/* 

Copyright 2016 David Trimm

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import racetrack.framework.BundlesDT;

import racetrack.util.Utils;

/**
 * Normalize bro log files into a file format readable by racetrack application (CSV, URL encoded...)
 * 
 * @author  D. Trimm
 * @version 0.1
 */
public class NormalizeBroLogs { 
  /**
   * Construct the instance... not much to see here.  Pre-determined scalar fields are calculated here.
   */
  public NormalizeBroLogs() { 
    to_all_caps.add("orig_ip_bytes");
    to_all_caps.add("orig_bytes");
    to_all_caps.add("orig_pkts");
    to_all_caps.add("resp_ip_bytes");
    to_all_caps.add("resp_bytes");
    to_all_caps.add("resp_pkts");
    to_all_caps.add("response_body_len");
    to_all_caps.add("total_bytes");
    to_all_caps.add("seen_bytes");
    to_all_caps.add("missing_bytes");
    to_all_caps.add("missed_bytes");
    to_all_caps.add("overflow_bytes");
    to_all_caps.add("request_body_len");
  }

  /**
   * Fields that need to be made into all caps (to represent scalar values)
   */
  Set<String> to_all_caps = new HashSet<String>();

  /**
   * Generic parse errors flag - should be set if any parse error was found
   */
  boolean parse_errors = false;

  /**
   * Error flag - ints exceeded max and were converted back to the max
   */
  boolean ints_exceed_max = false;

  /**
   * Unrecognized metafile information
   */
  boolean unrecognized_metafile_info = false;

  /**
   * Unknown field separator encountered
   */
  boolean unknown_separator = false;

  /**
   * Print out parser errors.
   *
   *@param out print stream
   */
  public void printParseErrors(PrintStream out) {
    if (parse_errors) {
      out.println("**\n** Parse Errors\n**\n");
      if (ints_exceed_max)             out.println("  Ints exceed max int value and were set to max");
      if (unrecognized_metafile_info)  out.println("  Unrecognized metafile information");
      if (unknown_separator)           out.println("  Unknown separator format");
      out.println("\n\n");
    }
  }

  /**
   * Read a file in, normalize the header and the records and dump to the provided PrintStream.
   *
   *@param file file to parse
   *@param out  PrintStream for normalized results
   */
  public void normalize(File file, PrintStream out) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(file)); 
    
    // Parsing variables/state
    String               separator        = " ";
    String               fields[]         = null;
    boolean              has_timestamp    = false;
    boolean              has_duration     = false;
    int                  file_field_count = -1;
    int                  timestamp_i      = -1;
    int                  duration_i       = -1;
    String               path_str         = "";
    String               empty_str        = "";
    String               unset_str        = "";
    char                 set_separator    = ',';
    Map<String,Integer> fields_map        = new HashMap<String,Integer>();

    String line = null; int line_no = 0; while ((line = in.readLine()) != null) { line_no++;
      //
      // Parse the metafile lines separately -- these include info on fields, field types, etc.
      //
      if (line.startsWith("#")) {
        StringTokenizer st = new StringTokenizer(line, separator);
	String first = st.nextToken();
        if        (first.equals("#separator"))     { separator = parseSeparator(st.nextToken());
	} else if (first.equals("#set_separator")) { String str = st.nextToken(); set_separator = str.charAt(0); if (str.length() > 1) throw new RuntimeException("Set separator more than one character \"" + str + "\"");
	} else if (first.equals("#empty_field"))   { empty_str = st.nextToken();
	} else if (first.equals("#unset_field"))   { unset_str = st.nextToken();
	} else if (first.equals("#path"))          { path_str = st.nextToken();
	} else if (first.equals("#open"))          {
	} else if (first.equals("#fields"))        {
	  file_field_count = st.countTokens();
	  fields = new String[file_field_count + 1]; // extra for file type
	  for (int i=0;i<fields.length-1;i++) fields[i] = st.nextToken();
          fields[fields.length - 1] = "parse_filetype";
	  for (int i=0;i<fields.length;i++) { if (fields[i].equals("ts"))       { has_timestamp = true; fields[i] = "timestamp"; timestamp_i = i; }
                                              if (fields[i].equals("duration")) { has_duration  = true;                          duration_i  = i; } }
	  if (has_timestamp && has_duration) { String new_fields[] = new String[fields.length+1]; System.arraycopy(fields, 0, new_fields, 0, fields.length); new_fields[new_fields.length - 1] = "timestamp_end"; fields = new_fields; }

          // Make the scalar fields all caps
          for (int i=0;i<fields.length;i++) { if (to_all_caps.contains(fields[i].toLowerCase())) fields[i] = fields[i].toUpperCase(); 
	                                      else                                               fields[i] = fields[i].toLowerCase(); }

          // Print the header
	  for (int i=0;i<fields.length;i++) {
	    if (i > 0) out.print(",");
	    out.print(fields[i]);
            fields_map.put(fields[i], i);
	  }

	  // For dns records... awful hack
	  if (fields_map.containsKey("answers") && fields_map.containsKey("query") && fields_map.containsKey("timestamp")) out.print(",tags");

	  out.println("");

	} else if (first.equals("#types"))         {
	} else if (first.equals("#close"))         {
	} else { parse_errors = unrecognized_metafile_info = true;  throw new RuntimeException("Unrecognized metafile line in " + file + "\nLine :\"" + line + "\""); }

      //
      // Parse the actual record lines separately
      //
      } else                   {
        StringTokenizer st = new StringTokenizer(line, separator);
	if (st.countTokens() != file_field_count) { System.err.println("File \"" + file + "\", Line " + line_no + " - Incorrect Token Count\n  Line \"" + line + "\"");
        } else {
	  String tokens[] = new String[fields.length]; for (int i=0;i<file_field_count;i++) tokens[i] = st.nextToken();

	  // Append file type and file path
	  tokens[file_field_count+0] = path_str;
	  
	  // Take care of timestamp conversions
	  if (has_timestamp) {
	    // Extract the timestamp field and convert it to a normalized value
	    long millis = secondFractionsToMillis(tokens[timestamp_i]);
	    tokens[timestamp_i] = Utils.exactDate(millis);

            // Take care of the duration (if it exists)
	    if (fields[fields.length-1].equals("timestamp_end")) {
	      if (tokens[duration_i].equals(unset_str)) {
	        tokens[tokens.length-1] = Utils.exactDate(millis);
	      } else {
	        long duration_millis = secondFractionsToMillis(tokens[duration_i]);
	        tokens[tokens.length-1] = Utils.exactDate(millis + duration_millis);
              }
	    }
	  }

	  // Make a few replicas (in case the processign modifies the states)
          String proc_fields[] = new String[fields.length]; System.arraycopy(fields, 0, proc_fields, 0, proc_fields.length);
          String proc_tokens[] = new String[tokens.length]; System.arraycopy(tokens, 0, proc_tokens, 0, proc_tokens.length);

          // Special processing section -- usually for a field with multiple components
	  if (fields_map.containsKey("answers")                       && fields_map.containsKey("query")                         && 
	      fields_map.containsKey("timestamp"))                                                                                  { String results[][] = captureDNS(proc_fields, proc_tokens, fields_map, set_separator, unset_str);
                                                                                                                                      proc_fields = results[0];
                                                                                                                                      proc_tokens = results[1]; }
	  if (fields_map.containsKey("certificate.issuer")            && fields_map.containsKey("certificate.subject")           && 
	      fields_map.containsKey("certificate.not_valid_before")  && fields_map.containsKey("certificate.not_valid_after")   &&
	      fields_map.containsKey("certificate.key_alg")           && fields_map.containsKey("certificate.sig_alg")           &&
	      fields_map.containsKey("certificate.serial")            && fields_map.containsKey("uid"))                             { captureCertificate(proc_fields, proc_tokens, fields_map, set_separator); }


          if (fields_map.containsKey("client_issuer")  && isNonEmpty(proc_tokens[fields_map.get("client_issuer")],  empty_str, unset_str) && fields_map.containsKey("uid"))
            captureSSL(proc_tokens[fields_map.get("timestamp")], "client_issuer", proc_tokens[fields_map.get("client_issuer")], proc_tokens[fields_map.get("uid")], 
                       proc_tokens[fields_map.get("server_name")], proc_tokens[fields_map.get("id.orig_h")], set_separator);

	  if (fields_map.containsKey("client_subject") && isNonEmpty(proc_tokens[fields_map.get("client_subject")], empty_str, unset_str) && fields_map.containsKey("uid"))
            captureSSL(proc_tokens[fields_map.get("timestamp")], "client_subject", proc_tokens[fields_map.get("client_subject")], proc_tokens[fields_map.get("uid")],
                       proc_tokens[fields_map.get("server_name")], proc_tokens[fields_map.get("id.orig_h")], set_separator);

	  if (fields_map.containsKey("issuer")         && isNonEmpty(proc_tokens[fields_map.get("issuer")],         empty_str, unset_str) && fields_map.containsKey("uid"))
            captureSSL(proc_tokens[fields_map.get("timestamp")], "issuer", proc_tokens[fields_map.get("issuer")], proc_tokens[fields_map.get("uid")],
                       proc_tokens[fields_map.get("server_name")], proc_tokens[fields_map.get("id.orig_h")], set_separator);

	  if (fields_map.containsKey("subject")        && isNonEmpty(proc_tokens[fields_map.get("subject")],        empty_str, unset_str) && fields_map.containsKey("uid"))
            captureSSL(proc_tokens[fields_map.get("timestamp")], "subject", proc_tokens[fields_map.get("subject")], proc_tokens[fields_map.get("uid")],
                       proc_tokens[fields_map.get("server_name")], proc_tokens[fields_map.get("id.orig_h")], set_separator);

	  // Print it to a line
          for (int i=0;i<proc_tokens.length;i++) {
	    if (i > 0) out.print(",");
	    if (proc_fields[i].equals("timestamp") || proc_fields[i].equals("timestamp_end")) out.print(proc_tokens[i]);
	    else {
             if (Utils.allNumbers(proc_tokens[i])) { try { Integer.parseInt(proc_tokens[i]); } catch (NumberFormatException nfe) { proc_tokens[i] = "" + Integer.MAX_VALUE; parse_errors = ints_exceed_max = true; } } 
             if (proc_tokens[i].equals(empty_str) || proc_tokens[i].equals(unset_str)) proc_tokens[i] = "";
             out.print(Utils.encToURL(proc_tokens[i]));
            }
	  }
	  out.println("");
	  }
	}
      }
    in.close();
  }

  /**
   * Field is not empty
   *
   *@return true if the field is not empty
   */
  public boolean isNonEmpty(String str, String empty_str, String unset_str) {
    if (str == null || str.equals("") || str.equals(empty_str) || str.equals(unset_str)) return false;
    return true;
  }

  /**
   * Capture information about dns.
   *
   *@param fields     field names
   *@param tokens     field values for this record
   *@param fields_map lookup to convert fields to indices
   *
   *@return header and tokens...
   */
  public String[][] captureDNS(String fields[], String tokens[], Map<String,Integer> fields_map, char set_sep, String unset_str) {
    // Add a tags field...
    String new_fields[] = new String[fields.length+1]; System.arraycopy(fields, 0, new_fields, 0, fields.length); new_fields[new_fields.length-1] = "tags"; fields = new_fields;
    String new_tokens[] = new String[tokens.length+1]; System.arraycopy(tokens, 0, new_tokens, 0, tokens.length); new_tokens[new_tokens.length-1] = "";     tokens = new_tokens;

    // Figure out if there were answer(s)
    String ans = tokens[fields_map.get("answers")];
    if (ans.equals("") || ans.equals(unset_str)) { } else {
      List<String> sep = separate(ans, set_sep);
      StringBuffer sb = new StringBuffer();
      for (int i=0;i<sep.size();i++) {
        if (i > 0) sb.append(BundlesDT.DELIM);
        sb.append("answer=" + Utils.encToURL(sep.get(i)));
      }
      tokens[tokens.length-1] = sb.toString();
    }

    // Capture the fields/tokens into a return results
    String results[][] = new String[2][];
    results[0] = fields;
    results[1] = tokens;

    return results;
  }

  /**
   * Capture information about an SSL connection.
   *
   *@param timestamp timestamp of the SSL connect
   *@param prefix    prefix (primary field) for the issuer/client/etc
   *@param keyvals   key values to break apart
   *@param uid       uid of the original record for a linkage
   *@param set_sep   set separator
   */
  public void captureSSL(String timestamp, String prefix, String keyvals, String uid, String server_name, String orig_h, char set_sep) {
    Map<String,String> map  = makeMap(keyvals, set_sep);
    Set<String>        set  = new HashSet<String>(); set.addAll(map.keySet());
    // Add in the list of knowns (this decreases the tablet entropy...)
    set.add("CN"); set.add("O"); set.add("C"); set.add("OU"); set.add("L"); set.add("ST"); set.add("serialNumber"); set.add("emailAddress");
    set.add("businessCategory"); set.add("street"); set.add("description"); set.add("DC"); set.add("postOfficeBox");
    set.add("postalCode");
    List<String>       sort = new ArrayList<String>(); sort.addAll(set); Collections.sort(sort);

    String header = "timestamp"   + "," +
                    "prefix"      + "," +
                    "server_name" + "," +
                    "id.orig_h"   + "," +
                    "uid";
    for (int i=0;i<sort.size();i++) header += "," + "ssl_info." + sort.get(i);
    
    String row = timestamp + "," + prefix + "," + server_name + "," + orig_h + "," + uid;
    for (int i=0;i<sort.size();i++){
       if (map.containsKey(sort.get(i))) {
        String to_cat = map.get(sort.get(i));
        if (Utils.allNumbers(to_cat)) {
          try { Integer.parseInt(to_cat); } catch (NumberFormatException nfe) { to_cat = "toobig-" + to_cat; }
        }
        row += "," + Utils.encToURL(to_cat);
       } else row += ",";
    }

    // Save off for later output
    if (hdr_to_rows.containsKey(header) == false) hdr_to_rows.put(header, new ArrayList<String>());
    hdr_to_rows.get(header).add(row);
  }

  /**
   * Capture information about certificates.
   *
   *@param fields     field names
   *@param tokens     field values for this record
   *@param fields_map lookup to convert fields to indices
   */
  public void captureCertificate(String fields[], String tokens[], Map<String,Integer> fields_map, char set_sep) {
    Map<String,String> subject_map = makeMap(tokens[fields_map.get("certificate.subject")], set_sep),
                       issuer_map  = makeMap(tokens[fields_map.get("certificate.issuer")], set_sep);
    String not_valid_before_str = tokens[fields_map.get("certificate.not_valid_before")],
           not_valid_after_str  = tokens[fields_map.get("certificate.not_valid_after")];
    long   not_valid_before     = secondFractionsToMillis(not_valid_before_str),
           not_valid_after      = secondFractionsToMillis(not_valid_after_str);
    String uid                  = tokens[fields_map.get("uid")];

    // Applies to all certificates ... probably needs more
    String header =  "timestamp"                       + "," + 
                     "timestamp_end"                   + "," + 
                     "certificate.key_alg"             + "," +
                     "certificate.sig_alg"             + "," +
                     "certificate.serial"              + "," +
                     "uid";

    String row    =  Utils.exactDate(not_valid_before)                             + "," +
                     Utils.exactDate(not_valid_after)                              + "," +
                     Utils.encToURL(tokens[fields_map.get("certificate.key_alg")])  + "," +
                     Utils.encToURL(tokens[fields_map.get("certificate.sig_alg")])  + "," +
                     Utils.encToURL(tokens[fields_map.get("certificate.serial")])   + "," +
                     uid;

    // Subject info
    List<String> sort = new ArrayList<String>(); Iterator<String> it = null;
    sort.clear(); sort.addAll(subject_map.keySet()); Collections.sort(sort);
    it = sort.iterator(); while (it.hasNext()) {
      String str = it.next();
      header += "," + "certificate.subject." + str;
      String to_cat = subject_map.get(str);
      if (Utils.allNumbers(to_cat)) { try { Integer.parseInt(to_cat); } catch (NumberFormatException nfe) { to_cat = "toobig-" + to_cat; } }
      row    += "," + Utils.encToURL(to_cat);
    }

    // Issuer info
    sort.clear(); sort.addAll(issuer_map.keySet()); Collections.sort(sort);
    it = sort.iterator(); while (it.hasNext()) {
      String str = it.next();
      header += "," + "certificate.issuer." + str;
      String to_cat = issuer_map.get(str);
      if (Utils.allNumbers(to_cat)) { try { Integer.parseInt(to_cat); } catch (NumberFormatException nfe) { to_cat = "toobig-" + to_cat; } }
      row    += "," + Utils.encToURL(to_cat);
    }

    // Save off for later output
    if (hdr_to_rows.containsKey(header) == false) hdr_to_rows.put(header, new ArrayList<String>());
    hdr_to_rows.get(header).add(row);
  }

  /**
   * Map linking addition info -- header goes to list of rows for that header
   */
  Map<String,List<String>> hdr_to_rows = new HashMap<String,List<String>>();

  /**
   * Separate a delimited field into its parts.
   *
   *@param str     string to parse
   *@param set_sep set separator character
   *
   *@return separated list
   */
  public List<String> separate(String str, char set_sep) {
    List<String> list = new ArrayList<String>();
    int i0 = 0, i1 = 1; boolean found = false;
    while (i0 < str.length()) {
      i1 = i0; found = false;
      while (i1 < (str.length()-1) && found == false) {
        i1++; // System.err.println("" + i0 + " - " + i1 + " \"" + str.substring(i0,i1) + "\"");
	if (str.charAt(i1) == set_sep) {
          if (i1 >=2 && str.charAt(i1-1) == '\\' && str.charAt(i1-2) == '\\') { /* escaped sequence */ } else {
	    list.add(str.substring(i0,i1));
	    found = true; i0 = i1+1;
	  }
	}
      }
      if (i1 >= (str.length()-1) && (i1 - i0) > 1) { list.add(str.substring(i0,i1+1)); i0 = i1 + 1; }
    }
    return list;
  }

  /**
   * Make a key-value map from a delimited field.
   *
   *@param str     string to parse
   *@param set_sep set separator character
   *
   *@return key value pairs
   */
  public Map<String,String> makeMap(String str, char set_sep) {
    // split into the keyval pairs... tricky because the commas may be escaped...
    List<String> keyvals = separate(str, set_sep);

    // Convert to a map
    Map<String,String> map = new HashMap<String,String>();
    for (int i=0;i<keyvals.size();i++) {
      String keyval = keyvals.get(i);
      map.put(keyval.substring(0,                       keyval.indexOf("=")),
              keyval.substring(keyval.indexOf("=") + 1, keyval.length()));
    }

    return map;
  }

  /**
   * Print tables from captured information.
   *
   *@param out print stream
   */
  public void printCapturedTables(PrintStream out) {
    Iterator<String> it_hdr = hdr_to_rows.keySet().iterator(); while (it_hdr.hasNext()) {
      out.println(""); String hdr = it_hdr.next(); out.println(hdr);
      List<String> rows = hdr_to_rows.get(hdr);
      for (int i=0;i<rows.size();i++) out.println(rows.get(i));
    }
  }

  /**
   * Convert a string with seconds (and possible fractions) to a long millis.
   */
  public long secondFractionsToMillis(String secs_str) {
    String secs_fraction_str = ""; 
    if (secs_str.indexOf(".") >= 0) { secs_fraction_str = secs_str.substring(secs_str.indexOf(".") + 1, secs_str.length());
                                      secs_str          = secs_str.substring(0,secs_str.indexOf(".")); }

    // Convert to a millisecond value
    long millis = Long.parseLong(secs_str) * 1000L;
    if        (secs_fraction_str.length() == 1) { millis += Long.parseLong(secs_fraction_str) * 100;
    } else if (secs_fraction_str.length() == 2) { millis += Long.parseLong(secs_fraction_str) * 10;
    } else if (secs_fraction_str.length() >= 3) { millis += Long.parseLong(secs_fraction_str.substring(0,3)); }

    return millis;
  }

  /**
   * Parse the separator characters.
   *
   *@param str string representation on separator \x09
   *
   *@return string of separator(s)
   */
  public String parseSeparator(String str) {
    if (str.startsWith("\\x")) {
      str = str.substring(2,str.length());
      char c = (char) Integer.parseInt(str, 16);
      return "" + c;
    } else { unknown_separator = parse_errors = true; throw new RuntimeException("Do Not Know How To Parse Separator \"" + str + "\""); }
  }

  /**
   * Main body...  show license, [parse params], normalize supplied files, print to stdout
   *
   *@param args input arguments
   */
  public static void main(String args[]) {
    // Print out the apache license info...
    System.err.println("License Information");
    System.err.println("");
    System.err.println("Copyright 2016 David Trimm");
    System.err.println("");
    System.err.println("Licensed under the Apache License, Version 2.0 (the \"License\");");
    System.err.println("you may not use this file except in compliance with the License.");
    System.err.println("You may obtain a copy of the License at");
    System.err.println("");
    System.err.println("http://www.apache.org/licenses/LICENSE-2.0");
    System.err.println("");
    System.err.println("Unless required by applicable law or agreed to in writing, software");
    System.err.println("distributed under the License is distributed on an \"AS IS\" BASIS,");
    System.err.println("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
    System.err.println("See the License for the specific language governing permissions and");
    System.err.println("limitations under the License.");
    System.err.println("");

    try {
      // Construct the normalizer
      NormalizeBroLogs normalizer = new NormalizeBroLogs();

      // Parse the options (nothing here yet)
      
      // Parse the files and dump the output
      for (int i=0;i<args.length;i++) {
        normalizer.normalize(new File(args[i]), System.out);
	if (i < args.length - 1) System.out.println(""); // blank line between headers
      }

      // Print out captured info
      normalizer.printCapturedTables(System.out);

      // Print out parse errors (if any existed)
      normalizer.printParseErrors(System.err);
    } catch (Throwable t) {
      System.err.println("Throwable: " + t);
      t.printStackTrace(System.err);
    }
  }
}

