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
			InputStream inputStream=downLoadSocket.getInputStream();//获取套接字的输入流
			//获取文件的输入流
			BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
			//读取客户端要下载的文件信息
			String fileName =reader.readLine();//获取文件的信息
			//将文件的大小信息发送给客户端
			File file=new File(fileName);
			OutputStream outputStream=downLoadSocket.getOutputStream();
			outputStream.write((file.length()+"\r\n").getBytes());//将文件的大小发送给客户端
			//准备发送给客户端
			Thread.sleep(5000);//等待客户端处理
			byte []buff=new byte[1024];
			FileInputStream fileInputStream=new FileInputStream(file);
			BufferedInputStream bufferedInputStream=new BufferedInputStream(fileInputStream);
			DataInputStream dataInputStream=new DataInputStream(bufferedInputStream);
			DataOutputStream dataOutputStream=new DataOutputStream(outputStream);
			
			while(dataInputStream.read(buff)!=-1){
				dataOutputStream.write(buff);//向输出流写出数据
				System.out.println("服务器端发送文件");
			}
			dataInputStream.close();
		} catch (Exception e) {
			
		}
		
	}
}
