package com.dyh.server;

public class Mssage {
	public String Msg_Time;
	public String Msg_Text;
	public String msg_Who;
	
	public Mssage(String msg_Time, String msg_Text, String msg_Who) {
		super();
		Msg_Time = msg_Time;
		Msg_Text = msg_Text;
		this.msg_Who = msg_Who;
	}
	
}