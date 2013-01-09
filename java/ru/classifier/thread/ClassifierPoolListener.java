package ru.classifier.thread;

import ru.classifier.common.AbstractClass;
import ru.classifier.common.BytePoint;
import ru.classifier.common.Point;

import java.util.HashMap;
import java.util.List;

/**
 * User: root
 * Date: 17.07.2008
 * Time: 18:56:48
 */
public class ClassifierPoolListener implements Runnable{
  private final static long sleepTime = 100;

  private final static Integer lock = new Integer(0);
  private static int pointsProcessed = 0;

  public static int getPointsProcessed() {
    return pointsProcessed;
  }

  private final List pool;
  private final AbstractClass c;
  private final HashMap hashMap;

  public ClassifierPoolListener(final List pool, final HashMap hashMap, final AbstractClass c) {
    this.pool = pool;
    this.c  = c;
    this.hashMap = hashMap;
  }

  public void run() {
    while (true) {
      if (pool.size() > 0) {
        final BytePoint bytePoint;
        synchronized(pool) {
          bytePoint = (BytePoint) pool.remove(0);
        }
        final long startTime = System.currentTimeMillis();

        final Point p  = bytePoint.getPoint();
        final int id = c.classify(p);
        bytePoint.setClassId(id);
        synchronized (hashMap) {
          hashMap.put(bytePoint, new Integer(id));
        }
        pointsProcessed++;
        //System.out.print("\nPoint " + bytePoint + "processed in " + (System.currentTimeMillis() - startTime) + " ms");
        Thread.yield();
      } else {
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
