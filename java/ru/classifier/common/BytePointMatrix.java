package ru.classifier.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * User: root
 * Date: 13.07.2008
 * Time: 23:39:29
 */
public class BytePointMatrix {

  public static BytePoint[][] create(final String txtFileName) throws IOException {
    final String hdrFileName = txtFileName.substring(0, txtFileName.length() - 3) + "hdr";

    System.out.println("\nhdr filename = " + hdrFileName);

    final FileInputStream headerInput = new FileInputStream(hdrFileName);
    final Properties properties = new Properties();
    properties.load(headerInput);
    headerInput.close();

    final int lines = Integer.valueOf(properties.getProperty("lines")).intValue();
    final int samples = Integer.valueOf(properties.getProperty("samples")).intValue();
    final int bands = Integer.valueOf(properties.getProperty("bands")).intValue();
    final String interleave = properties.getProperty("interleave");

    System.out.println("\nlines = " + lines + "\nsamples = " + samples +
                      "\nbands = " + bands + "\ninterleave = " + interleave);

    final FileInputStream input = new FileInputStream(txtFileName);
    final BytePoint[][] matrix = new BytePoint[lines][samples];

    if ("bip".equalsIgnoreCase(interleave)) {              /// BIP
      final byte[] buffer = new byte[bands];
      for (int i = 0; i<lines; i++) {
        for (int j = 0; j<samples; j++) {
          if (bands != input.read(buffer))
            throw new IOException("file corrupted");
          matrix[i][j] = new BytePoint(i, j, buffer);
        }
      }
    } else if ("bsq".equalsIgnoreCase(interleave)) {       /// BSQ
      final byte[] line = new byte[samples];

      for (int i = 0; i<lines; i++)
        for (int j = 0; j<samples; j++)
          matrix[i][j] = new BytePoint(i, j, bands);

      for (int i = 0; i<bands; i++)
        for (int j = 0; j<lines; j++) {
          if (samples != input.read(line))
            throw new IOException("file corrupted");
          for (int k = 0; k<samples; k++)
            matrix[j][k].getBytes()[i] = line[k];
        }
    }
    input.close();

    return matrix;
  }

  public static void save(final BytePoint[][] maxrix, final String teachFileName, final String outFileName) throws IOException {
    final String hdrFileName = outFileName.substring(0, outFileName.length() - 3) + "hdr";
    final String teachHdrFileName = teachFileName.substring(0, teachFileName.length() - 3) + "hdr";

    // Copying teach hdr file
    {
      final FileInputStream inStream = new FileInputStream(teachHdrFileName);
      final FileOutputStream outStream = new FileOutputStream(hdrFileName);

      final byte [] buffer = new byte[4096];
      while(true) {
        int len = inStream.read(buffer);
        outStream.write(buffer, 0, len);
        if (len < buffer.length)
          break;
      }
      outStream.close();
      inStream.close();
    }
    { // creating txt file
      final FileOutputStream output = new FileOutputStream(outFileName);
      for (int i = 0; i<maxrix.length; i++)
        for (int j = 0; j<maxrix[i].length; j++)
          output.write(maxrix[i][j].getClassId());
      output.flush();
      output.close();
    }
  }


  public static void main(String[] args) {
    try {
      final BytePoint[][] matrix = BytePointMatrix.create(args[0]);

      System.out.println("\nPoint at 0,0 =" + matrix[0][0]);

    } catch (IOException e) {
      System.out.print("Error " + e.getMessage());
      e.printStackTrace();
    }
  }
}
