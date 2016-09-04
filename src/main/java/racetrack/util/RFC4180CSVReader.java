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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import racetrack.framework.BundlesDT;

/**
 * Read a CSV encoded using the RFC4180 Standard file.  Caveats include
 * - Files ending with a .gz will be automatically unzipped
 * - blank fields will be replaced with the notset string
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class RFC4180CSVReader {
  /**
   * Consumer for the parsed tokens
   */
  private CSVTokenConsumer consumer;
  /**
   * Flag to indicate that parsing should continue
   */
  private boolean          keep_going = true;

  /**
   * Construct the reader and run it through the file.  Simplified version to handle backward compatibility.
   *
   *@param file         csv file to parse
   *@param consumer     consumer to direct tokens to
   */
  public RFC4180CSVReader(File file, CSVTokenConsumer consumer) throws IOException { this(file, consumer, null); }

  /**
   * Construct the reader and run it through the file.
   *
   *@param file         csv file to parse
   *@param consumer     consumer to direct tokens to
   *@param encoding     null if no decoding is specified; else the specified decoding string will be used (e.g., "UTF-8")
   */
  public RFC4180CSVReader(File file, CSVTokenConsumer consumer, String encoding) throws IOException {
    this.consumer = consumer; InputStream in = null; List<Byte> bytes = new ArrayList<Byte>();
    try {
      if (file.getName().toLowerCase().endsWith(".gz")) in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)), 1024*1024*8);
      else                                              in = new BufferedInputStream(new FileInputStream(file), 1024*1024*8);
      int line_no = 1; boolean in_dquotes = false, last_was_dquote = false; List<String> tokens = new ArrayList<String>(); StringBuffer sb = new StringBuffer(), line = new StringBuffer();
      while (in.available() > 0 && keep_going) {
        int c = in.read(); line.append((char) c);
	if        (c == -1)    { /* Shouldn't Happen    */ tokens.add(sb.toString()); pushTokens(tokens, line.toString(), line_no); sb.delete(0,sb.length()); line.delete(0,line.length()); tokens.clear(); line_no++;
	} else if (in_dquotes) {
	  if (last_was_dquote) {
	    if        (c == ',')  { /* End Token           */ addToken(tokens, sb, bytes, encoding); in_dquotes = false; last_was_dquote = false;
            } else if (c == '\r') { /* Ingore              */ in_dquotes = false; last_was_dquote = false;
            } else if (c == '\n') { /* End Token, End Line */ addToken(tokens, sb, bytes, encoding); pushTokens(tokens, line.toString(), line_no); line.delete(0,line.length()); tokens.clear(); in_dquotes = false; line_no++; last_was_dquote = false;
	    } else                { sb.append((char) c); bytes.add((byte) c); last_was_dquote = false; }
	  } else if (c == '\"') { last_was_dquote = true; 
	  } else                { sb.append((char) c); bytes.add((byte) c); }
	} else if (c == ',')   { /* End Token           */ addToken(tokens, sb, bytes, encoding);
	} else if (c == '\"')  { /* Enter Quotes        */ in_dquotes = true;
	} else if (c == '\r')  { /* Ignore              */
	} else if (c == '\n')  { /* End Token, End Line */ addToken(tokens, sb, bytes, encoding); pushTokens(tokens, line.toString(), line_no); line.delete(0,line.length()); tokens.clear(); line_no++;
	} else                 { sb.append((char) c); bytes.add((byte) c); }
      }
      in.close(); in = null;
    } catch (IOException ioe) { throw ioe;
    } finally                 { if (in != null) in.close();
    }
  }

  /**
   * Add a token -- if encoding is set, use the bytes list to decode the token.  Otherwise, use the stringbuffer version.  After token is added, clear both structures.
   */
  private void addToken(List<String> tokens, StringBuffer sb, List<Byte> bytes, String encoding) throws UnsupportedEncodingException {
    // System.err.print("sb = \"" + sb.toString() + "\" bytes.size() = " + bytes.size() + " :::: ");
    if (encoding == null) { tokens.add(sb.toString()); } else {
      byte as_array[] = new byte[bytes.size()]; 
      for (int i=0;i<as_array.length;i++) as_array[i] = bytes.get(i);
      tokens.add(new String(as_array, encoding));
    }
    // System.err.println("added token \"" + tokens.get(tokens.size()-1) + "\"");
    sb.delete(0,sb.length()); bytes.clear();
  }

  /**
   *
   */
  private void pushTokens(List<String> tokens, String line, int line_no) {
    String array[] = new String[tokens.size()]; 
    for (int i=0;i<array.length;i++) {
      array[i] = tokens.get(i);
      if (array[i] == null || array[i].length() == 0) array[i] = BundlesDT.NOTSET;
    }
    keep_going = consumer.consume(array, line, line_no);
  }

  /**
   *
   */
  public static void main(String args[]) {
    try {
      RFC4180CSVReader reader = new RFC4180CSVReader(new File(args[0]), new CSVTokenConsumer() {
        public boolean consume(String tokens[], String line, int line_no) {
	  System.out.println("@ " +line_no);
          for (int i=0;i<tokens.length;i++) System.out.println("  T[" + i + "] = \"" + tokens[i] + "\" (" + BundlesDT.getEntityDataType(tokens[i]) + ")");
          return true;
	} public void commentLine(String line) { } } );
    } catch (IOException ioe) {
      System.err.println("IOException: " + ioe);
    }
  }
}

