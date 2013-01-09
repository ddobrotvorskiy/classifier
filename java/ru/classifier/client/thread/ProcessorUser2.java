package ru.classifier.client.thread;

import ru.classifier.server.rmi.Processor;
import ru.classifier.util.Configuration;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

/**
 * User: root
 * Date: 06.04.2008
 * Time: 21:45:27
 */
public class ProcessorUser2 {

  private final static long SLEEP_TIME = Configuration.getIntParam("client.processoruser.sleeptime", 7);
  private final static int MAX_BUFFER_SIZE =Configuration.getIntParam("client.processoruser.buffersize", 30);
  private final static int CHUNK_SIZE =Configuration.getIntParam("client.chunk.size", 1);

  private final BlockingQueue inPool;
  private final BlockingQueue outPool;
  private final Processor processor;

  private final HashSet buffer = new HashSet();

  public ProcessorUser2(final BlockingQueue inPool, final BlockingQueue outPool, final Processor processor) {
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
        final Writer log = new FileWriter("ProcessorUser.putter.log");
        log.write("\nInitial sleeptime = " + SLEEP_TIME + " ideal chunk size = " + CHUNK_SIZE + " max buffer size = " + MAX_BUFFER_SIZE);
        log.flush();

        while(true) {
          log.write("\nbuffer size = " + buffer.size() + " inPool size =  " + inPool.size());
          log.flush();
          if (inPool.size() > 0 && buffer.size() < MAX_BUFFER_SIZE) {
            try {
              while (inPool.size() > 0 && buffer.size() < MAX_BUFFER_SIZE) {
                final int len = Math.min(Math.min(MAX_BUFFER_SIZE - buffer.size(), inPool.size()), CHUNK_SIZE);
                final Object[] chunk = new Object[len];
                for (int i = 0; i < len; i ++) {
                  Object o = inPool.take();
                  chunk[i] = o;
                  buffer.add(o);
                }
                processor.put(chunk);
                log.write("\nput chunk size  = " + chunk.length);
                log.flush();
              }
            } catch (InterruptedException e) {e.printStackTrace();}
          } else {
            try {
              Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {}
          }
        }
      } catch (RemoteException e) {
        synchronized(inPool) {
          synchronized(buffer) {
            inPool.addAll(buffer);
            inPool.notifyAll();
          }
        }
        e.printStackTrace();
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }

  private class Getter implements Runnable {
    public void run() {




      long sleepTime = SLEEP_TIME;

      try {
        final Writer log = new FileWriter("ProcessorUser.getter.log");

        log.write("\nInitial sleeptime = " + SLEEP_TIME + " ideal chunk size = " + CHUNK_SIZE);

        while(true) {

          Object[] chunk = processor.getChunk();
          int len = chunk.length;

          log.write("\nSleeptime = " + sleepTime + " chunk.size = " + chunk.length);
          log.flush();

          if (chunk.length > 0) {

            sleepTime = Math.round((float) sleepTime * ((float)CHUNK_SIZE / (float)len));
            sleepTime = Math.max(sleepTime, 10);
            sleepTime = Math.min(sleepTime, 500);

            for (int i = 0; i < chunk.length; i++) {
              buffer.remove(chunk[i]);
              outPool.add(chunk[i]);
            }
          }

          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException e) {}
        }
      } catch(RemoteException e) {
        synchronized(inPool) {     // rollback all processing objects to inPool
          synchronized(buffer) {
            inPool.addAll(buffer);
            inPool.notifyAll();
          }
        }
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
