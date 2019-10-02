package com.dyh.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// 当有客户端连接时，创建一个线程
public class ClientThread extends Thread{
	//用户的消息集合
	public ArrayList<Mssage> mssagesList =new ArrayList<Mssage>();
	public ArrayList<File> fileList =new ArrayList<File>();//文件列表
	public boolean userIsLive=true;//判断用户是否已断开
	private String UserIp;
	private int UserPort;
	private Socket socket;
	private Socket fileSocket;
	public ClientThread(Socket socket,Socket fileSocket){
		this.socket=socket;
		this.fileSocket=fileSocket;
		UserIp=socket.getInetAddress().toString();
		UserPort=socket.getPort();
	}
	@Override
	public void run() {
		//开启转发功能，转发消息
		BroadCast broadCast=new BroadCast();
		broadCast.setName(socket.toString()+"向自己的客户端转发消息");
		broadCast.start();
		
		//转发文件
		BroadCastFile broadCastFile=new BroadCastFile();
		broadCastFile.setName(socket.toString()+"转发文件信息线程");
		broadCastFile.start();
			
		while(userIsLive==true){//接收客户端消息
			try {
				//获取输入流，读取客户端的消息
				InputStream in=socket.getInputStream();
				//获取客户端信息，即从输入流读取信息
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				//获取接收到的信息
				String msg=reader.readLine();
				if(msg!=null){
					System.out.println("接收到来自["+socket.getPort()+"]的消息,内容为:"+msg+"");
					//将消息加到消息集合
					Server.msgList.add(new Mssage(getTime(0), msg, (socket.getInetAddress()+"."+socket.getPort())));
				}
			}catch (Exception e) {
				e.printStackTrace();
				Server.msgList.add(new Mssage(getTime(1), "用户【"+socket.getInetAddress()+"."+socket.getPort()+"】退出聊天！", "系统提示"));
				System.err.println(socket.getPort()+"断开连接");
				userIsLive=false;
			}
		}
	}
	
	//将服务器上的文件信息发送到客户端，一直处于转播状态
	class BroadCastFile extends Thread{
		@Override
		public void run() {
			super.run();
			while(userIsLive==true){
				try {
					sleep(80);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				for(int i=0;i<fileList.size();i++){
					System.out.println("开始发送文件信息");
					File file=fileList.get(0);
					try {
						OutputStream out=fileSocket.getOutputStream();
						out.write((file.toString()+"\r").getBytes());//向客户端发送消息
						out.flush();
						fileList.remove(0);
					} catch (IOException e) {
						System.err.println("发送文件信息给自己失败："+e.getMessage());
						userIsLive=false;
						break;
					}
				}
			}	
		}
	}
	//向自己发送消息
	class BroadCast extends Thread{
		@Override
		public void run() {
			super.run();
			while(userIsLive==true){
				try {
					sleep(80);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				for(int i=0;i<mssagesList.size();){
					System.out.println("开始发送消息");
					Mssage m=mssagesList.get(0);
					try {
						OutputStream out=socket.getOutputStream();
						out.write((m.msg_Who+" 时间"+m.Msg_Time+"】:"+m.Msg_Text+"\n").getBytes());
						out.flush();
						mssagesList.remove(0);
					} catch (IOException e) {
						System.err.println("发送给自己失败："+e.getMessage());
						userIsLive=false;
						break;
					}
				}
			}
		}
	}
	
	//获取本地时间
	private String getTime(int timeType){//i为1是长时间，有年月日，0位短时间
		Date date=new Date();
		SimpleDateFormat format = null;
		switch (timeType) {
		case 0:
			format=new SimpleDateFormat("HH:mm:ss");
			break;
		case 1:
			format=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			break;
		default:
			System.out.println("获取时间失败，时间类型为(0,1):"+timeType);
			Server.serverLogs.add(new ServerLog(getTime(1), "获取时间失败，时间类型为(0,1):"+timeType));
			break;
		}
		
		return format.format(date);
	}
	/**
	 * @return socket
	 */
	public Socket getSocket() {
		return socket;
	}
	/**
	 * @return userIp
	 */
	public String getUserIp() {
		return UserIp;
	}
	/**
	 * @return userPort
	 */
	public int getUserPort() {
		return UserPort;
	}
}