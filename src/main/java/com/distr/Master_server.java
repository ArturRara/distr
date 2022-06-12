package com.distr;

import java.rmi.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Master_server extends UnicastRemoteObject implements Service {
	private Map <String , List <Server_interface> > filesLocation;
	private Map <Server_interface ,List <String> >  Replica;
	private Set <Server_interface> StorageServers;
	private List<Server_interface> replicaServersLocs;
	private Random random;
	

	public Master_server () throws RemoteException {
		filesLocation = new HashMap <String , List<Server_interface> >();
		Replica = new HashMap <Server_interface, List<String>> ();
		StorageServers = new HashSet<Server_interface>();
		replicaServersLocs = new ArrayList<Server_interface>();
		random = new Random(); 
	}

	public synchronized void start(String PORT) throws RemoteException {
		Registry registry = LocateRegistry.createRegistry(Integer.parseInt(PORT));		
		registry.rebind("Master_server", new Master_server());
	}

	public String[] register(String IP_STORAGE_SERVER , int storage_port , String [] files, Server_interface command_stub ) throws RemoteException, NotBoundException{	
		StorageServers.add(command_stub);

		System.out.println("Storage server : "+  IP_STORAGE_SERVER + " "+storage_port+ " connected");
		for (String file : files) {
			if (filesLocation.get(file) == null ){
				List<Server_interface> temp = new ArrayList<Server_interface>();
				temp.add(command_stub);
				filesLocation.put(file , temp);
			}
			else
				filesLocation.get(file).add(command_stub);
		}
		if ( Replica.get(command_stub) == null){
			List<String> temp = new ArrayList<String>();
			temp.add(new String(IP_STORAGE_SERVER));
			temp.add(new String(storage_port + "") );
			Replica.put(command_stub, temp);
		}
		
		return new String[2];
	}

	public List<String> getStorage(String file) throws RemoteException, FileNotFoundException , IOException {
		System.out.println("Client connected");
		Server_interface  random_server = filesLocation.get(file).get(random.nextInt(filesLocation.get(file).size()));
		System.out.println("random server " + Replica.get(random_server));

		random_server.read(file);
		
		return Replica.get(random_server);
	}	

	public boolean put(String ip , String port , String path )throws Exception {
			System.out.println("Senfing file to : " + ip);

				for (Server_interface stub : StorageServers){
					stub.write(ip , port , path);
				}
				return true;
	}

	public List <String> listFiles() throws Exception {
		return new ArrayList(filesLocation.keySet());
	}

	public static void main (String args[] ) throws RemoteException  , NotBoundException , UnknownHostException, IOException {
		String ip = args.length>0 ? args[0] : "localhost";
		String port = args.length>1 ? args[1] : "60000";
		System.setProperty("java.rmi.server.hostname",ip);
		new Master_server().start(port);
		System.out.println("\nListening Incoming  Connections on :: " + port);
	}

}
