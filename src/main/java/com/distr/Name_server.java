package com.distr;

import java.rmi.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Name_server extends UnicastRemoteObject implements Register_server , Service {

	private Map <String , List <Server_interface> > filesLocation;
	private Map <Server_interface ,List <String> >  Replica;
	private Set <Server_interface> StorageServers;
	private List<Server_interface> replicaServersLocs;
	private Random random;

	public Name_server () throws RemoteException {
		filesLocation = new HashMap <String , List<Server_interface> >();
		Replica = new HashMap <Server_interface, List<String>> ();
		StorageServers = new HashSet<Server_interface>();
	}

	public synchronized void start(String PORT) throws RemoteException {
		Registry registry = LocateRegistry.createRegistry(Integer.parseInt(PORT));		
		registry.rebind("Name_server", new Name_server()); // bind remote obj with name 
	}

	public String[] register(String IP_STORAGE_SERVER , int storage_port , String [] files, Server_interface command_stub ) throws RemoteException, NotBoundException{	
		StorageServers.add(command_stub); //check if server is active 

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
			Replica.put( command_stub,  temp);
		}
		
		return new String[2];
	}

	public boolean createFile(String file ) throws RemoteException, FileNotFoundException{//nie wiem czy działa
		System.out.println("[@Master] Creating new file initiated");
		int luckyReplicas[] = new int[replicationN];
		List<Server_interface> replicas = new ArrayList<Server_interface>();

		Set<Integer> chosenReplicas = new TreeSet<Integer>();

		for (int i = 0; i < luckyReplicas.length; i++) {

			do {
				luckyReplicas[i] = random.nextInt(replicationN);

			} while(!replicaServersLocs.get(luckyReplicas[i]).isAlive() || chosenReplicas.contains(luckyReplicas[i]));


			chosenReplicas.add(luckyReplicas[i]);
			// add the lucky replica to the list of replicas maintaining the file
			replicas.add(replicaServersLocs.get(luckyReplicas[i]));

			// create the file at the lucky replicas 
			try {
				replicaServersStubs.get(luckyReplicas[i]).createFile(file);
			} catch (IOException e) {
				// failed to create the file at replica server 
				e.printStackTrace();
			}

		}

		// the primary replica is the first lucky replica picked
		int primary = luckyReplicas[0];
		try {
			replicaServersStubs.get(primary).takeCharge(fileName, replicas);
		} catch (RemoteException | NotBoundException e) {
			// couldn't assign the master replica
			e.printStackTrace();
		}

		filesLocationMap.put(fileName, replicas);
		primaryReplicaMap.put(fileName, replicaServersLocs.get(primary));	
	}


	public List<String> getStorage(String file ) throws RemoteException, FileNotFoundException , IOException {

		Server_interface  random_server = filesLocation.get(file).get(new Random().nextInt(filesLocation.get(file).size()));
		System.out.println("random server " + Replica.get(random_server));

		random_server.read(file); // start read thread at server 
		return Replica.get(random_server);
	}	

	public boolean put(String IP , String PORT , String path )throws Exception {
			System.out.println("Senfing file to : ");

				System.out.println("File "+ path + "not exist" + " storing file ");

				for (Server_interface stub : StorageServers){
					stub.write(IP , PORT , path);
				}
				return true;
	}

	public List <String> list() throws Exception {
		return new ArrayList( filesLocation.keySet()) ;
	}


	public static void main (String args[] ) throws RemoteException  , NotBoundException , UnknownHostException, IOException {
		if (args.length < 2){
			System.err.println( "Bad usage:   " + "ITS OWN IP Address  Port oto use > 1100");
			System.exit(1);
		}
		System.setProperty("java.rmi.server.hostname",args[0]);//ip  
		new Name_server().start(args[1]);
		System.out.println("\nListening Incoming  Connections on :: " + args[1]);
	}

}
