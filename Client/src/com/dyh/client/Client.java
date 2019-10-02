package com.dyh.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Client{
	
	public static boolean isOpenClient=false;//判断客户端是否关闭,总开关
	public boolean isEndRecordLog=false;//是否退出日志线程
	private String ServerIp;
	private int ServerPort;
	private Socket downLoadSocket;//下载文件的套接字
	private Socket uploadSocket;//上传文件用的套接字
	private Socket fileSocket;//整体的文件套接字，不关闭
	private int LocalPort;
	private Socket socket;
	public static ArrayList<String> msgList=new ArrayList<String>();//服务器发来的消息存储
	public static ArrayList<ClientLog> clientLogs=new ArrayList<ClientLog>();//错误消息
	public static ArrayList<String> fileList=new ArrayList<String>();//本地的文件列表，从服务器得到的
	private RecordLogThread recordLogThread;
	public void OpenClient(String ServerIp,int ServerPort){
		
		this.ServerIp=ServerIp;//得到服务器Ip
		this.ServerPort=ServerPort;//得到服务器端口
		
		ConnectServerThread connectServerThread=new ConnectServerThread();
		connectServerThread.setName("连接服务器");
		connectServerThread.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		//记录日志到链表线程，写入到磁盘
		recordLogThread=new RecordLogThread("./log");
		recordLogThread.setName("日志记录");
		recordLogThread.start();
	}
	
	
	public void downLoadFile(String fileName,String savePath){
		DownLoadFile downLoadFile=new DownLoadFile(fileName, savePath);
		downLoadFile.start();
	}
	//下载文件
	
	public class DownLoadFile extends Thread{//获取服务器上的文件名,保存本地的路径
		private String fileName;
		private String savePath;
		DownLoadFile(String fileName,String savePath){
			this.fileName=fileName;
			this.savePath=savePath;
		}
		@Override
		public void run() {
			super.run();
		try {
			downLoadSocket=new Socket();
			SocketAddress address=new InetSocketAddress(ServerIp, 6666);//下载文件端口固定
			downLoadSocket.connect(address,3000);//等待3秒
			//发送一个文件名给服务器，路径
			OutputStream outputStream=downLoadSocket.getOutputStream();
			outputStream.write((fileName+"\r\n").getBytes());//将要下载的文件名，路径给服务器
			System.out.println("fileName="+fileName);
			Thread.sleep(100);//等待服务器准备输出流，接收服务器传回的大小信息
			
			InputStream inputStream=downLoadSocket.getInputStream();
			BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
			//收到服务器发来的大小信息
			long fileLength=Long.parseLong(reader.readLine());
			//服务器将大小发送过来
			File dir=new File(savePath);//判断文件夹是否存在
			if(!dir.exists()){
				dir.mkdir();
				System.out.println("创建下载目录");
			}
			int index=fileName.lastIndexOf("\\");
			System.out.println("index="+index);
			//创建文件准备接受文件
			String saveFileName= fileName.substring(index+1, fileName.length()) ;//创建文件
			System.out.println("保存文件名"+ saveFileName);
			File saveFile =new File(savePath, saveFileName);
			
			BufferedInputStream bufferedInputStream=new BufferedInputStream(inputStream);
			DataInputStream dataInputStream=new DataInputStream(bufferedInputStream);
			
			FileOutputStream fileOutputStream=new FileOutputStream(saveFile);
			BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(fileOutputStream);
			DataOutputStream dataOutputStream=new DataOutputStream(bufferedOutputStream);
			System.out.println("下载文件...");
			byte []buff=new byte[1024];
			dataInputStream.read(buff);
			while(true){
				dataOutputStream.write(buff);
				dataOutputStream.flush();
				if(saveFile.length()>=fileLength){
					break;
				}
				System.out.println("客户端下载文件");
				dataInputStream.read(buff);
			}
			dataOutputStream.close();
			System.out.println("接收到："+saveFileName.length()+"B");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		}
			
	}
	
	public void sendFile(String filePath){
		SendFile sendFile=new SendFile(filePath);
		sendFile.start();
	}
	
	//上传文件的线程
	class SendFile extends Thread{
		private String filePath;
		SendFile(String filePath){
			this.filePath=filePath;
		}
		@Override
		public void run() {
			super.run();
		try {
				uploadSocket=new Socket();
				SocketAddress address=new InetSocketAddress(ServerIp, 5555);//上传文件端口固定
				uploadSocket.connect(address, 3000);//连接上了发送文件的套接字
				//将文件的信息上传
				//发送文件大小//发送文件名称
				OutputStream out=uploadSocket.getOutputStream();
				File sendFile=new File(filePath);
				String fileName=sendFile.getName();
				long fileSize=sendFile.length();
				out.write((fileName+"|"+fileSize+"\r\n").getBytes());//发送出去文件名和文件大小
				DataOutputStream dataOutputStream=new DataOutputStream(out);
				//等待服务器创建文件夹等时间
				Thread.sleep(3000);
				try {
					FileInputStream fileInputStream=new FileInputStream(sendFile);
					BufferedInputStream bufferedInputStream=new BufferedInputStream(fileInputStream);
					DataInputStream dataInputStream=new DataInputStream(bufferedInputStream);

					byte []bite=new byte[1024];//接收到1024字节发送一次
					System.out.println("开始上传文件中....");
					while((dataInputStream.read(bite))!=-1){
						dataOutputStream.write(bite);
						System.out.println("已上传:"+bite.length+" 字节");
						dataOutputStream.flush();
					}
					System.out.println("上传文件："+sendFile.getName()+"\t大小："+ dataOutputStream.size()/1024+"KB");
					dataInputStream.close();

				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.getStackTrace();
				}
				
			} catch (Exception e) {
				
			}
		}
	}
	
	class ConnectServerThread extends Thread{
		@Override
		public void run() {
			super.run();
			try {
				//连接Server
				socket=new Socket();
				SocketAddress address =new InetSocketAddress(ServerIp, ServerPort);
				socket.connect(address, 3000);//设置超时
				isOpenClient=true;
				
				//连接服务器的文件线程
				fileSocket =new Socket();
				SocketAddress fileaddress =new InetSocketAddress(ServerIp, 3333);
				fileSocket.connect(fileaddress, 3000);//设置超时
				
				//客户端接收消息
				RecvMsgThread recvMsgThread=new RecvMsgThread();
				recvMsgThread.setName("接收消息");
				recvMsgThread.start();
				
				//客户端接收文件
				RecvFileThread recvFileThread=new RecvFileThread();
				recvFileThread.setName("接收服务器的文件");
				recvFileThread.start();
				
				LocalPort = socket.getLocalPort();//得到本地的的端口
			} catch (Exception e) {
				clientLogs.add(new ClientLog(getTime(), "连接服务器失败："+e.getMessage()+"服务器拒绝连接"));
				System.out.println("连接服务器失败："+e.getMessage());
				e.printStackTrace();
				try {
					Thread.sleep(5000);//要结束了,等待日志记录如文件
					isEndRecordLog=true;//给日志线程发送结束信号
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	class RecvFileThread extends Thread{
		@Override
		public void run() {
			super.run();
			while(isOpenClient==true){//判断是否关闭,关闭了线程就结束
				try {
					//接收消息
					InputStream in=fileSocket.getInputStream();
					BufferedReader reader=new BufferedReader(new InputStreamReader(in));
					String RecvFile= reader.readLine();
					if(RecvFile!=null){
						fileList.add(RecvFile);
						System.out.println("来自服务器的文件消息："+fileList.get(0));
					}
					RecvFile=null;
				} catch (Exception e) {
					e.getStackTrace();
					clientLogs.add(new ClientLog(getTime(), "服务器已关闭："+e.getMessage()));
					System.err.println("异常："+e.getMessage()+"退出接收文件");
					isOpenClient=false;
					return;
				}
			}
		}
		
	}
	 
	
	
	public void SendMsg(String SetMsg){
		//发送消息到服务器
		OutputStream out;
		try {
			out = socket.getOutputStream();
			try {
				Thread.sleep(80);
				out.write((SetMsg+"\r\n").getBytes());
				out.flush();
			} catch (Exception e) {
				clientLogs.add(new ClientLog(getTime(), "发送消息失败："+e.getMessage()));
				System.out.println("异常："+e.getMessage());
				isOpenClient=false;
			}
		} catch (IOException e) {
			clientLogs.add(new ClientLog(getTime(), "已从服务器断开连接"+e.getMessage()));
			e.printStackTrace();
		}
	}
	
	//记录日志线程
	class RecordLogThread extends Thread{
		private String parentPath;
		RecordLogThread(String parentPath){
			this.parentPath=parentPath;
		}
		@Override
		public void run() {
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
			while(isEndRecordLog==false){
				try {
					try {
						wr = new FileWriter(logFile,true);//true 为追加模式
						writer=new BufferedWriter(wr);
						
					} catch (IOException e1) {
						clientLogs.add(new ClientLog(getTime(), "打开文件出错"+e1.getMessage()));
						e1.printStackTrace();
					}
					while(isEndRecordLog==false){
						//获取日志信息
						for(int i=0;i<clientLogs.size();){
							ClientLog serverLog=clientLogs.get(0);
							writer.write(serverLog.occurTime+"\t"+serverLog.occurMsg);
							writer.newLine();
							writer.flush();
							clientLogs.remove(0);
						}
						sleep(500);//写入一次日志间隔
					}
				} catch (Exception e) {
					System.out.println("写日志出错"+e.getMessage());
				}finally {
					try {
						wr.close();
						writer.close();
						System.out.println("关闭日志写出流");
					} catch (IOException e) {
						System.out.println("关闭流失败");
						clientLogs.add(new ClientLog(getTime(), "关闭日志写出流失败："+e.getMessage()));
						e.printStackTrace();
					}
				}
			}
		}
	}
	public String RecvMsg;
	//客户端接收消息
	class RecvMsgThread extends Thread{
		@Override
		public void run() {
			super.run();
			while(isOpenClient==true){//判断是否关闭,关闭了线程就结束
				try {
					//接收消息
					InputStream in=socket.getInputStream();
					BufferedReader reader=new BufferedReader(new InputStreamReader(in));
					RecvMsg= reader.readLine();
					if(RecvMsg!=null){
						System.out.println("来自服务器的消息："+RecvMsg);
						String temp="";
						//获取第二个隔离标志
						int m=RecvMsg.indexOf("】");
						temp+=RecvMsg.substring(0, m+2)+"\n";
						while(true){
							char[] c=RecvMsg.toCharArray();
							//将消息打断，加入回车，换行
							for(int i=m+2;i<c.length;i++){
								temp+=c[i];
							}
							break;
						}
						msgList.add(temp);
					}
					RecvMsg=null;
				} catch (Exception e) {
					clientLogs.add(new ClientLog(getTime(), "服务器已关闭："+e.getMessage()));
					System.err.println("异常："+e.getMessage()+"退出接收");
					isOpenClient=false;
					return;
				}
			}
		}
	}
	
	//获取本地时间
	private String getTime(){
		Date date=new Date();
		SimpleDateFormat format=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return format.format(date);
	}
	
	/**
	 * @return serverIp
	 */
	public String getServerIp() {
		return ServerIp;
	}
	/**
	 * @param serverIp 要设置的 serverIp
	 */
	public void setServerIp(String serverIp) {
		ServerIp = serverIp;
	}
	/**
	 * @return serverPort
	 */
	public int getServerPort() {
		return ServerPort;
	}
	/**
	 * @param serverPort 要设置的 serverPort
	 */
	public void setServerPort(int serverPort) {
		ServerPort = serverPort;
	}
	/**
	 * @return localPort
	 */
	public int getLocalPort() {
		return LocalPort;
	}
	/**
	 * @param localPort 要设置的 localPort
	 */
	public void setLocalPort(int localPort) {
		LocalPort = localPort;
	}
}
