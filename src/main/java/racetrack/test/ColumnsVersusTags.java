/* 

Copyright 2014 David Trimm

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
package racetrack.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Generates test files that compare the performance of the application when columns and tags are
 * to store the same information.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class ColumnsVersusTags {
  /**
   * Create four files -- all columns, all tags, and a mix of columns and tags.
   */
  public static void main(String args[]) {
    final int num_of_recs = 40000,
              num_of_cols = 20;

    // Make the header
    String header[] = new String[num_of_cols];
    for (int i=0;i<header.length;i++) header[i] = randomString(10);

    // Make the data
    String data[][] = new String[num_of_recs][num_of_cols];
    for (int i=0;i<data.length;i++) for (int j=0;j<data[i].length;j++) data[i][j] = randomString(4);

    // Make the last couple of columns sparse
    for (int j=0;j<data.length;j++) {
      if (Math.random() < 0.8) data[j][num_of_cols-1] = null;
      if (Math.random() < 0.5) data[j][num_of_cols-2] = null;
    }

    // Write to a file
    try {
      File cols_file = new File("cvt_cols.csv"),
           mix1_file = new File("cvt_mix1.csv"),
	   mix2_file = new File("cvt_mix2.csv"),
	   tags_file = new File("cvt_tags.csv");
      PrintStream out;

      // All Columns First
      out = new PrintStream(new FileOutputStream(cols_file));
      for (int i=0;i<header.length;i++) { if (i != 0) out.print(","); out.print(header[i]); }
      out.println("");
      for (int i=0;i<data.length;i++) {
        for (int j=0;j<data[i].length;j++) {
	  if (j != 0) out.print(",");
	  if (data[i][j] != null) out.print(data[i][j]);
	}
        out.println("");
      }
      out.close();

      // All Tags Next
      out = new PrintStream(new FileOutputStream(tags_file));
      out.println("tags");
      for (int i=0;i<data.length;i++) {
        StringBuffer tags = new StringBuffer();
        for (int j=0;j<data[i].length;j++) {
	  if (data[i][j] != null && tags.length() > 0) tags.append("|");
	  if (data[i][j] != null) tags.append(header[j] + "=" + data[i][j]);
	}
        out.println(tags.toString());
      }
      out.close();

      // Mix1 Next (3/4 columns, 1/4 tags)
      int tags_i = (num_of_cols*3)/4;
      out = new PrintStream(new FileOutputStream(mix1_file));
      for (int i=0;i<tags_i;i++) { if (i != 0) out.print(","); out.print(header[i]); } out.println(",tags");
      for (int i=0;i<data.length;i++) {
        for (int j=0;j<tags_i;j++) {
	  if (j != 0) out.print(",");
	  if (data[i][j] != null) out.print(data[i][j]);
        }
        StringBuffer tags = new StringBuffer();
        for (int j=tags_i;j<data[i].length;j++) {
	  if (data[i][j] != null && tags.length() > 0) tags.append("|");
	  if (data[i][j] != null) tags.append(header[j] + "=" + data[i][j]);
	}
        out.println("," + tags.toString());
      }
      out.close();

      // Mix2 Next (half columns, half tags)
      tags_i = num_of_cols/2;
      out = new PrintStream(new FileOutputStream(mix2_file));
      for (int i=0;i<tags_i;i++) { if (i != 0) out.print(","); out.print(header[i]); } out.println(",tags");
      for (int i=0;i<data.length;i++) {
        for (int j=0;j<tags_i;j++) {
	  if (j != 0) out.print(",");
	  if (data[i][j] != null) out.print(data[i][j]);
        }
        StringBuffer tags = new StringBuffer();
        for (int j=tags_i;j<data[i].length;j++) {
	  if (data[i][j] != null && tags.length() > 0) tags.append("|");
	  if (data[i][j] != null) tags.append(header[j] + "=" + data[i][j]);
	}
        out.println("," + tags.toString());
      }
      out.close();



    } catch (IOException ioe) {
      System.err.println("IOException: " + ioe);
    }
  }


  /**
   * Create a random string of the specified number of characters.  Use a-z only to avoid parsing issues.
   */
  public static String randomString(int chars) {
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<chars;i++) {
      char c = ((char) ('a' + ((int) (52*Math.random()))%26));
      sb.append(c);
    }
    return sb.toString();
  }
}

