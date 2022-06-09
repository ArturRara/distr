package com.distr;

import java.rmi.RemoteException;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;


interface Server_interface extends Remote 
{
	public byte[] read() throws RemoteException;
	public void read(String path) throws IOException , RemoteException;
	public boolean create (String file) throws RemoteException, IOException;
	public void write(String IP , String PORT , String  path) throws UnknownHostException, IOException;
}

interface Register_server extends Remote  
{
	public String[] register (String IP_STORAGE_SERVER , int PORT_STORAGE_SERVER , String [] files , Server_interface command_stub ) throws RemoteException, NotBoundException;
}


interface Create_file extends Remote  
{
	public boolean create (String file) throws RemoteException, IOException;
}

interface Service  extends Remote
{
        public boolean createFile(String file ) throws RemoteException, FileNotFoundException;
		public List<String> getStorage(String file ) throws RemoteException, FileNotFoundException ,IOException ;	
		public boolean put(String IP , String PORT , String path ) throws Exception;
		public List<String> list() throws Exception;
}

interface Command extends Remote  
{
	public boolean create (String file) throws RemoteException, IOException;
}