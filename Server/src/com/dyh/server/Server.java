package com.dyh.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server{
	private String serverIp;
	private int serverPort;
	
	private static ServerSocket fileSocket;//上传文件套接字
	private static ServerSocket downLoadSocket;//下载文件套接字
	public static ServerSocket serverSocket;
	public static ServerSocket serverFileSocket;//服务器的用户的文件socket
	public  static ArrayList<ClientThread> userThreads=new ArrayList<ClientThread>();//用户线程
	public  static ArrayList<Mssage> msgList=new ArrayList<Mssage>();//用户的消息
	public 	static ArrayList<File> fileList=new ArrayList<File>();//用户上传的文件
	public	static ArrayList<ServerLog> serverLogs=new ArrayList<ServerLog>();//服务器的异常情况
	Monitor monitor;//侦听线程
	BroadcastThread broadcastThread;//广播线程
	RecordLogThread recordLogThread;//记录日志线程
	public static boolean isClosedServer= false;//判断是否关闭了服务器,总开关
	
	public void OpenServer(String ServerIp,int ServerPort){
		this.serverIp=ServerIp;
		this.serverPort=ServerPort;
		
		//创建serverSocket并申请服务端口为serverPort
		OpenServerThread openServerThread=new OpenServerThread();
		openServerThread.setName("开启服务器");
		openServerThread.start();
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//记录日志到链表线程，写入到磁盘
		recordLogThread=new RecordLogThread("./log");
		recordLogThread.setName("日志记录");
		recordLogThread.start();
	}
	
	class OpenServerThread extends Thread{
		@Override
		public void run() {
			super.run();
			try {
				InetAddress address=InetAddress.getByName(serverIp);//设置本地IP地址
				serverSocket= new ServerSocket(serverPort, 0,address);
				System.out.println("聊天端口："+serverSocket.toString());
				
				//整体的文件端口
				serverFileSocket =new ServerSocket(3333,0,address);
				System.out.println("服务器的文件端口："+serverFileSocket.toString());
				
				//上传文件端口
				fileSocket =new ServerSocket(5555,0, address);//上传文件端口固定
				System.out.println("文件上传端口："+fileSocket.toString());
				
				//下载文件端口
				downLoadSocket =new ServerSocket(6666,0,address);
				System.out.println("文件下载端口："+downLoadSocket.toString());
				
				//循环侦听客户端连接
				monitor= new Monitor();
				monitor.setName("侦听客户端连接");
				monitor.start();
				
				//监听有无上传文件的连接
				FileMonitor fileMonitor=new FileMonitor();
				fileMonitor.setName("监听用户上传文件");
				fileMonitor.start();
				
				//监听文件下载请求
				DownLoadFileMonitor downLoadFileMonitor=new DownLoadFileMonitor();
				downLoadFileMonitor.setName("监听用户下载文件");
				downLoadFileMonitor.start();
				//发送消息线程
				broadcastThread =new BroadcastThread();
				broadcastThread.setName("广播消息");
				broadcastThread.start();
			} catch (Exception e) {
				serverLogs.add(new ServerLog(getTime(1), "开启服务器失败："+e.getMessage()));
				System.out.println( "开启服务器失败,等待结束："+e.getMessage());
				try {
					Thread.sleep(5000);//等待写入日志
					isClosedServer=true;
					System.out.println("已关闭服务器");
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}
	}
	
	class RecordLogThread extends Thread{
		private String parentPath;
		public RecordLogThread(String parentPath){
			this.parentPath=parentPath;
		}
		@Override
		public void run() {
			System.out.println("开始记录日志");
			File temp=new File(parentPath);
			if(!temp.exists()){
				temp.mkdir();
				System.out.println("新建文件夹"+parentPath);
			}
			File logFile = new File(parentPath,"log.log");
			if(!logFile.exists()){
				try {
					logFile.createNewFile();
					System.out.println("新建文件:log.log");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//打开输入流
			FileWriter wr = null;
			BufferedWriter writer=null;
			super.run();
			while(Server.isClosedServer==false){
				try {
					try {
						wr = new FileWriter(logFile,true);//true 为追加模式
						writer=new BufferedWriter(wr);
					} catch (IOException e1) {
						Server.serverLogs.add(new ServerLog(getTime(1), "打开文件出错"+e1.getMessage()));
						e1.printStackTrace();
					}
					while(Server.isClosedServer==false){
						//获取日志信息
						for(int i=0;i<Server.serverLogs.size();){
							ServerLog serverLog=Server.serverLogs.get(0);
							writer.write(serverLog.occurTime+"\t"+serverLog.occurMsg);
							writer.newLine();
							writer.flush();
							System.out.println("写入日志文件");
							Server.serverLogs.remove(0);
						}
						Thread.sleep(1000);//每隔1秒记录一次日志
					}
				} catch (Exception e) {
					System.out.println("写日志出错"+e.getMessage());
				}finally {
					try {
						wr.close();
						writer.close();
						System.out.println("关闭流记录日志文件流成功");
					} catch (IOException e) {
						System.out.println("关闭流记录日志文件流失败");
						e.printStackTrace();
					}
				}
			}
		}
	}

	//当有上传请求就启动线程让用户上传
	class FileMonitor extends Thread{
		@Override
		public void run() {
			super.run();
			System.out.println("等待用户上传文件...");
			while(isClosedServer==false){
				try {
					fileSocket.setSoTimeout(2000);//等待2秒，没有用户就执行走
					Socket socket =fileSocket.accept();//等待2秒，如果没人连接就不等待，抛出异常
					System.out.println("用户"+socket.getPort()+"上传文件...");
					//当一个客户端连接就启动线程
					try {
						UploadFileThread uploadFileThread = new UploadFileThread(socket);
						uploadFileThread.setName(socket.toString()+"上传文件线程");
						uploadFileThread.start();
					} catch (Exception e) {
						serverLogs.add(new ServerLog(getTime(1),"为用户"+socket+"创建上传文件线程失败"+e.getMessage()));
						System.out.println("创建用户线程失败"+e.getMessage());
					}
				} catch (Exception e) {
					//System.out.println(e.getMessage());
				}
			}
		}
		
	}
	
	//当有用户下载，就启动一个线程给用户提供下载
	class DownLoadFileMonitor extends Thread{
		@Override
		public void run() {
			super.run();
			System.out.println("等待用户下载文件...");
			while(isClosedServer==false){
				try {
					downLoadSocket.setSoTimeout(2000);//等待2秒，没有用户就执行走
					Socket socket =downLoadSocket.accept();//等待2秒，如果没人连接就不等待，抛出异常
					System.out.println("用户"+socket.getPort()+"下载文件...");
					//当一个客户端连接就启动线程
					try {
						DownLoadFileThread downLoadFileThread=new DownLoadFileThread(socket);
						downLoadFileThread.setName(socket.toString()+"下载文件");
						downLoadFileThread.start();
					} catch (Exception e) {
						serverLogs.add(new ServerLog(getTime(1),"为用户"+socket+"创建下载文件线程失败"+e.getMessage()));
						System.out.println("创建用户线程失败"+e.getMessage());
					}
				} catch (Exception e) {
					//System.out.println(e.getMessage());
				}
			}
		}
		
	}
	//服务器的监听用户连接线程
	class Monitor extends Thread {
		@Override
		public void run() {
			super.run();
			while(isClosedServer==false){
				try {
					//System.out.println("等待用户连接...");
					serverSocket.setSoTimeout(2000);//等待2秒，没有用户就执行走
					Socket socket =serverSocket.accept();//等待2秒，如果没人连接就不等待，抛出异常
					Socket userfileSocket=serverFileSocket.accept();
					System.out.println("用户"+socket.getPort()+"连接...");
					msgList.add(new Mssage(getTime(0), "欢迎【"+socket.getInetAddress()+"."+socket.getPort()+"】您加入聊天！","系统提示"));
					//当一个客户端连接就启动线程
					try {
						ClientThread clientThread = new ClientThread(socket,userfileSocket);
						userThreads.add(clientThread);
						clientThread.setName(socket.toString()+"主线程");
						clientThread.start();
						serverLogs.add(new ServerLog(getTime(1),"用户："+socket.getLocalAddress()+"连接"));
					} catch (Exception e) {
						serverLogs.add(new ServerLog(getTime(1),"为用户"+socket+"创建线程失败"+e.getMessage()));
						System.out.println("创建用户线程失败"+e.getMessage());
					}
				} catch (Exception e) {
					//System.out.println(e.getMessage());
				}
			}
		}
	}
	//服务器发送消息到所有客户端线程，由线程发送
	class BroadcastThread extends Thread{
		@Override
		public void run() {
			super.run();
			ClientThread userThread = null;
			Socket socket = null;
			
			while(isClosedServer==false){
				try {
					//发送消息到自己的线程，消息框
					for(int i=0;i<msgList.size();){//判断消息框中是否有消息
						for (int j=0;j<userThreads.size();){//发送至一个客户端
							userThread=userThreads.get(j);
							socket=userThread.getSocket();
							Mssage m=msgList.get(i);
							userThread.mssagesList.add(m);
							System.out.println("向用户消息盒子写入消息");
							System.out.println("发送消息至："+socket.getPort());
							j++;
						}
						msgList.remove(i);//去掉消息框中的消息
					}
					//发送文件列表到用户自己的文件列表
					for(int i=0;i<fileList.size();){
						for (int j=0;j<userThreads.size();j++){
							userThread=userThreads.get(j);
							System.out.println("fileList:"+userThread.fileList.size());
							socket=userThread.getSocket();
							File file=fileList.get(0);
							userThread.fileList.add(file);
							System.out.println("向用户文件列表写入文件息");
							System.out.println("发送文件信息至："+socket.getPort());
						}
						fileList.remove(0);//移除文件表中的文件
					}
					
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
				try {
					Thread.sleep(80);//发送消息延迟
				} catch (InterruptedException e) {
					e.printStackTrace();
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
			serverLogs.add(new ServerLog(getTime(1), "获取时间失败，时间类型为(0,1):"+timeType));
			break;
		}
		
		return format.format(date);
	}

	/**
	 * @return serverIp
	 */
	public String getServerIp() {
		return serverIp;
	}

	/**
	 * @param serverIp 要设置的 serverIp
	 */
	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	/**
	 * @return serverPort
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * @param serverPort 要设置的 serverPort
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	
	
}
