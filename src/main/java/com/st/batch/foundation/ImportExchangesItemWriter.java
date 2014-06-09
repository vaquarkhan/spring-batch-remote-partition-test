package com.st.batch.foundation;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

import com.st.symfony.Symfony;
import com.st.symfony.SymfonyInt;

public class ImportExchangesItemWriter<T> implements ItemWriter<T> {

	long replyTimeout;

	SymfonyInt symfony;

	String logFilePath;

	public String getLogFilePath() {
		return logFilePath;
	}

	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}
	
	public SymfonyInt getSymfony() {
		return symfony;
	}

	public void setSymfony(SymfonyInt symfony) {
		this.symfony = symfony;
	}

	public long getReplyTimeout() {
		return replyTimeout;
	}

	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	@Override
	public void write(List<? extends T> exchanges) throws Exception {
		
		symfony.setLogFilePath(this.logFilePath);

		for (T exchange : exchanges) {

			 Thread.sleep(6000);
			String command = "echo" + " " + exchange.toString() + " ";

			symfony.run(command, this.replyTimeout);	
		}

	}
}
