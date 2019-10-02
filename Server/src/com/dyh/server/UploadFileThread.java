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
			//��ȡ������
			InputStream in =socket.getInputStream();
			//��ȡ�ļ��Ĵ�С��������Ϣ
			BufferedReader reader=new BufferedReader(new InputStreamReader(in));
			String fileInfo=reader.readLine();
			int index =fileInfo.indexOf("|");//��ȡ��һ�γ��ַָ�����λ��
			String fileName=fileInfo.substring(0, index);
			
			long fileLength=Long.parseLong(fileInfo.substring(index+1, fileInfo.length()));
			
			System.out.println("�ļ�����"+fileName+"\t�ļ���С��"+fileLength+"b");
			//���ݶ˿ںŴ����ļ���
			File fileDir=new File("./files/"+socket.getPort());
			if(!fileDir.exists()){
				boolean isCreaded= fileDir.mkdirs();
				System.out.println("�����ļ���:"+isCreaded);
			}
			File saveFile=new File("./files/"+socket.getPort()+"/"+fileName);
			if(!saveFile.exists()){
				System.out.println("�����ļ���"+saveFile.createNewFile());
			}
			DataInputStream dataInputStream=new DataInputStream(in);
			//���ļ����浽���ش���
			File outFile =new File(saveFile.toString());
			FileOutputStream outputStream=new FileOutputStream(outFile);
			BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(outputStream);
			DataOutputStream dataOutputStream=new DataOutputStream(bufferedOutputStream);
			byte []bite=new byte[1024];//������1024�ֽ�д��һ��
			
			dataInputStream.read(bite);
			while(true){
				outputStream.write(bite);
				System.out.println("�����������ļ�����������"+outFile.getName()+"��С��"+outFile.length()+" �ֽ�");
				dataOutputStream.flush();
				if(outFile.length()>=fileLength){//��С����1024֤��û�������ˣ�����ĩβ
					break;
				}
				dataInputStream.read(bite);
			}
			System.out.println("�������ѽ��գ�"+outFile.length()/1024+"KB");
			dataInputStream.close();
			dataOutputStream.close();
			
			//���ļ������ڷ������ˣ��ӵ�������
			Server.fileList.add(saveFile);
			
			System.out.println(Server.fileList.get(0).getName());
			System.out.println("�ܴ�С��"+outFile.length()/1024+"KB");
		} catch (Exception e) {
			
		}
	}
}
