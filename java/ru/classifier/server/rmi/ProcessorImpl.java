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
import java.util.LinkedList;
import java.util.List;

public class ProcessorImpl extends PortableRemoteObject implements Processor {
  private final List inPool = new LinkedList();
  private final List outPool = new LinkedList();

  public ProcessorImpl(final ObjectOperation operation) throws RemoteException {
    super();     // invoke rmi linking and remote object initialization

    for (int i = 0; i < Configuration.getIntParam("server.poollistener.count", 1); i++) {
      final Thread poolListener = new Thread(new PoolListener(inPool, outPool, operation));
      poolListener.setDaemon(true);
      poolListener.start();
    }
  }

  public void put(final Object o) throws RemoteException {
    if (o != null) {
      synchronized(inPool) {
        inPool.add(o);
      }
    }
  }

  public Object get() throws RemoteException {
    synchronized (outPool) {
    if (!outPool.isEmpty())
      return outPool.remove(0);
    else
      return null;
    }
  }

  public void stop() throws RemoteException {
    Thread.currentThread().interrupt();
  }

  public void put(final Object[] o) throws RemoteException {
  }

  public Object[] getChunk() throws RemoteException {
    return new Object[] {};
  }

}

