/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.st.symfony;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;

public class Symfony {

	private String dir;
	private String consolePath;
	String logFilePath;

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public String getConsolePath() {
		return consolePath;
	}

	public void setConsolePath(String consolePath) {
		this.consolePath = consolePath;
	}


	public String getLogFilePath() {
		return logFilePath;
	}

	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}


	public void run(final String command, final long replyTimeout)
			throws Exception {

		String[] commands = command.split("\\s+");

		ProcessBuilder pb = new ProcessBuilder(commands);
		File log = new File(this.logFilePath);
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		Process p = pb.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		String line = null;
		StringBuilder output = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			System.out.println(line + "\n");
			output.append(line + "\n");
		}
		
		p.waitFor();

	}

}
