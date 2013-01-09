package ru.classifier.server.rmi;

import ru.classifier.common.ObjectOperation;
import ru.classifier.util.Configuration;

import java.util.concurrent.BlockingQueue;

/**
 * User: root
 * Date: 17.07.2008
 * Time: 18:56:48
 */
public class PoolListener2 implements Runnable{
  private final static long sleepTime = Configuration.getIntParam("server.poollistener.sleeptime", 7);

  private static int objectsProcessed = 0;

  public static int getObjectsProcessed() {
    return objectsProcessed;
  }

  private final BlockingQueue inPool;
  private final BlockingQueue outPool;
  private final ObjectOperation operation;

  public PoolListener2(final BlockingQueue inPool, final BlockingQueue outPool, final ObjectOperation operation) {

    this.inPool = inPool;
    this.outPool  = outPool;
    this.operation = operation;
  }

  public void run() {
    while (true) {
      try {
        Object o = inPool.take();
        operation.process(o); // processing object
        outPool.put(o);
        objectsProcessed ++;
      } catch (InterruptedException e) {}
    }
  }
}
