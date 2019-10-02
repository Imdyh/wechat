package com.dyh.server;

public class UserInfo {
	private String UserName;
	private char UserSex;
	private String loginTime;//上线时间
	private String userIp;
	private int UserPort;
	public UserInfo(String userName, char userSex, String loginTime, String userIp, int userPort) {
		super();
		UserName = userName;
		UserSex = userSex;
		this.loginTime = loginTime;
		this.userIp = userIp;
		UserPort = userPort;
	}
	/**
	 * @return userName
	 */
	public String getUserName() {
		return UserName;
	}
	/**
	 * @param userName 要设置的 userName
	 */
	public void setUserName(String userName) {
		UserName = userName;
	}
	/**
	 * @return userSex
	 */
	public char getUserSex() {
		return UserSex;
	}
	/**
	 * @param userSex 要设置的 userSex
	 */
	public void setUserSex(char userSex) {
		UserSex = userSex;
	}
	/**
	 * @return loginTime
	 */
	public String getLoginTime() {
		return loginTime;
	}
	/**
	 * @param loginTime 要设置的 loginTime
	 */
	public void setLoginTime(String loginTime) {
		this.loginTime = loginTime;
	}
	/**
	 * @return userIp
	 */
	public String getUserIp() {
		return userIp;
	}
	/**
	 * @param userIp 要设置的 userIp
	 */
	public void setUserIp(String userIp) {
		this.userIp = userIp;
	}
	/**
	 * @return userPort
	 */
	public int getUserPort() {
		return UserPort;
	}
	/**
	 * @param userPort 要设置的 userPort
	 */
	public void setUserPort(int userPort) {
		UserPort = userPort;
	}
	
	
}
