package com.distr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class File_send  {
	private static ServerSocket serverSocket ;
	private static Socket socket;

	private Registry name_server; 
	private Service service_stub;
	private String host;
	private int port;
	String path;

	public File_send(String IP , String PORT, int Server_Port ) throws Exception {
		this.host = IP ;
		this.port = Integer.parseInt(PORT);

		serverSocket = new ServerSocket(Server_Port);

		name_server = LocateRegistry.getRegistry(host, port);
		service_stub = (Service)name_server.lookup("name_server");
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

	public static void main(String[] args) throws Exception {
		if (args.length < 5){ 
			System.err.println( "Bad usage  " + "file | IP address of naming server | port of naming server | port for tcp  |its own ip "); //contact naming serv to put file 
			System.exit(1);
		}
		File_send file_send = new File_send(args[1], args[2], Integer.parseInt(args[3])); //2nd arg   ip of naming server
		file_send.write(args);
		
	}
}