/*
 * @(#)HelloImpl.java	1.5 05/11/17
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

package ru.classifier.server.rmi;

import ru.classifier.common.ObjectOperation;
import ru.classifier.util.Configuration;

import javax.rmi.PortableRemoteObject;
import java.rmi.RemoteException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ProcessorImpl2 extends PortableRemoteObject implements Processor {
  private final BlockingQueue inPool = new LinkedBlockingQueue();
  private final BlockingQueue outPool = new LinkedBlockingQueue();

  public ProcessorImpl2(final ObjectOperation operation) throws RemoteException {
    super();     // invoke rmi linking and remote object initialization

    for (int i = 0; i < Configuration.getIntParam("server.poollistener.count", 1); i++) {
      final Thread poolListener = new Thread(new PoolListener2(inPool, outPool, operation));
      poolListener.setDaemon(true);
      poolListener.start();
    }
  }

  public void put(final Object o) throws RemoteException {
    if (o != null) {
      try {
        inPool.put(o);
      } catch (InterruptedException e) {}
    }
  }

  public Object get() throws RemoteException {
    return outPool.poll();
  }

  public void stop() throws RemoteException {
    Thread.currentThread().interrupt();
  }

  public void put(final Object[] o) throws RemoteException {
    if (o != null) {
      for (int i = 0; i < o.length; i ++) {
        try {
          inPool.put(o[i]);
        } catch (InterruptedException e) {}
      }
    }
  }

  private final Object lock = new Object();

  public Object[] getChunk() throws RemoteException {
    Object[] chunk;

    synchronized(lock) {
      int len = outPool.size();
      chunk = new Object[len];
      for (int i = 0; i < len; i++)
        chunk[i] = outPool.poll();
    }

    return chunk;
  }

}

