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
	
	private static ServerSocket fileSocket;//�ϴ��ļ��׽���
	private static ServerSocket downLoadSocket;//�����ļ��׽���
	public static ServerSocket serverSocket;
	public static ServerSocket serverFileSocket;//���������û����ļ�socket
	public  static ArrayList<ClientThread> userThreads=new ArrayList<ClientThread>();//�û��߳�
	public  static ArrayList<Mssage> msgList=new ArrayList<Mssage>();//�û�����Ϣ
	public 	static ArrayList<File> fileList=new ArrayList<File>();//�û��ϴ����ļ�
	public	static ArrayList<ServerLog> serverLogs=new ArrayList<ServerLog>();//���������쳣���
	Monitor monitor;//�����߳�
	BroadcastThread broadcastThread;//�㲥�߳�
	RecordLogThread recordLogThread;//��¼��־�߳�
	public static boolean isClosedServer= false;//�ж��Ƿ�ر��˷�����,�ܿ���
	
	public void OpenServer(String ServerIp,int ServerPort){
		this.serverIp=ServerIp;
		this.serverPort=ServerPort;
		
		//����serverSocket���������˿�ΪserverPort
		OpenServerThread openServerThread=new OpenServerThread();
		openServerThread.setName("����������");
		openServerThread.start();
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//��¼��־�������̣߳�д�뵽����
		recordLogThread=new RecordLogThread("./log");
		recordLogThread.setName("��־��¼");
		recordLogThread.start();
	}
	
	class OpenServerThread extends Thread{
		@Override
		public void run() {
			super.run();
			try {
				InetAddress address=InetAddress.getByName(serverIp);//���ñ���IP��ַ
				serverSocket= new ServerSocket(serverPort, 0,address);
				System.out.println("����˿ڣ�"+serverSocket.toString());
				
				//������ļ��˿�
				serverFileSocket =new ServerSocket(3333,0,address);
				System.out.println("���������ļ��˿ڣ�"+serverFileSocket.toString());
				
				//�ϴ��ļ��˿�
				fileSocket =new ServerSocket(5555,0, address);//�ϴ��ļ��˿ڹ̶�
				System.out.println("�ļ��ϴ��˿ڣ�"+fileSocket.toString());
				
				//�����ļ��˿�
				downLoadSocket =new ServerSocket(6666,0,address);
				System.out.println("�ļ����ض˿ڣ�"+downLoadSocket.toString());
				
				//ѭ�������ͻ�������
				monitor= new Monitor();
				monitor.setName("�����ͻ�������");
				monitor.start();
				
				//���������ϴ��ļ�������
				FileMonitor fileMonitor=new FileMonitor();
				fileMonitor.setName("�����û��ϴ��ļ�");
				fileMonitor.start();
				
				//�����ļ���������
				DownLoadFileMonitor downLoadFileMonitor=new DownLoadFileMonitor();
				downLoadFileMonitor.setName("�����û������ļ�");
				downLoadFileMonitor.start();
				//������Ϣ�߳�
				broadcastThread =new BroadcastThread();
				broadcastThread.setName("�㲥��Ϣ");
				broadcastThread.start();
			} catch (Exception e) {
				serverLogs.add(new ServerLog(getTime(1), "����������ʧ�ܣ�"+e.getMessage()));
				System.out.println( "����������ʧ��,�ȴ�������"+e.getMessage());
				try {
					Thread.sleep(5000);//�ȴ�д����־
					isClosedServer=true;
					System.out.println("�ѹرշ�����");
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
			System.out.println("��ʼ��¼��־");
			File temp=new File(parentPath);
			if(!temp.exists()){
				temp.mkdir();
				System.out.println("�½��ļ���"+parentPath);
			}
			File logFile = new File(parentPath,"log.log");
			if(!logFile.exists()){
				try {
					logFile.createNewFile();
					System.out.println("�½��ļ�:log.log");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//��������
			FileWriter wr = null;
			BufferedWriter writer=null;
			super.run();
			while(Server.isClosedServer==false){
				try {
					try {
						wr = new FileWriter(logFile,true);//true Ϊ׷��ģʽ
						writer=new BufferedWriter(wr);
					} catch (IOException e1) {
						Server.serverLogs.add(new ServerLog(getTime(1), "���ļ�����"+e1.getMessage()));
						e1.printStackTrace();
					}
					while(Server.isClosedServer==false){
						//��ȡ��־��Ϣ
						for(int i=0;i<Server.serverLogs.size();){
							ServerLog serverLog=Server.serverLogs.get(0);
							writer.write(serverLog.occurTime+"\t"+serverLog.occurMsg);
							writer.newLine();
							writer.flush();
							System.out.println("д����־�ļ�");
							Server.serverLogs.remove(0);
						}
						Thread.sleep(1000);//ÿ��1���¼һ����־
					}
				} catch (Exception e) {
					System.out.println("д��־����"+e.getMessage());
				}finally {
					try {
						wr.close();
						writer.close();
						System.out.println("�ر�����¼��־�ļ����ɹ�");
					} catch (IOException e) {
						System.out.println("�ر�����¼��־�ļ���ʧ��");
						e.printStackTrace();
					}
				}
			}
		}
	}

	//�����ϴ�����������߳����û��ϴ�
	class FileMonitor extends Thread{
		@Override
		public void run() {
			super.run();
			System.out.println("�ȴ��û��ϴ��ļ�...");
			while(isClosedServer==false){
				try {
					fileSocket.setSoTimeout(2000);//�ȴ�2�룬û���û���ִ����
					Socket socket =fileSocket.accept();//�ȴ�2�룬���û�����ӾͲ��ȴ����׳��쳣
					System.out.println("�û�"+socket.getPort()+"�ϴ��ļ�...");
					//��һ���ͻ������Ӿ������߳�
					try {
						UploadFileThread uploadFileThread = new UploadFileThread(socket);
						uploadFileThread.setName(socket.toString()+"�ϴ��ļ��߳�");
						uploadFileThread.start();
					} catch (Exception e) {
						serverLogs.add(new ServerLog(getTime(1),"Ϊ�û�"+socket+"�����ϴ��ļ��߳�ʧ��"+e.getMessage()));
						System.out.println("�����û��߳�ʧ��"+e.getMessage());
					}
				} catch (Exception e) {
					//System.out.println(e.getMessage());
				}
			}
		}
		
	}
	
	//�����û����أ�������һ���̸߳��û��ṩ����
	class DownLoadFileMonitor extends Thread{
		@Override
		public void run() {
			super.run();
			System.out.println("�ȴ��û������ļ�...");
			while(isClosedServer==false){
				try {
					downLoadSocket.setSoTimeout(2000);//�ȴ�2�룬û���û���ִ����
					Socket socket =downLoadSocket.accept();//�ȴ�2�룬���û�����ӾͲ��ȴ����׳��쳣
					System.out.println("�û�"+socket.getPort()+"�����ļ�...");
					//��һ���ͻ������Ӿ������߳�
					try {
						DownLoadFileThread downLoadFileThread=new DownLoadFileThread(socket);
						downLoadFileThread.setName(socket.toString()+"�����ļ�");
						downLoadFileThread.start();
					} catch (Exception e) {
						serverLogs.add(new ServerLog(getTime(1),"Ϊ�û�"+socket+"���������ļ��߳�ʧ��"+e.getMessage()));
						System.out.println("�����û��߳�ʧ��"+e.getMessage());
					}
				} catch (Exception e) {
					//System.out.println(e.getMessage());
				}
			}
		}
		
	}
	//�������ļ����û������߳�
	class Monitor extends Thread {
		@Override
		public void run() {
			super.run();
			while(isClosedServer==false){
				try {
					//System.out.println("�ȴ��û�����...");
					serverSocket.setSoTimeout(2000);//�ȴ�2�룬û���û���ִ����
					Socket socket =serverSocket.accept();//�ȴ�2�룬���û�����ӾͲ��ȴ����׳��쳣
					Socket userfileSocket=serverFileSocket.accept();
					System.out.println("�û�"+socket.getPort()+"����...");
					msgList.add(new Mssage(getTime(0), "��ӭ��"+socket.getInetAddress()+"."+socket.getPort()+"�����������죡","ϵͳ��ʾ"));
					//��һ���ͻ������Ӿ������߳�
					try {
						ClientThread clientThread = new ClientThread(socket,userfileSocket);
						userThreads.add(clientThread);
						clientThread.setName(socket.toString()+"���߳�");
						clientThread.start();
						serverLogs.add(new ServerLog(getTime(1),"�û���"+socket.getLocalAddress()+"����"));
					} catch (Exception e) {
						serverLogs.add(new ServerLog(getTime(1),"Ϊ�û�"+socket+"�����߳�ʧ��"+e.getMessage()));
						System.out.println("�����û��߳�ʧ��"+e.getMessage());
					}
				} catch (Exception e) {
					//System.out.println(e.getMessage());
				}
			}
		}
	}
	//������������Ϣ�����пͻ����̣߳����̷߳���
	class BroadcastThread extends Thread{
		@Override
		public void run() {
			super.run();
			ClientThread userThread = null;
			Socket socket = null;
			
			while(isClosedServer==false){
				try {
					//������Ϣ���Լ����̣߳���Ϣ��
					for(int i=0;i<msgList.size();){//�ж���Ϣ�����Ƿ�����Ϣ
						for (int j=0;j<userThreads.size();){//������һ���ͻ���
							userThread=userThreads.get(j);
							socket=userThread.getSocket();
							Mssage m=msgList.get(i);
							userThread.mssagesList.add(m);
							System.out.println("���û���Ϣ����д����Ϣ");
							System.out.println("������Ϣ����"+socket.getPort());
							j++;
						}
						msgList.remove(i);//ȥ����Ϣ���е���Ϣ
					}
					//�����ļ��б��û��Լ����ļ��б�
					for(int i=0;i<fileList.size();){
						for (int j=0;j<userThreads.size();j++){
							userThread=userThreads.get(j);
							System.out.println("fileList:"+userThread.fileList.size());
							socket=userThread.getSocket();
							File file=fileList.get(0);
							userThread.fileList.add(file);
							System.out.println("���û��ļ��б�д���ļ�Ϣ");
							System.out.println("�����ļ���Ϣ����"+socket.getPort());
						}
						fileList.remove(0);//�Ƴ��ļ����е��ļ�
					}
					
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
				try {
					Thread.sleep(80);//������Ϣ�ӳ�
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	//��ȡ����ʱ��
	private String getTime(int timeType){//iΪ1�ǳ�ʱ�䣬�������գ�0λ��ʱ��
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
			System.out.println("��ȡʱ��ʧ�ܣ�ʱ������Ϊ(0,1):"+timeType);
			serverLogs.add(new ServerLog(getTime(1), "��ȡʱ��ʧ�ܣ�ʱ������Ϊ(0,1):"+timeType));
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
	 * @param serverIp Ҫ���õ� serverIp
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
	 * @param serverPort Ҫ���õ� serverPort
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	
	
}
