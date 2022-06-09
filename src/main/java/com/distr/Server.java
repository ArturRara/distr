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
        
public class Server extends UnicastRemoteObject implements Server_interface, Command, Serializable  
{
	private static String host ;
	private static int port ;
	private static int storage_port ;
	private static String storage_ip;
	private static int PORT_RMI ;  
	private String [] files ; 
	public Server_interface masterStub ; 
	private String paths;

	private static ServerSocket serverSocket ;
	private static Socket socket;

	public Server() throws RemoteException { 
	}
	public Server (File  root ) throws RemoteException {
	}

	public Server (String args[]) throws Exception {
		storage_ip = args[0];
		storage_port = Integer.parseInt(args[1]);
		PORT_RMI = Integer.parseInt(args[2]);
		host = args[3];
		port = Integer.parseInt(args[4]);
		serverSocket = new ServerSocket(storage_port);
		
		
		createServer(storage_ip, PORT_RMI);
		getFiles();
		registerNaming(host, port);
	}

	private void createServer(String IP , int PORT)throws Exception{
		System.setProperty("java.rmi.server.hostname",IP);

		Registry registry = LocateRegistry.createRegistry(PORT);  
		registry.rebind("Service" , new Server());
		Registry storageserver = LocateRegistry.getRegistry("localhost", PORT);

		masterStub  = (Server_interface)storageserver.lookup("Service"); 
	}

	private void getFiles()throws Exception {
		File currentDir = new File(".");
		File [] filesList = currentDir.listFiles();
		ArrayList <String> list = new ArrayList<String>();
		for (File file : filesList){
			if (file.isFile())
				list.add(file.getName());
		}
		files  = list.toArray(new String[list.size()]);
	} 

	private void registerNaming(String IP , int PORT)throws Exception{

		Registry name_server = LocateRegistry.getRegistry(IP, PORT);
		Register_server registration_stub = (Register_server)name_server.lookup("name_server");	

		registration_stub.register(storage_ip ,storage_port ,files ,masterStub );
	}

	public boolean create(String file )throws RemoteException , IOException {
		File f = new File(file) ;
		if (f.exists() && !f.isDirectory()){
			return false;	
		}
		else {
			f.createNewFile();
			return true;
		}
	}

	public byte[] read() throws RemoteException {
		byte[] b = "Read String from Storage server ".getBytes();
		System.out.println(new String (b));	
		return b;	
	}

	public void read(String path) throws IOException , RemoteException {
		this.paths = path;
		new Thread(new Runnable(){

			public void run(){
				String anim = "|/-\\";
				try{
					socket = serverSocket.accept();
					socket.setSoTimeout(3000000);
					File file = new File(paths);
					FileInputStream fis = new FileInputStream(file);
					BufferedInputStream bin = new BufferedInputStream(fis); 
					OutputStream os = socket.getOutputStream();
					byte[] contents;
					long fileLength = file.length(); 
					long current = 0;
					while(current!=fileLength){ 
						int size = 10000;
						if(fileLength - current >= size)
							current += size;    
						else{ 
							size = (int)(fileLength - current); 
							current = fileLength;
						} 
						contents = new byte[size]; 
						bin.read(contents, 0, size); 
						os.write(contents);
						int x = (int)((current * 100)/fileLength) ;

						String data = "\r" + anim.charAt(x % anim.length()) + " " + x + "%" ;
						System.out.write(data.getBytes());

					}   
					os.flush(); 
					bin.close();  
					//socket.close();
				}catch(Exception e ){e.printStackTrace();}

				System.out.println("File sent succesfully!");
			}
		}).start();	
	}	

	public void write(String IP , String PORT , String  path ) throws UnknownHostException, IOException{

		System.out.println("Write "+ path +"in Storage Server " + storage_ip + " "+PORT_RMI );

		String addr  = new String (IP);  // ip o
		int port  = Integer.parseInt(PORT);// Tcp port listening on sender (put)

		Socket socket = new Socket(InetAddress.getByName(addr), port);// crate socket 

		byte[] contents = new byte[10000];
		FileOutputStream fout = new FileOutputStream(path);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		InputStream is = socket.getInputStream();
		int bytesRead = 0;
		while((bytesRead=is.read(contents))!=-1)
		bout.write(contents, 0, bytesRead);

		bout.flush();
		bout.close();    
		socket.close();
		System.out.println("File saved successfully!");
	}

	public static void main (String[] args) throws RemoteException, NotBoundException,UnknownHostException {
		if (args.length < 5){
			System.err.println( "Bad usage  " + "ITS OWN IP | PORT to use for tcp | PORT RMI || IP of Naming server | Port naming server  ");
			System.exit(1);
		}
		try{
			Server server =  new Server(args);
		}catch(Exception e ){e.printStackTrace();}	

		System.out.println(storage_ip);
	}

}