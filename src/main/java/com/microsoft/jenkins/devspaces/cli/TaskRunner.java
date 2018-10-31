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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskRunner {
    private static final Logger LOGGER = Logger.getLogger(TaskRunner.class.getName());
    private String taskName;

    public static final boolean isWindows = Util.isWindows();
    private static final String windowsCommand = "cmd /c %s";
    private static final String nonWindowsCommand = "bash %s";

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
    public TaskResult run(String command) throws IOException, InterruptedException {
        if (StringUtils.isBlank(command)) {
            return new TaskResult(taskName, outputSb.toString(), errorSb.toString(), false);
        }

        String wholeCommand = String.format(isWindows ? windowsCommand : nonWindowsCommand, command);
        File dir = StringUtils.isBlank(this.workDirectory) ? null : new File(this.workDirectory);
        Process process = Runtime.getRuntime().exec(wholeCommand, null, dir);
        // TODO filter credentials in log
        LOGGER.log(Level.INFO, "Execute command {0} at {1} using {2}", new String[]{taskName, workDirectory, wholeCommand});

        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

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
        in.close();
        error.close();
        LOGGER.log(Level.INFO, "Command {0} exits with code {1}", new Object[]{taskName, exitCode});
        return new TaskResult(taskName, outputSb.toString(), errorSb.toString(), exitCode == 0);
    }

    public TaskResult run(String command, String[] inputs) throws IOException, InterruptedException {
        if (StringUtils.isBlank(command) || inputs == null) {
            return new TaskResult(taskName, outputSb.toString(), errorSb.toString(), false);
        }
        String wholeCommand = String.format(isWindows ? windowsCommand : nonWindowsCommand, command);
        File dir = StringUtils.isBlank(this.workDirectory) ? null : new File(this.workDirectory);
        Process process = Runtime.getRuntime().exec(wholeCommand, null, dir);
        LOGGER.log(Level.INFO, "Execute command {0} at {1} using {2}", new String[]{taskName, workDirectory, wholeCommand});


        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
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
        in.close();
        error.close();
        out.close();
        LOGGER.log(Level.INFO, "Command {0} exits with code {1}", new Object[]{taskName, exitCode});
        return new TaskResult(taskName, outputSb.toString(), errorSb.toString(), exitCode == 0);
    }

    public String getOutput() {
        return this.outputSb.toString();
    }

    public String getError() {
        return this.errorSb.toString();
    }
}
