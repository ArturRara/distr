package com.distr;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server extends UnicastRemoteObject implements Server_interface, Serializable  
{
	private static String master_server_ip ;
	private static int master_server_port ;
	private static int client_socket_port ;
	private static String server_ip;
	private static int server_port ;  
	private String [] files; 
	public Server_interface masterStub ; 
	private String paths;
	private ConcurrentMap<String, ReentrantReadWriteLock> locks;
	ReentrantReadWriteLock lock;
	Registry master_server;
	private static ServerSocket serverSocket ;
	private static Socket socket;

	public Server() throws RemoteException { 
	}
	
	public Server (String args[]) throws Exception {
		server_ip = args.length>0 ? args[0] : "localhost";
		server_port = args.length>1 ? Integer.parseInt(args[1]) : 60001;
		client_socket_port = args.length>2 ? Integer.parseInt(args[2]) : 6666; //pamiętaj
		master_server_ip = args.length>3 ? args[3] : "localhost";
		master_server_port = args.length>3 ? Integer.parseInt(args[4]) : 60000;
		serverSocket = new ServerSocket(client_socket_port);
		locks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
		
		registerServer(server_ip, server_port);
		getFiles();
		master_server = LocateRegistry.getRegistry(master_server_ip, master_server_port);
		Service registration_stub = (Service)master_server.lookup("Master_server");	
		registration_stub.register(server_ip ,client_socket_port ,files ,masterStub );
	}

	private void registerServer(String ip , int port)throws Exception{
		System.setProperty("java.rmi.server.hostname",ip);
		Registry registry = LocateRegistry.createRegistry(port);  
		registry.rebind("Server" , new Server());
		Registry storageServer = LocateRegistry.getRegistry("localhost", port);
		masterStub  = (Server_interface)storageServer.lookup("Server"); 
	}

	public boolean createFile(String fileName)throws RemoteException , IOException {
		File f = new File(fileName);
		//locks.putIfAbsent(fileName, new ReentrantReadWriteLock());
		//lock = locks.get(fileName);
		
		if (f.exists() && !f.isDirectory()){
			return false;	
		}
		else {
			//lock.writeLock().lock();
			f.createNewFile();
			//lock.writeLock().unlock();
			return true;
		}
	}

	public void read(String path) throws IOException , RemoteException {
		this.paths = path;
		new Thread(new Runnable(){

			public void run(){
				String anim = "|/-\\";
				try{
					System.out.println("Byłem tu read");
					socket = serverSocket.accept();
					socket.setSoTimeout(3000000);
					File file = new File(paths);
					FileInputStream fis = new FileInputStream(file);
					BufferedInputStream bin = new BufferedInputStream(fis); 
					OutputStream os = socket.getOutputStream();
					byte[] contents;
					long fileLength = file.length(); 
					long i = 0;
					while(i!=fileLength){ 
						int size = 10000;
						if(fileLength - i >= size)
							i += size;    
						else{ 
							size = (int)(fileLength - i); 
							i = fileLength;
						} 
						contents = new byte[size]; 
						bin.read(contents, 0, size); 
						os.write(contents);
						int x = (int)((i * 100)/fileLength) ;

						String data = "\r" + anim.charAt(x % anim.length()) + " " + x + "%" ;
						System.out.write(data.getBytes());
					}   
					os.flush(); 
					bin.close();  
					socket.close();

				}catch(Exception e ){e.printStackTrace();}

				System.out.println("File sent succesfully!");
			}
		}).start();	

	}	

	public void write(String IP , String PORT , String path) throws UnknownHostException, IOException{

		System.out.println("Write "+ path +" in Storage Server " + server_ip + " "+server_port );

		Socket socket = new Socket(InetAddress.getByName(IP), Integer.parseInt(PORT));
		//lock = locks.get(path);
		//lock.writeLock().lock();
		System.out.println("Byłem tu");
		byte[] contents = new byte[10000];
		FileOutputStream fout = new FileOutputStream(path);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		InputStream is = socket.getInputStream();
		int bytesRead = 0;
		while((bytesRead=is.read(contents))!=-1)
			bout.write(contents, 0, bytesRead);
		//lock.writeLock().unlock();
		bout.flush();
		bout.close();    
		socket.close();
		System.out.println("File saved successfully!");
	}

	private void getFiles()throws Exception{
		File currentDir = new File(".");
		File [] filesList = currentDir.listFiles();
		ArrayList <String> list = new ArrayList<String>();
		
		for (File file : filesList){
			if (file.isFile())
				list.add(file.getName());
		}
		files  = list.toArray(new String[list.size()]);
	} 

	public static void main (String[] args) throws RemoteException, NotBoundException,UnknownHostException {
		if (args.length < 5){
			System.out.println( "Program przyjuje 5 argumnetów, nie wpisane argumenty dostały wartości domyślne \n"
			 + "server_ip | server_port | client_socket_port | Master_server ip | Master_server port  ");
		}
		try{
			Server server =  new Server(args);
		}catch(Exception e ){e.printStackTrace();}	

		System.out.println(server_ip);
	}

}