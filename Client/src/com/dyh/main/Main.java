package com.dyh.main;

import com.dyh.client.Client;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		//Client��
		Client client=new Client();
		client.OpenClient("49.235.21.167",5560);
		Thread.sleep(100);
		if(Client.isOpenClient==true){
			client.SendMsg("�������˷�����");
		}
	}
}
