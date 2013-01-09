package ru.classifier.server.rmi;

import ru.classifier.common.ObjectOperation;
import ru.classifier.util.Configuration;

import java.util.List;

/**
 * User: root
 * Date: 17.07.2008
 * Time: 18:56:48
 */
public class PoolListener implements Runnable{
  private final static long sleepTime = Configuration.getIntParam("server.poollistener.sleeptime", 7);

  private static int objectsProcessed = 0;

  public static int getObjectsProcessed() {
    return objectsProcessed;
  }

  private final List inPool;
  private final List outPool;
  private final ObjectOperation operation;

  public PoolListener(final List inPool, final List outPool, final ObjectOperation operation) {
    this.inPool = inPool;
    this.outPool  = outPool;
    this.operation = operation;
  }

  public void run() {
    while (true) {
      Object o = null;
      synchronized(inPool) {
        if (inPool.size() > 0)
          o = inPool.remove(0);
      }            
      if (o != null) {
        operation.process(o); // processing object
        synchronized (outPool) {
          outPool.add(o);
        }
        objectsProcessed ++;
        Thread.yield();   //  todo try without yield

      } else {
        try {
          Thread.sleep(sleepTime);
          continue;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
