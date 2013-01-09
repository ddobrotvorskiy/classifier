package ru.classifier.common;

import Jama.Matrix;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Class Point represents point with double coordinates
 * <br>
 * All instances are IMMUTABLE.
 * <br>
 * Class is serializable. Default java serialization used.
 *
 * <pre>
 * User: root
 * Date: 06.07.2008
 * Time: 21:39:47
 * </pre>
 */

public final class Point implements Serializable {

  private final double[] data;
  private final int weight;

  private Point(double [] data, int weight) {
    if (data == null || weight < 1)
      throw new IllegalArgumentException();

    this.weight = weight;
    this.data = data;
  }

  public static Point create(double [] data, int weight) {

    double [] a = new double[data.length];
    System.arraycopy(data, 0, a, 0, data.length);

    return new Point(a, weight);
  }

  public double[] getArray() {
    double [] a = new double[data.length];
    System.arraycopy(data, 0, a, 0, data.length);
    return a;
  }

  public int getDim() {
    return data.length;
  }

  public int getWeight() {
    return weight;
  }

  public double get(int i) {
    return data[i];
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append('[');
    for (double d : data)
      s.append(d).append(' ');
    s.append("; weight = ").append(weight).append(']');
    return s.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if ( ! (o instanceof Point)) return false;

    Point point = (Point) o;

    if (weight != point.weight) return false;
    if (!Arrays.equals(data, point.data)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = data != null ? Arrays.hashCode(data) : 0;
    result = 31 * result + weight;
    return result;
  }

  /**
   * Returns point recounted in new features
   *
   * @param features - features matrix
   * @return point recounted in new features
   */
  public Point recount(Matrix features) {
    if (data.length != features.getRowDimension())
      throw new IllegalArgumentException("Features row dimension " + features.getRowDimension() +
              " is not equal point dimension " + data.length);

    int dim = features.getColumnDimension();
    double [][] A = features.getArray();
    double [] data = new double[dim];

    for (int i = 0; i < dim; i++) {
      double val = 0.;
      for (int j = 0; j < data.length ; j++)
        val += data[j] * A[j][i];
      data[i] = val;
    }

    return new Point(data, weight);
  }

  /**
   * Returns nearest of passed points
   * Returns null if no points passed
   *
   * @param points list of points
   * @return nearest of passed points
   */
  public Point nearest(List<Point> points) {
    if (points == null || points.isEmpty())
      return null;

    double minDist = Double.MAX_VALUE;
    Point nearestPoint = null;
    for (Point p : points) {
      final double d = distance(this, p);
      if (d < minDist) {
        minDist = d;
        nearestPoint = p;
      }
    }
    return nearestPoint;
  }

  /**
   * Euclidean distance between two points p1, p2.
   * Points should have the same dimension.
   * Points should be not null.
   *
   * @param p1 point
   * @param p2 point
   * @return euclidean distance between p1 and p2
   * @throws IllegalArgumentException if points have different dimension
   */
  public static double distance(Point p1, Point p2) {
    if (p1 == p2)
      return 0.0d;

    if (p1.data.length != p2.data.length)
      throw new IllegalArgumentException("Incomparable points: " + p1 + "  " + p2);

    double d = 0.0d;
    for (int i = 0; i < p1.data.length; i++ )
      d += Math.pow(p1.data[i] - p2.data[i], 2.0d);

    return Math.sqrt(d);
  }

  /**
   * Returns mean point of passed points.
   * Weight of result is equal to passed points count.
   *
   * points should not be null.
   * points should not be empty.
   * all passed points should have equal dimension
   *
   *
   * @param points list of points
   * @return mean of passed point with weight equal to points.size()
   * @throws IllegalArgumentException if points is empty
   *         IllegalArgumentException if passed points have different dimension
   */
  public static Point mean(List<Point> points) {
    if (points.isEmpty())
      throw new IllegalArgumentException("Argument must be non-empty list");

    if (points.size() == 1)
      return points.get(0);

    int dim = points.get(0).getDim();
    int weight = points.size();
    double [] data = new double[dim];

    for (int i = 0; i< dim; i++)
      data[i] = 0;

    for (Point p : points) {
      if (p.getDim() != dim)
        throw new IllegalArgumentException("All points should have equal dimension");

      for (int i = 0; i< dim; i++)
        data[i] += p.data[i];
    }

    for (int i = 0; i< dim; i++)
      data[i] /= (double) weight;

    return new Point(data, weight);
  }

  /**
   * Returns median of passed points.
   * Weight of result is 1.
   *
   * Points should not be null.
   * Points should have equal dimension.
   *
   *
   * @param p1 point
   * @param p2 point
   * @return median of passed points
   * @throws IllegalArgumentException if passed points have different dimension
   */
  public static Point median(Point p1, Point p2) {
    if (p1 == p2)
      return p1;

    if (p1.getDim() != p2.getDim())
      throw new IllegalArgumentException("Points has different dimensions");

    double [] data = new double[p1.data.length];

    for (int i = 0; i < data.length; i++)
      data[i] = (data[i] + p2.data[i]) / 2.0;

    return new Point(data, 1);
  }


}
