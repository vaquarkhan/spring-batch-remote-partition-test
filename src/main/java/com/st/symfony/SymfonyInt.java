package com.st.symfony;

public interface SymfonyInt {

	void run(String command, long replyTimeout) throws Exception;

	void setLogFilePath(String logFilePath);

}
