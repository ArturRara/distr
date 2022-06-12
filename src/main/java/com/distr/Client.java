package com.distr;

import java.rmi.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;  

public class Client { 
	private Registry master_server;
	private Service service_stub;
	private String master_server_ip;
	private int master_server_port;
	private String server_port;
	private static ServerSocket server_socket;
	private static Socket socket;
	String path;
	
	public Client(String args[]) throws RemoteException , NotBoundException, IOException{
		this.master_server_ip = args.length>0 ? args[0] : "localhost";
		this.master_server_port = args.length>1 ? Integer.parseInt(args[1]) : 60000;
		this.server_port = args.length>2 ? args[2] : "6668";
		server_socket = new ServerSocket(Integer.parseInt(server_port)); 
		master_server = LocateRegistry.getRegistry(master_server_ip, master_server_port);
		service_stub = (Service)master_server.lookup("Master_server");
	}

	public void read(String path) throws UnknownHostException, IOException{
		List <String> fileServer = service_stub.getStorage(path); 
		
		System.out.println(fileServer);
		String addr  = new String(fileServer.get(0));  
		int port  = Integer.parseInt(fileServer.get(1));
		Socket socket = new Socket(InetAddress.getByName(addr), port);

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

	private void write(String file_name) throws Exception {
		this.path = file_name;

		new Thread (new Runnable (){
			public void run(){
				System.out.println(" listening ...");
			try{
			socket = server_socket.accept();

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

		service_stub.put("localhost", server_port , file_name);
	}

	public void list_files() throws Exception
	{
		List <String> files = service_stub.listFiles();
		for (String file : files)
			System.out.println(file);
	}


	public static void main (String args[] )throws  RemoteException , NotBoundException , UnknownHostException , IOException{
		if (args.length < 3){ 
			System.out.println( "Program przyjuje 3 argumnenty, nie wpisane argumenty dostały wartości domyślne " 
			+ "host port socket ");
		}
		Client client = new Client(args);
		System.out.println("Program czeka na komendy");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";

			while (line.equalsIgnoreCase("quit") == false) {
				String[] words = in.readLine().split(" ");
				switch (words[0].toLowerCase()) {
					case "read":
						try {
							client.read(words[1]);
						} catch (Exception e) {
							System.err.println("Error: the file could not be read");
							//e.printStackTrace();
						}
						break;
					case "write":
						try {
							client.write(words[1]);
						} catch (Exception e) {
							System.err.println("Error: File not found");
							e.printStackTrace();
						}
						break;
					case "ls":
						try {
							client.list_files();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case "help":
					default:
						System.out.println("Dostępne komendy \n" + 
						" write \"filename\"| read \"filename\" | ls");
				}
			}
   		in.close();
	}
}
