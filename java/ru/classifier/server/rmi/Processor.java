package ru.classifier.server.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Processor extends Remote {

  public void put(final Object o) throws RemoteException;

  public Object get() throws RemoteException;

  public void put(final Object[] o) throws RemoteException;

  public Object[] getChunk() throws RemoteException;

  public void stop() throws RemoteException;

}

