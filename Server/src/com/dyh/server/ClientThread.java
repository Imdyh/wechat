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

// ���пͻ�������ʱ������һ���߳�
public class ClientThread extends Thread{
	//�û�����Ϣ����
	public ArrayList<Mssage> mssagesList =new ArrayList<Mssage>();
	public ArrayList<File> fileList =new ArrayList<File>();//�ļ��б�
	public boolean userIsLive=true;//�ж��û��Ƿ��ѶϿ�
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
		//����ת�����ܣ�ת����Ϣ
		BroadCast broadCast=new BroadCast();
		broadCast.setName(socket.toString()+"���Լ��Ŀͻ���ת����Ϣ");
		broadCast.start();
		
		//ת���ļ�
		BroadCastFile broadCastFile=new BroadCastFile();
		broadCastFile.setName(socket.toString()+"ת���ļ���Ϣ�߳�");
		broadCastFile.start();
			
		while(userIsLive==true){//���տͻ�����Ϣ
			try {
				//��ȡ����������ȡ�ͻ��˵���Ϣ
				InputStream in=socket.getInputStream();
				//��ȡ�ͻ�����Ϣ��������������ȡ��Ϣ
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				//��ȡ���յ�����Ϣ
				String msg=reader.readLine();
				if(msg!=null){
					System.out.println("���յ�����["+socket.getPort()+"]����Ϣ,����Ϊ:"+msg+"");
					//����Ϣ�ӵ���Ϣ����
					Server.msgList.add(new Mssage(getTime(0), msg, (socket.getInetAddress()+"."+socket.getPort())));
				}
			}catch (Exception e) {
				e.printStackTrace();
				Server.msgList.add(new Mssage(getTime(1), "�û���"+socket.getInetAddress()+"."+socket.getPort()+"���˳����죡", "ϵͳ��ʾ"));
				System.err.println(socket.getPort()+"�Ͽ�����");
				userIsLive=false;
			}
		}
	}
	
	//���������ϵ��ļ���Ϣ���͵��ͻ��ˣ�һֱ����ת��״̬
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
					System.out.println("��ʼ�����ļ���Ϣ");
					File file=fileList.get(0);
					try {
						OutputStream out=fileSocket.getOutputStream();
						out.write((file.toString()+"\r").getBytes());//��ͻ��˷�����Ϣ
						out.flush();
						fileList.remove(0);
					} catch (IOException e) {
						System.err.println("�����ļ���Ϣ���Լ�ʧ�ܣ�"+e.getMessage());
						userIsLive=false;
						break;
					}
				}
			}	
		}
	}
	//���Լ�������Ϣ
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
					System.out.println("��ʼ������Ϣ");
					Mssage m=mssagesList.get(0);
					try {
						OutputStream out=socket.getOutputStream();
						out.write((m.msg_Who+" ʱ��"+m.Msg_Time+"��:"+m.Msg_Text+"\n").getBytes());
						out.flush();
						mssagesList.remove(0);
					} catch (IOException e) {
						System.err.println("���͸��Լ�ʧ�ܣ�"+e.getMessage());
						userIsLive=false;
						break;
					}
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
			Server.serverLogs.add(new ServerLog(getTime(1), "��ȡʱ��ʧ�ܣ�ʱ������Ϊ(0,1):"+timeType));
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