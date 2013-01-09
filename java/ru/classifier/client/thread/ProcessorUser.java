package ru.classifier.client.thread;

import ru.classifier.server.rmi.Processor;
import ru.classifier.util.Configuration;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;

/**
 * User: root
 * Date: 06.04.2008
 * Time: 21:45:27
 */
public class ProcessorUser {

  private final static long SLEEP_TIME = Configuration.getIntParam("client.processoruser.sleeptime", 7);
  private final static long MAX_BUFFER_SIZE =Configuration.getIntParam("client.processoruser.buffersize", 30);

  private final List inPool;
  private final List outPool;
  private final Processor processor;

  private final HashSet buffer = new HashSet();

  public ProcessorUser(final List inPool, final List outPool, final Processor processor) {
    super();
    this.inPool = inPool;
    this.outPool = outPool;
    this.processor = processor;
  }

  public void start() {
    final Thread putter = new Thread(new Putter());
    putter.setDaemon(true);
    putter.start();

    final Thread getter = new Thread(new Getter());
    getter.setDaemon(true);
    getter.start();
  }

  private class Putter implements Runnable {
    public void run() {
      try {
        while(true) {

          if (buffer.size() < MAX_BUFFER_SIZE) {
            Object o = null;
            synchronized(inPool) {
              if (inPool.size() > 0)
                o = inPool.remove(0);
            }
            if (o != null) {
              synchronized(buffer) {
                buffer.add(o);
              }
              processor.put(o);
              Thread.yield();   //  todo try without yield
            } else {
              try {
                Thread.sleep(SLEEP_TIME);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          } else {
            try {
              Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      } catch(RemoteException e) {
        synchronized(inPool) {     // rollback all processing objects to inPool
          inPool.addAll(buffer);
        }
        e.printStackTrace();
      }
    }
  }

  private class Getter implements Runnable {
    public void run() {
      try {
        while(true) {
          Object o = processor.get();
          if (o != null) {
            synchronized(buffer) {
              buffer.remove(o);
            }
            synchronized(outPool) {
              outPool.add(o);
            }
            Thread.yield();   //  todo try without yield
          } else {
            try {
              Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      } catch(RemoteException e) {
        synchronized(inPool) {     // rollback all processing objects to inPool
          inPool.addAll(buffer);
        }
        e.printStackTrace();
      }
    }
  }
}
