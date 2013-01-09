package ru.classifier;

import ru.classifier.common.BytePoint;
import ru.classifier.common.BytePointMatrix;
import ru.classifier.common.ClassFactory;
import ru.classifier.common.ParzenClass;
import ru.classifier.thread.ClassifierPoolListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * User: root
 * Date: 13.07.2008
 * Time: 23:26:07
 */
public class Manager {


  private final static List BYTE_POINTS_POOL = new LinkedList();
  private final static HashMap HASH_MAP = new HashMap();

  public static void main1(String[] args) {
    final byte[] p1 = new byte[8];
    final byte[] p2 = new byte[8];

    for (byte i = 0; i<8; i++) {
      p1[i] = i;
      p2[i] = i;
    }

    System.out.println("\np1 " + (p1.equals(p2) ? "="  : "<>") + " p2");
    System.out.println("\np1 " + p1.hashCode() + " p2 " + p2.hashCode());

  }

  public static void main(String[] args) {
    final String dataFileName = args[0];
    final String teachFileName = args[1];

    try {
      final BytePoint[][] dataMatrix = BytePointMatrix.create(dataFileName);

      final ParzenClass pc = ClassFactory.create(teachFileName, dataMatrix);
      pc.initClassifier();
      System.out.print(pc.getClassInfo("  ", true));

      //System.exit(0);

      final Thread poolListener1 = new Thread(new ClassifierPoolListener(BYTE_POINTS_POOL, HASH_MAP, pc));
      final Thread poolListener2 = new Thread(new ClassifierPoolListener(BYTE_POINTS_POOL, HASH_MAP, pc));
      poolListener1.setDaemon(true);
      poolListener2.setDaemon(true);




      int doubles = 0;
      int total = 0;
      for (int i = 0; i< dataMatrix.length; i++) {
        for (int j = 0; j< dataMatrix[i].length; j++){
          total ++;
          final BytePoint p = dataMatrix[i][j];
          if (!HASH_MAP.containsKey(p)) {
            synchronized(HASH_MAP) {
              HASH_MAP.put(p, null);
            }
            BYTE_POINTS_POOL.add(p);
          } else {
            doubles ++;
            final Integer id = (Integer) HASH_MAP.get(p);
            if (id != null)
              p.setClassId(id.intValue());
          }
          Thread.yield();
        }
      }

      poolListener1.start();
      poolListener2.start();
      System.out.print("\nManager : ClassifierPoolListener started");
      final long startTime = System.currentTimeMillis();

      System.out.print("\nManager : Total points = " + total + "  repeated points = " + doubles);
      System.out.print("\nManager : Pool filled in " + (System.currentTimeMillis() - startTime) + " ms");
      System.out.print("\nManager : Pool size = " + BYTE_POINTS_POOL.size());
      System.out.print("\nManager : Waiting for ClassifierPoolListener");

      // waiting for classificators
      long count = 0;
      long oldCount = 0;
      long time = 0;
      final long sleepTime = 6000;
      while (BYTE_POINTS_POOL.size() > 0) {
        try {
          Thread.sleep(sleepTime);
          System.out.print("\nManager : Pool size = " + BYTE_POINTS_POOL.size());
          count = ClassifierPoolListener.getPointsProcessed();
          double speed = ((double) 1000.0 * (count - oldCount)/ sleepTime);
          oldCount = count;
          System.out.print("  Speed  = " + speed + " points per second");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      long totalTime = (System.currentTimeMillis() - startTime);
      double speed = ((double) ClassifierPoolListener.getPointsProcessed())/ totalTime;

      System.out.print("\nManager : Pool processed in " + totalTime  + " ms");
      System.out.print("\nManager : Average speed  = " + speed + " points per second");

      //set classes for repeated points
      for (int i = 0; i< dataMatrix.length; i++) {
        for (int j = 0; j< dataMatrix[i].length; j++){
          final BytePoint p = dataMatrix[i][j];
          if (p.getClassId() > 0)
            continue;
          final Integer id = (Integer) HASH_MAP.get(p);
          p.setClassId(id.intValue());
        }
      }

      final String outFileName = "d:\\src\\data\\result.txt";
      System.out.print("\nManager : saving result to " + outFileName);

      BytePointMatrix.save(dataMatrix, teachFileName, outFileName);

      System.out.print("\nManager : exit");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
