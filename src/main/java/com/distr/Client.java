package com.distr;

import java.rmi.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;  

public class Client extends Name_server {
	private Registry name_server;
	private Service service_stub;
	private String host;//localhost
	private int port;//40870
	private static ServerSocket serverSocket;
	private static Socket socket;
	String path;
	
	public Client(String IP , String PORT, int Server_Port) throws RemoteException , NotBoundException, IOException{
		this.host = IP ;
		this.port = Integer.parseInt(PORT);
		serverSocket = new ServerSocket(Server_Port);
		name_server = LocateRegistry.getRegistry(host, port);
		service_stub = (Service)name_server.lookup("name_server");
	}

	public void read(String path) throws UnknownHostException, IOException
	{
		List <String> fileServer = service_stub.getStorage(path); // get storge server hosting "path" file 
		
		System.out.println(fileServer);
		String addr  = new String(fileServer.get(0));  // ip o
		int port  = Integer.parseInt(fileServer.get(1));// Tcp port listening on storageserver  

		Socket socket = new Socket(InetAddress.getByName(addr), port);// crate socket 

		byte[] contents = new byte[10000];
		FileOutputStream fos = new FileOutputStream(path);
		BufferedOutputStream bout = new BufferedOutputStream(fos);
		InputStream is = socket.getInputStream();
		int bytesRead = 0;
		while((bytesRead=is.read(contents))!=-1)
		bout.write(contents, 0, bytesRead);

		bout.flush();
		bout.close();    
		socket.close();
		System.out.println("File saved successfully!");
	}

	private void write(String args[] ) throws Exception {
		path = args[0];

		new Thread (new Runnable (){
			public void run(){
				System.out.println(" listening ...");
			try{
			socket = serverSocket.accept();

			String anim = "|/-\\";
				File file = new File(path);
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

					String data = "\r" + anim.charAt(x % anim.length()) + " " + x + "%" + "Sent" ;
					System.out.write(data.getBytes());

				}   
				os.flush(); 
				bin.close();
				System.out.println("File sent succesfully!");
				}catch(Exception e){ e.printStackTrace();}
			}

		}).start(); 

		service_stub.put(args[4], args[3] , args[0]);

	}


	public void list_files() throws Exception
	{
		List <String> files = service_stub.list(); // get storge server hosting "path" file 
		for (String file : files)
			System.out.println(file);
	}



	public static void main (String args[] )throws  RemoteException , NotBoundException , UnknownHostException , IOException{
		//new Get().run(args[0]);
		if (args.length < 3){ 
			System.err.println( "Bad usage  " + "file IP address of naming server port of naming server ");
			System.exit(1);
		}
		Client get_file = new Client(args[1], args[2], Integer.parseInt(args[3])); //2nd arg   ip of naming server
		get_file.read(args[0] ); // 1st   arg file
	}

}
