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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import racetrack.framework.BundlesDT;

/**
 * Read a CSV file.  Caveats include
 * - Files ending with a .gz will be automatically unzipped
 * - blank fields will be replaced with the notset string
 * - strings are decoded by using (sortof) URL encoding.  For the most part,
 *   this means that %xx symbols are escapes to be decoded as hex characters
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class CSVReader {
  /**
   * Construct the reader and run it through the file.
   *
   *@param file     csv file to parse
   *@param consumer consumer to direct tokens to
   */
  public CSVReader(File file, CSVTokenConsumer consumer) throws IOException {
    this(file, consumer, ",");
  }

  /**
   * Construct the reader and run it through the file.
   *
   *@param file         csv file to parse
   *@param consumer     consumer to direct tokens to
   *@param delim        specific delimiter to use
   */
  public CSVReader(File file, CSVTokenConsumer consumer, String delim) throws IOException {
    this(file, consumer, delim, false);
  }

  /**
   * Construct the reader and run it through the file.
   *
   *@param file         csv file to parse
   *@param consumer     consumer to direct tokens to
   *@param delim        specific delimiter to use
   *@param strip_spaces remove spaces at the beginning and ending of the tokens
   */
  public CSVReader(File file, CSVTokenConsumer consumer,String delim, boolean strip_spaces) throws IOException {
    BufferedReader in = null;
    try {
      if (file.getName().toLowerCase().endsWith(".gz")) in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
      else                                              in = new BufferedReader(new FileReader(file));
      int            line_no = 1;
      String         line;
      boolean        keep_parsing = true;
      while ((line = in.readLine()) != null && keep_parsing) { line_no++;
        if (line.startsWith("#")) { consumer.commentLine(line); continue; }
	// Put the tokens into an array list
        List<String> al = new ArrayList<String>();
	StringTokenizer st = new StringTokenizer(line, delim, true); 
	while (st.hasMoreTokens()) al.add(st.nextToken());
        if (al.size() > 0) {
          if (al.get(0).indexOf(delim)>=0)           al.add(0,BundlesDT.NOTSET);
	  if (al.get(al.size()-1).indexOf(delim)>=0) al.add(BundlesDT.NOTSET);
	  int i = 1;
	  while (i < al.size()-1) {
            if (al.get(i).indexOf(delim)>=0 && al.get(i+1).indexOf(delim)>=0) al.add(i+1,BundlesDT.NOTSET);
	    i++;
	  }
	  String tokens[] = new String[al.size()/2 + 1];
	  for (i=0;i<tokens.length;i++) {
	    tokens[i] = Utils.decFmURL(al.get(2*i));
	    if (strip_spaces) {
              tokens[i] = Utils.stripSpaces(tokens[i]);
              if (tokens[i].equals("")) tokens[i] = BundlesDT.NOTSET;
            }
          }
	  keep_parsing = consumer.consume(tokens, line, line_no);
	} else keep_parsing = consumer.consume(new String[0], line, line_no);
      }
      in.close();
    } catch (IOException ioe) { throw ioe;
    } finally                 { if (in != null) in.close();
    }
  }
}
