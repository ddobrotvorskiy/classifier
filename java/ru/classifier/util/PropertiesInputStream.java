package ru.classifier.util;

import java.io.*;

public class PropertiesInputStream extends InputStream {
  private static final String specialSaveChars = "\r\n\f";

  private BufferedReader inReader;
  private byte[] buffer;
  private int position = 0;
  private boolean isEndFile = false;

  public PropertiesInputStream(final String fileName, final String encoding) throws IOException {
    inReader = new java.io.BufferedReader(new java.io.InputStreamReader(new BufferedInputStream(new FileInputStream(fileName)), encoding));
  }

  private void loadLine() throws IOException {
    position = 0;
    final String line = inReader.readLine();
    if (line == null) {
      isEndFile = true;
      return;
    }
    buffer = (convert(line, false) + "\n").getBytes();
  }

  public int read() throws IOException {
    while (!isEndFile) {
      if (buffer != null && buffer.length > 0 && position < buffer.length) {
        position++;
        return (int)buffer[position - 1];
      }
      loadLine();
    }
    return -1;
  }

  /*
   * Converts unicodes to encoded &#92;uxxxx
   * and writes out any of the characters in specialSaveChars
   * with a preceding slash
   */
  public String convert(final String theString, final boolean escapeSpace) {
    final int len = theString.length();
    final StringBuffer outBuffer = new StringBuffer(len * 2);

    for (int x = 0; x < len; x++) {
      final char aChar = theString.charAt(x);
      switch (aChar) {
        case ' ':
          if (x == 0 || escapeSpace)
            outBuffer.append('\\');

          outBuffer.append(' ');
          break;
        case '\\':
          outBuffer.append('\\');
          outBuffer.append('\\');
          break;
        case '\t':
          outBuffer.append('\\');
          outBuffer.append('t');
          break;
        case '\n':
          outBuffer.append('\\');
          outBuffer.append('n');
          break;
        case '\r':
          outBuffer.append('\\');
          outBuffer.append('r');
          break;
        case '\f':
          outBuffer.append('\\');
          outBuffer.append('f');
          break;
        default:
          if ((aChar < 0x0020) || (aChar > 0x007e)) {
            outBuffer.append('\\');
            outBuffer.append('u');
            outBuffer.append(toHex((aChar >> 12) & 0xF));
            outBuffer.append(toHex((aChar >> 8) & 0xF));
            outBuffer.append(toHex((aChar >> 4) & 0xF));
            outBuffer.append(toHex(aChar & 0xF));
          } else {
            if (specialSaveChars.indexOf(aChar) != -1)
              outBuffer.append('\\');
            outBuffer.append(aChar);
          }
      }
    }
    return outBuffer.toString();
  }

  /**
   * Convert a nibble to a hex character
   * @param     nibble  the nibble to convert.
   */
  private char toHex(final int nibble) {
    return hexDigit[(nibble & 0xF)];
  }

  /** A table of hex digits */
  private final char[] hexDigit = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };
}

