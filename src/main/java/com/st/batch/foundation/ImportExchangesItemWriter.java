package com.st.batch.foundation;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

import com.st.symfony.Symfony;

public class ImportExchangesItemWriter<T> implements ItemWriter<T> {

	long replyTimeout;

	Symfony symfony;

	String logFilePath;

	public String getLogFilePath() {
		return logFilePath;
	}

	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}
	
	public Symfony getSymfony() {
		return symfony;
	}

	public void setSymfony(Symfony symfony) {
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

			String command = symfony.getConsolePath() + " "
					+ "st:import exchange" + " " + exchange.toString();
			
			//System.out.println("Running:" + command + " with replyTimeout="
				//	+ this.replyTimeout);

			symfony.run(command, this.replyTimeout);	
		}

	}
}
