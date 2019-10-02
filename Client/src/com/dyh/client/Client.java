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
	
	public static boolean isOpenClient=false;//�жϿͻ����Ƿ�ر�,�ܿ���
	public boolean isEndRecordLog=false;//�Ƿ��˳���־�߳�
	private String ServerIp;
	private int ServerPort;
	private Socket downLoadSocket;//�����ļ����׽���
	private Socket uploadSocket;//�ϴ��ļ��õ��׽���
	private Socket fileSocket;//������ļ��׽��֣����ر�
	private int LocalPort;
	private Socket socket;
	public static ArrayList<String> msgList=new ArrayList<String>();//��������������Ϣ�洢
	public static ArrayList<ClientLog> clientLogs=new ArrayList<ClientLog>();//������Ϣ
	public static ArrayList<String> fileList=new ArrayList<String>();//���ص��ļ��б��ӷ������õ���
	private RecordLogThread recordLogThread;
	public void OpenClient(String ServerIp,int ServerPort){
		
		this.ServerIp=ServerIp;//�õ�������Ip
		this.ServerPort=ServerPort;//�õ��������˿�
		
		ConnectServerThread connectServerThread=new ConnectServerThread();
		connectServerThread.setName("���ӷ�����");
		connectServerThread.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		//��¼��־�������̣߳�д�뵽����
		recordLogThread=new RecordLogThread("./log");
		recordLogThread.setName("��־��¼");
		recordLogThread.start();
	}
	
	
	public void downLoadFile(String fileName,String savePath){
		DownLoadFile downLoadFile=new DownLoadFile(fileName, savePath);
		downLoadFile.start();
	}
	//�����ļ�
	
	public class DownLoadFile extends Thread{//��ȡ�������ϵ��ļ���,���汾�ص�·��
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
			SocketAddress address=new InetSocketAddress(ServerIp, 6666);//�����ļ��˿ڹ̶�
			downLoadSocket.connect(address,3000);//�ȴ�3��
			//����һ���ļ�������������·��
			OutputStream outputStream=downLoadSocket.getOutputStream();
			outputStream.write((fileName+"\r\n").getBytes());//��Ҫ���ص��ļ�����·����������
			System.out.println("fileName="+fileName);
			Thread.sleep(100);//�ȴ�������׼������������շ��������صĴ�С��Ϣ
			
			InputStream inputStream=downLoadSocket.getInputStream();
			BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
			//�յ������������Ĵ�С��Ϣ
			long fileLength=Long.parseLong(reader.readLine());
			//����������С���͹���
			File dir=new File(savePath);//�ж��ļ����Ƿ����
			if(!dir.exists()){
				dir.mkdir();
				System.out.println("��������Ŀ¼");
			}
			int index=fileName.lastIndexOf("\\");
			System.out.println("index="+index);
			//�����ļ�׼�������ļ�
			String saveFileName= fileName.substring(index+1, fileName.length()) ;//�����ļ�
			System.out.println("�����ļ���"+ saveFileName);
			File saveFile =new File(savePath, saveFileName);
			
			BufferedInputStream bufferedInputStream=new BufferedInputStream(inputStream);
			DataInputStream dataInputStream=new DataInputStream(bufferedInputStream);
			
			FileOutputStream fileOutputStream=new FileOutputStream(saveFile);
			BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(fileOutputStream);
			DataOutputStream dataOutputStream=new DataOutputStream(bufferedOutputStream);
			System.out.println("�����ļ�...");
			byte []buff=new byte[1024];
			dataInputStream.read(buff);
			while(true){
				dataOutputStream.write(buff);
				dataOutputStream.flush();
				if(saveFile.length()>=fileLength){
					break;
				}
				System.out.println("�ͻ��������ļ�");
				dataInputStream.read(buff);
			}
			dataOutputStream.close();
			System.out.println("���յ���"+saveFileName.length()+"B");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		}
			
	}
	
	public void sendFile(String filePath){
		SendFile sendFile=new SendFile(filePath);
		sendFile.start();
	}
	
	//�ϴ��ļ����߳�
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
				SocketAddress address=new InetSocketAddress(ServerIp, 5555);//�ϴ��ļ��˿ڹ̶�
				uploadSocket.connect(address, 3000);//�������˷����ļ����׽���
				//���ļ�����Ϣ�ϴ�
				//�����ļ���С//�����ļ�����
				OutputStream out=uploadSocket.getOutputStream();
				File sendFile=new File(filePath);
				String fileName=sendFile.getName();
				long fileSize=sendFile.length();
				out.write((fileName+"|"+fileSize+"\r\n").getBytes());//���ͳ�ȥ�ļ������ļ���С
				DataOutputStream dataOutputStream=new DataOutputStream(out);
				//�ȴ������������ļ��е�ʱ��
				Thread.sleep(3000);
				try {
					FileInputStream fileInputStream=new FileInputStream(sendFile);
					BufferedInputStream bufferedInputStream=new BufferedInputStream(fileInputStream);
					DataInputStream dataInputStream=new DataInputStream(bufferedInputStream);

					byte []bite=new byte[1024];//���յ�1024�ֽڷ���һ��
					System.out.println("��ʼ�ϴ��ļ���....");
					while((dataInputStream.read(bite))!=-1){
						dataOutputStream.write(bite);
						System.out.println("���ϴ�:"+bite.length+" �ֽ�");
						dataOutputStream.flush();
					}
					System.out.println("�ϴ��ļ���"+sendFile.getName()+"\t��С��"+ dataOutputStream.size()/1024+"KB");
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
				//����Server
				socket=new Socket();
				SocketAddress address =new InetSocketAddress(ServerIp, ServerPort);
				socket.connect(address, 3000);//���ó�ʱ
				isOpenClient=true;
				
				//���ӷ��������ļ��߳�
				fileSocket =new Socket();
				SocketAddress fileaddress =new InetSocketAddress(ServerIp, 3333);
				fileSocket.connect(fileaddress, 3000);//���ó�ʱ
				
				//�ͻ��˽�����Ϣ
				RecvMsgThread recvMsgThread=new RecvMsgThread();
				recvMsgThread.setName("������Ϣ");
				recvMsgThread.start();
				
				//�ͻ��˽����ļ�
				RecvFileThread recvFileThread=new RecvFileThread();
				recvFileThread.setName("���շ��������ļ�");
				recvFileThread.start();
				
				LocalPort = socket.getLocalPort();//�õ����صĵĶ˿�
			} catch (Exception e) {
				clientLogs.add(new ClientLog(getTime(), "���ӷ�����ʧ�ܣ�"+e.getMessage()+"�������ܾ�����"));
				System.out.println("���ӷ�����ʧ�ܣ�"+e.getMessage());
				e.printStackTrace();
				try {
					Thread.sleep(5000);//Ҫ������,�ȴ���־��¼���ļ�
					isEndRecordLog=true;//����־�̷߳��ͽ����ź�
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
			while(isOpenClient==true){//�ж��Ƿ�ر�,�ر����߳̾ͽ���
				try {
					//������Ϣ
					InputStream in=fileSocket.getInputStream();
					BufferedReader reader=new BufferedReader(new InputStreamReader(in));
					String RecvFile= reader.readLine();
					if(RecvFile!=null){
						fileList.add(RecvFile);
						System.out.println("���Է��������ļ���Ϣ��"+fileList.get(0));
					}
					RecvFile=null;
				} catch (Exception e) {
					e.getStackTrace();
					clientLogs.add(new ClientLog(getTime(), "�������ѹرգ�"+e.getMessage()));
					System.err.println("�쳣��"+e.getMessage()+"�˳������ļ�");
					isOpenClient=false;
					return;
				}
			}
		}
		
	}
	 
	
	
	public void SendMsg(String SetMsg){
		//������Ϣ��������
		OutputStream out;
		try {
			out = socket.getOutputStream();
			try {
				Thread.sleep(80);
				out.write((SetMsg+"\r\n").getBytes());
				out.flush();
			} catch (Exception e) {
				clientLogs.add(new ClientLog(getTime(), "������Ϣʧ�ܣ�"+e.getMessage()));
				System.out.println("�쳣��"+e.getMessage());
				isOpenClient=false;
			}
		} catch (IOException e) {
			clientLogs.add(new ClientLog(getTime(), "�Ѵӷ������Ͽ�����"+e.getMessage()));
			e.printStackTrace();
		}
	}
	
	//��¼��־�߳�
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
			while(isEndRecordLog==false){
				try {
					try {
						wr = new FileWriter(logFile,true);//true Ϊ׷��ģʽ
						writer=new BufferedWriter(wr);
						
					} catch (IOException e1) {
						clientLogs.add(new ClientLog(getTime(), "���ļ�����"+e1.getMessage()));
						e1.printStackTrace();
					}
					while(isEndRecordLog==false){
						//��ȡ��־��Ϣ
						for(int i=0;i<clientLogs.size();){
							ClientLog serverLog=clientLogs.get(0);
							writer.write(serverLog.occurTime+"\t"+serverLog.occurMsg);
							writer.newLine();
							writer.flush();
							clientLogs.remove(0);
						}
						sleep(500);//д��һ����־���
					}
				} catch (Exception e) {
					System.out.println("д��־����"+e.getMessage());
				}finally {
					try {
						wr.close();
						writer.close();
						System.out.println("�ر���־д����");
					} catch (IOException e) {
						System.out.println("�ر���ʧ��");
						clientLogs.add(new ClientLog(getTime(), "�ر���־д����ʧ�ܣ�"+e.getMessage()));
						e.printStackTrace();
					}
				}
			}
		}
	}
	public String RecvMsg;
	//�ͻ��˽�����Ϣ
	class RecvMsgThread extends Thread{
		@Override
		public void run() {
			super.run();
			while(isOpenClient==true){//�ж��Ƿ�ر�,�ر����߳̾ͽ���
				try {
					//������Ϣ
					InputStream in=socket.getInputStream();
					BufferedReader reader=new BufferedReader(new InputStreamReader(in));
					RecvMsg= reader.readLine();
					if(RecvMsg!=null){
						System.out.println("���Է���������Ϣ��"+RecvMsg);
						String temp="";
						//��ȡ�ڶ��������־
						int m=RecvMsg.indexOf("��");
						temp+=RecvMsg.substring(0, m+2)+"\n";
						while(true){
							char[] c=RecvMsg.toCharArray();
							//����Ϣ��ϣ�����س�������
							for(int i=m+2;i<c.length;i++){
								temp+=c[i];
							}
							break;
						}
						msgList.add(temp);
					}
					RecvMsg=null;
				} catch (Exception e) {
					clientLogs.add(new ClientLog(getTime(), "�������ѹرգ�"+e.getMessage()));
					System.err.println("�쳣��"+e.getMessage()+"�˳�����");
					isOpenClient=false;
					return;
				}
			}
		}
	}
	
	//��ȡ����ʱ��
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
	 * @param serverIp Ҫ���õ� serverIp
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
	 * @param serverPort Ҫ���õ� serverPort
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
	 * @param localPort Ҫ���õ� localPort
	 */
	public void setLocalPort(int localPort) {
		LocalPort = localPort;
	}
}
