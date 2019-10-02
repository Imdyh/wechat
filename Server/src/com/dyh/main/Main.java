package com.dyh.main;

import com.dyh.server.Server;

public class Main {
	public static void main(String[] args) {
		
		//Server
		Server server =new Server();
		server.OpenServer("172.17.0.9",5560);
	}
}
