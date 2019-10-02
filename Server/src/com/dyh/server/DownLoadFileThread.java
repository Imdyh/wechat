package com.dyh.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class DownLoadFileThread extends Thread{
	private Socket downLoadSocket;
	DownLoadFileThread(Socket downLoadSocket){
		this.downLoadSocket=downLoadSocket;		
	}
	@Override
	public void run() {
		super.run();
		try {
			InputStream inputStream=downLoadSocket.getInputStream();//��ȡ�׽��ֵ�������
			//��ȡ�ļ���������
			BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
			//��ȡ�ͻ���Ҫ���ص��ļ���Ϣ
			String fileName =reader.readLine();//��ȡ�ļ�����Ϣ
			//���ļ��Ĵ�С��Ϣ���͸��ͻ���
			File file=new File(fileName);
			OutputStream outputStream=downLoadSocket.getOutputStream();
			outputStream.write((file.length()+"\r\n").getBytes());//���ļ��Ĵ�С���͸��ͻ���
			//׼�����͸��ͻ���
			Thread.sleep(5000);//�ȴ��ͻ��˴���
			byte []buff=new byte[1024];
			FileInputStream fileInputStream=new FileInputStream(file);
			BufferedInputStream bufferedInputStream=new BufferedInputStream(fileInputStream);
			DataInputStream dataInputStream=new DataInputStream(bufferedInputStream);
			DataOutputStream dataOutputStream=new DataOutputStream(outputStream);
			
			while(dataInputStream.read(buff)!=-1){
				dataOutputStream.write(buff);//�������д������
				System.out.println("�������˷����ļ�");
			}
			dataInputStream.close();
		} catch (Exception e) {
			
		}
		
	}
}
