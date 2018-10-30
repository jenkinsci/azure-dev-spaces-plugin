/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.cli;


import com.microsoft.jenkins.devspaces.util.Constants;
import com.microsoft.jenkins.devspaces.util.Util;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskRunner {
    private static final Logger LOGGER = Logger.getLogger(TaskRunner.class.getName());
    private String taskName;

    public static final boolean isWindows = Util.isWindows();
    private static final String windowsCommand = "cmd /c %s";
    private static final String nonWindowsCommand = "bash -c %s";

    public String workDirectory;
    public RetryContext retryContext;
    public int retryTimes;
    public StringBuilder outputSb = new StringBuilder();
    public StringBuilder errorSb = new StringBuilder();

    public TaskRunner(String taskName, String workDirectory) {
        this.taskName = taskName;
        this.workDirectory = workDirectory;
    }

    /**
     * Run a command in different platforms.
     *
     * @param command command needs to be run
     * @throws IOException
     * @throws InterruptedException
     */
    public void run(String command) throws IOException, InterruptedException {
        if (StringUtils.isBlank(command)) {
            return;
        }

        String wholeCommand = String.format(isWindows ? windowsCommand : nonWindowsCommand, command);
        File dir = StringUtils.isBlank(this.workDirectory) ? null : new File(this.workDirectory);
        Process process = Runtime.getRuntime().exec(wholeCommand, null, dir);
        // TODO filter credentials in log
        LOGGER.log(Level.INFO, "Execute command {0} at {1} using {2}", new String[]{taskName, workDirectory, wholeCommand});

        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = in.readLine()) != null) {
            this.outputSb.append(line);
            this.outputSb.append(Constants.LINE_SEPARATOR);
        }
        while ((line = error.readLine()) != null) {
            this.errorSb.append(line);
            this.errorSb.append(Constants.LINE_SEPARATOR);
        }
        int exitCode = process.waitFor();
        LOGGER.log(Level.INFO, "Command {0} exits with code {1}", new Object[]{taskName, exitCode});
    }

    public void run(String command, String[] inputs) throws IOException, InterruptedException {
        if (StringUtils.isBlank(command) || inputs == null) {
            return;
        }
        String wholeCommand = String.format(isWindows ? windowsCommand : nonWindowsCommand, command);
        File dir = StringUtils.isBlank(this.workDirectory) ? null : new File(this.workDirectory);
        Process process = Runtime.getRuntime().exec(wholeCommand, null, dir);
        LOGGER.log(Level.INFO, "Execute command {0} at {1} using {2}", new String[]{taskName, workDirectory, wholeCommand});


        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        String line;

        for (String input : inputs) {
            out.write(input + "\n");
            out.flush();

            while ((line = in.readLine()) != null) {
                this.outputSb.append(line);
                this.outputSb.append(Constants.LINE_SEPARATOR);
            }
            while ((line = error.readLine()) != null) {
                this.errorSb.append(line);
                this.errorSb.append(Constants.LINE_SEPARATOR);
            }
        }

        int exitCode = process.waitFor();
        LOGGER.log(Level.INFO, "Command {0} exits with code {1}", new Object[]{taskName, exitCode});
    }

    public String getOutput() {
        return this.outputSb.toString();
    }

    public String getError() {
        return this.errorSb.toString();
    }

    public static void setUpStreamGobbler(final InputStream is, final PrintStream ps) {
        final InputStreamReader streamReader = new InputStreamReader(is);
        new Thread(new Runnable() {
            public void run() {
                BufferedReader br = new BufferedReader(streamReader);
                String line = null;
                try {
                    while ((line = br.readLine()) != null) {
                        ps.println("process stream: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
