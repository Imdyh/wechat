package com.dyh.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class UploadFileThread extends Thread{
	private Socket socket;
	UploadFileThread(Socket socket){
		this.socket=socket;
	}
	@Override
	public void run() {
		super.run();
		try {
			//获取输入流
			InputStream in =socket.getInputStream();
			//读取文件的大小，名称信息
			BufferedReader reader=new BufferedReader(new InputStreamReader(in));
			String fileInfo=reader.readLine();
			int index =fileInfo.indexOf("|");//获取第一次出现分隔符的位置
			String fileName=fileInfo.substring(0, index);
			
			long fileLength=Long.parseLong(fileInfo.substring(index+1, fileInfo.length()));
			
			System.out.println("文件名："+fileName+"\t文件大小："+fileLength+"b");
			//根据端口号创建文件夹
			File fileDir=new File("./files/"+socket.getPort());
			if(!fileDir.exists()){
				boolean isCreaded= fileDir.mkdirs();
				System.out.println("创建文件夹:"+isCreaded);
			}
			File saveFile=new File("./files/"+socket.getPort()+"/"+fileName);
			if(!saveFile.exists()){
				System.out.println("创建文件："+saveFile.createNewFile());
			}
			DataInputStream dataInputStream=new DataInputStream(in);
			//将文件保存到本地磁盘
			File outFile =new File(saveFile.toString());
			FileOutputStream outputStream=new FileOutputStream(outFile);
			BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(outputStream);
			DataOutputStream dataOutputStream=new DataOutputStream(bufferedOutputStream);
			byte []bite=new byte[1024];//接收了1024字节写入一次
			
			dataInputStream.read(bite);
			while(true){
				outputStream.write(bite);
				System.out.println("服务器下载文件到服务器："+outFile.getName()+"大小："+outFile.length()+" 字节");
				dataOutputStream.flush();
				if(outFile.length()>=fileLength){//当小于了1024证明没有数据了，到了末尾
					break;
				}
				dataInputStream.read(bite);
			}
			System.out.println("服务器已接收："+outFile.length()/1024+"KB");
			dataInputStream.close();
			dataOutputStream.close();
			
			//将文件保存在服务器端，加到链表中
			Server.fileList.add(saveFile);
			
			System.out.println(Server.fileList.get(0).getName());
			System.out.println("总大小："+outFile.length()/1024+"KB");
		} catch (Exception e) {
			
		}
	}
}
