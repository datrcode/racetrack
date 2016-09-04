/* 

Copyright 2015 David Trimm

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

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import racetrack.util.CSVTokenConsumer;
import racetrack.util.RFC4180CSVReader;
import racetrack.util.Utils;

/**
 * Class to import an RFC4180 formatted csv file.  Originally part of the RTControlPanel class,
 * re-instantiated within the framework to limit accesss to the framework variables.
 */
public class RFC4180Importer implements CSVTokenConsumer {
  /**
   * Flag to indicate that the file has a header which should be skipped during parsing.
   */
  boolean file_has_header;

  /**
   * Labels for the fields within the file
   */
  String  labels[];

  /**
   * Flag to indicate if the field is a scalar value.
   */
  boolean scalars[],
  /**
   * Flag to indicate if the field should be ignored.
   */
          ignores[],
  /**
   * Flag to indicate if the field should be converted to lowercase (for normalization).
   */
          to_lowercase[];

  /**
   * Flag to indicate that URL decoding should be applied to the field
   */
  boolean url_decode,
  /**
   * Flag to indicate if spaces should be stripped at the beginning and the ending of the field.
   */     strip_spaces;

  /**
   * Construct the importer by consuming the file's specification and then parses the file and updates
   * the application's data structure.
   *
   *@param file       file to parse
   *@param header     file has a header line that should not be parsed
   *@param bundles    primary data structure for the new records
   *@param ts_i0      index of the timestamp field, negative one if not present
   *@param ts_i1      index of the end timestamp file, negative one if not present
   *@param labs       field headers for the columns in the file
   *@param scales     indicates that the field is a scalar
   *@param igns       indicates that the field should be ignored
   *@param lower      indicates that the field should be lowercased
   *@param udecodes   flag to url decode the fields
   *@param strip_spcs flag to strip spaces at the beginning and ending of the fields
   *@param encoding   encoding information
   */
  public RFC4180Importer(File    file,
                         boolean header,
                         Bundles bundles, int ts_i0, int ts_i1, 
                         String  labs[], 
                         boolean scales[],
                         boolean igns[], 
			 boolean lower[],
			 boolean udecodes,
			 boolean strip_spcs,
                         String  encoding) throws IOException {
      // Retain the settings
      this.timestamp_i     = ts_i0;
      this.timestamp_end_i = ts_i1;
      this.file_has_header = header;
      this.labels          = labs;
      this.scalars         = scales;
      this.ignores         = igns;
      this.url_decode      = udecodes;
      this.to_lowercase    = lower;
      this.strip_spaces    = strip_spcs;

      // Get the framework variables
      BundlesG globals = bundles.getGlobals();

      // Create the tablet
      tablet = bundles.createTablet();
      if (timestamp_i     != -1) tablet.setHasTimeStamps();
      if (timestamp_end_i != -1) tablet.setHasDurations();
      int j = 0; for (int i=0;i<labels.length;i++) {
        if (ignores[i] || i == timestamp_i || i == timestamp_end_i) continue;
        int fld_i = globals.getOrCreateField(labels[i], Utils.isAllUpper(labels[i]));
	tablet.setFieldIndex(fld_i, j);
	j++;
      }
      // Check to see if another tablet already matches this one, use that instead
      Iterator<Tablet> it_tab = bundles.tabletIterator(); boolean found_match = false;
      while (it_tab.hasNext() && found_match == false) {
        Tablet compare = it_tab.next();
        if (compare.equals(tablet)) { found_match = true; tablet = compare; }
      }
      if (found_match == false) bundles.add(tablet);
      // Prepare the parser
      first_line_flag = true; caveats = new HashSet<String>();
      // Parse the file
      RFC4180CSVReader reader = new RFC4180CSVReader(file,this,encoding);
      // Reset the transforms to force the lookups to be created
      System.err.println("**\n** Probably Need To Include Cached Bundles...\n**");
      Set<Bundles> bundles_set = new HashSet<Bundles>(); bundles_set.add(bundles);
      globals.cleanse(bundles_set);
      // Print out any info messages that occurred
      if (caveats.size() > 0) {
        Iterator<String> it = caveats.iterator();
	System.err.println("Caveats Identified During Parsing:");
	while (it.hasNext()) System.err.println("  " + it.next());
      }
  }

  /**
   * Tablet to import the data items into
   */
  Tablet  tablet;

  /**
   * Index of the timestamp field (or -1 if not set)
   */
  int     timestamp_i     = -1,
  /**
   * Index of the timestamp end field (or -1 if not set)
   */
          timestamp_end_i = -1;
  /**
   * Flag indicating that the first line has not been found yet
   */
  boolean first_line_flag = true;

  /**
   * Errors / issues identified as the parser went through the file
   */
  Set<String> caveats = new HashSet<String>();

  /**
   * Consume a line of CSV input.  This provides the dialog with information about the headers
   * and fields.
   *
   *@param tokens     tokens from the line
   *@param line       complete line from the file
   *@param line_no    line number
   *
   *@return true to keep parsing
   */
  public boolean consume(String tokens[], String line, int line_no) {
      if ((line_no%10000) == 0) System.err.println("Importing Excel-CSV (Line " + line_no + ")"); // Provide some feedback... should be a progress bar
      if (tokens.length == labels.length) {
        if (first_line_flag && file_has_header) { first_line_flag = false; } else { first_line_flag = false;
          Map<String,String> attr = new HashMap<String,String>(); boolean valid = true; String reason = "", timestamp = null, timestamp_end = null;
          long ts0 = -1L, ts1 = -1L;
          for (int i=0;i<labels.length;i++) {
            if (ignores[i]) continue;
	    String str = tokens[i];
            // Check for extremely long tokens -- could indicate parse error
            if (tokens[i].length() > 1024) caveats.add("Excessively long token (> 1024 chars) discovered");
	    // Apply lowercase
            if (to_lowercase[i]) str = str.toLowerCase();
	    // Apply strip spaces
	    if (strip_spaces)    str = Utils.stripSpaces(str);
	    // Apply URL decode
            if (url_decode)      str = Utils.decFmURL(str);
	    // Check timestamp parsing
	    if        (i == timestamp_i)      { timestamp     = tokens[timestamp_i];
	      try { ts0 = Utils.parseTimeStamp(tokens[i]); } catch (Throwable t) { valid = false; reason += "Timestamp Not Parseable";     } }
	    // Check timestamp_end parsing
	    if (i == timestamp_end_i)  { timestamp_end = tokens[timestamp_end_i];
	      try { ts1 = Utils.parseTimeStamp(tokens[i]); } catch (Throwable t) { valid = false; reason += "Timestamp End Not Parseable"; } }
	    // Check for integers that are too large
	    if (Utils.appearsToBeInteger(str)) {
	      try { Integer.parseInt(str); } catch (NumberFormatException nfe) { 
                if (scalars[i]) {
                  if (str.startsWith("-")) { str = "" + Integer.MIN_VALUE; caveats.add("Excessively small integers capped at the negative max"); }
		  else                     { str = "" + Integer.MAX_VALUE; caveats.add("Excessively large integers capped at the positive max"); }
                } else str = "x" + str;
	      }
	    }
	    // Check for blank scalar fields -- set those to 0
	    if (scalars[i] && str.equals(""))          { str = "0"; }
            // Check for scalar fields that have non-numbers (recall that previously we capped it at the max if it looked like a number -- in this case, it's not a number)
	    if (scalars[i]) {
	      try { Integer.parseInt(str); } catch (NumberFormatException nfe) {
	        valid = false; reason += "Token[" + i + "] = \"" + str + "\" Not Parseable As Integer";
	      }
	    }
	    // Check for a blank field
	    if (str.equals(""))                                { str = BundlesDT.NOTSET; }

            if (i != timestamp_i && i != timestamp_end_i) attr.put(labels[i], str);
	  }
          if (ts0 != -1 && ts1 != -1 && ts1 < ts0) caveats.add("Some Durations Are Negative");
	  if (valid) {
            Bundle bundle = null;
            try {
              if      (timestamp_i != -1 && timestamp_end_i != -1) bundle = tablet.addBundle(attr,timestamp,timestamp_end);
	      else if (timestamp_i != -1)                          bundle = tablet.addBundle(attr,timestamp);
	      else                                                 bundle = tablet.addBundle(attr);
            } catch (Throwable t) { System.err.println("Error Adding Bundle With Following Characteristics\n" + attr); }
	    if (bundle == null) {
	      System.err.println("Tablet Bundle Add Unsuccessful @ Line " + line_no);
	      caveats.add("Unsuccessful Tablet Bundle Adds");
	    }
	  } else { System.err.println("Skipping Line " + line_no + " | \"" + Utils.shorten(line, 25) + "\" | Reason: " + reason); caveats.add(reason); }
	}
      } else { System.err.println("Skipping Line " + line_no + "| Incorrect Token Count (" + tokens.length +") | \"" + Utils.shorten(line, 25) + "\""); caveats.add("Not Correct Number Of Tokens"); }
      return true;
  }

  /**
   * Handle (or rather don't handle) comment lines.
   *
   *@param line
   */
  public void    commentLine(String line) { }
}

