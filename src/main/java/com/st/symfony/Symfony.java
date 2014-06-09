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
	StringUtils strUtil;

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

	public StringUtils getStrUtil() {
		return strUtil;
	}

	public void setStrUtil(StringUtils strUtil) {
		this.strUtil = strUtil;
	}

	public String getLogFilePath() {
		return logFilePath;
	}

	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}

	private static class Worker extends Thread {
		private final Process process;
		private Integer exitValue;

		Worker(final Process process) {
			this.process = process;
		}

		public Integer getExitValue() {
			return exitValue;
		}

		public void run() {
			try {
				exitValue = process.waitFor();
			} catch (InterruptedException ignore) {
				return;
			}
		}
	}

	public String run(final String command, final long replyTimeout)
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
			//System.out.println(line + "\n");
			output.append(line + "\n");
		}

		Worker worker = new Worker(p);
		worker.start();

		try {
			worker.join(replyTimeout);
			Integer exitValue = worker.getExitValue();

			//System.out.println(exitValue);
			p.getInputStream().close();
			p.getOutputStream().close();
			p.getErrorStream().close();

			if (exitValue != null) {
				if (exitValue > 0) {
					throw new Exception(output.toString());
				} else {
					//System.out.println(output.toString());
					return output.toString();
				}
			} else {
				throw new TimeoutException();
			}

		} catch (InterruptedException ex) {
			worker.interrupt();
			Thread.currentThread().interrupt();
			throw ex;
		} finally {
			p.destroy();
		}

	}

	/*
	 * public String run(String command) throws Exception {
	 * 
	 * StringBuffer output = new StringBuffer(); Process p =
	 * Runtime.getRuntime().exec(command);
	 * 
	 * BufferedReader errReader = new BufferedReader(new InputStreamReader(
	 * p.getErrorStream())); StringBuffer errOutput = new StringBuffer(); String
	 * errLine = ""; while ((errLine = errReader.readLine()) != null) {
	 * errOutput.append(errLine + "\n"); }
	 * System.out.println(errOutput.toString());
	 * 
	 * StringBuffer inputStreamOutput = new StringBuffer(); BufferedReader
	 * inputReader = new BufferedReader(new InputStreamReader(
	 * p.getInputStream())); String line = ""; while ((line =
	 * inputReader.readLine()) != null) { inputStreamOutput.append(line + "\n");
	 * } System.out.println(inputStreamOutput.toString());
	 * 
	 * p.waitFor();
	 * 
	 * if (p.exitValue() > 0) { throw new Exception(errOutput.toString()); }
	 * 
	 * return inputStreamOutput.toString();
	 * 
	 * }
	 */

	public String run(String command) throws Exception {

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
		p.getInputStream().close();
		p.getOutputStream().close();
		p.getErrorStream().close();
		if (p.exitValue() > 0) {
			throw new Exception(output.toString());
		}

		return output.toString();

	}

	public void run(String command, String file) throws Exception {

		StringBuffer output = new StringBuffer();
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
		if (p.exitValue() > 0) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
			System.out.println(output.toString());
			System.out.println(p.exitValue());
			throw new Exception(output.toString());
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
		}

		File fileObj = new File(file);
		if (!fileObj.exists()) {
			fileObj.getParentFile().mkdirs();
		}

		FileWriter fw = new FileWriter(fileObj.getAbsoluteFile(), true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(output.toString());
		bw.close();

	}

	public Process run(String[] command) throws Exception {

		StringBuffer output = new StringBuffer();
		Process p;
		p = Runtime.getRuntime().exec(command);
		p.waitFor();
		if (p.exitValue() > 0) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
			System.out.println(output.toString());
			System.out.println(p.exitValue());
			throw new Exception(output.toString());
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
			System.out.println(output.toString());
			System.out.println(p.exitValue());
		}

		return null;

	}

	public String getOutput(String command) throws Exception {

		StringBuffer output = new StringBuffer();
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
		if (p.exitValue() > 0) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
			System.out.println(output.toString());
			System.out.println(p.exitValue());
			throw new Exception(output.toString());
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				for (String item : line.split(" ")) {
					output.append(item.trim() + "\n");
				}
			}
		}

		return output.toString();
	}

	public void outputToFile(String command, String fileLocation,
			String fileName, String writeMode) throws Exception {

	}

	public void outputItemsToFile(String command, String file, Boolean append)
			throws Exception {

		StringBuffer output = new StringBuffer();
		System.out.println("Running command : " + command);
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
		if (p.exitValue() > 0) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
			System.out.println(output.toString());
			System.out.println(p.exitValue());
			throw new Exception(output.toString());
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				for (String item : line.split(" ")) {
					output.append(item.trim() + "\n");
				}
			}
		}

		System.out.println("Writing to file" + file);
		File fileObj = new File(file);
		if (!fileObj.exists()) {
			fileObj.getParentFile().mkdirs();
		}

		FileWriter fw = new FileWriter(fileObj.getAbsoluteFile(), append);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(output.toString());
		bw.close();

	}

	public List<String> getOutputItems(String command) throws Exception {

		List<String> list = new ArrayList<String>();
		StringBuffer output = new StringBuffer();
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
		if (p.exitValue() > 0) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
			System.out.println(output.toString());
			System.out.println(p.exitValue());
			throw new Exception(output.toString());
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				for (String item : line.split(" ")) {
					list.add(item.trim());
				}
			}
		}

		return list;

	}

}
