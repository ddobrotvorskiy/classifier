package ru.classifier.common;

import java.io.Serializable;
import java.util.Arrays;

/**
 * User: root
 * Date: 13.07.2008
 * Time: 23:28:44
 */
public class BytePoint implements Serializable {
  private final byte[] bytes;
  private final int x;
  private final int y;

  private int classId = 0;

  public BytePoint(int x, int y, int dim) {
    this.bytes = new byte[dim];
    this.x = x;
    this.y = y;
  }

  public BytePoint(int x, int y, final byte[] bytes) {
    this.bytes = bytes.clone();
    this.x = x;
    this.y = y;
  }

  public Point getPoint() {
    final int dim = bytes.length;
    final double[] data = new double[dim];
    for (int i = 0; i < dim; i++) {
      data[i] = (bytes[i] < 0 ? (256 + bytes[i]) : bytes[i]);
    }
    return Point.create(data, 1);
  }

  public byte[] getBytes() {
    return bytes;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getClassId() {
    return classId;
  }

  public void setClassId(int classId) {
    this.classId = classId;
  }

  public String toString(){
    String s = "[";
    for (int i = 0; i<bytes.length; i++)
      s += " " + (bytes[i] < 0 ? (256 + bytes[i]) : bytes[i]);
    s += "]";
    return s;
  }

  public int hashCode(){
    int result = 1;
    for (int i = 0 ; i < bytes.length ; i++) {
      result = 31 * result + bytes[i];
    }
    return result;
  }

  public boolean equals (final Object o) {
    if (o instanceof BytePoint)
      return Arrays.equals(bytes, ((BytePoint) o).bytes);
    return false;
  }
}
