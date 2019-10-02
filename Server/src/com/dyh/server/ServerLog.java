package com.dyh.server;

public class ServerLog {
	public String occurTime;//发生异常的时间
	public String  occurMsg;//
	
	public ServerLog(String occurTime, String occurMsg) {
		super();
		this.occurTime = occurTime;
		this.occurMsg = occurMsg;
	}
	
}
