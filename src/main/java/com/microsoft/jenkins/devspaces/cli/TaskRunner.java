/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.cli;


import com.microsoft.jenkins.devspaces.util.Constants;
import com.microsoft.jenkins.devspaces.util.Util;
import hudson.EnvVars;
import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskRunner {
    private static final Logger LOGGER = Logger.getLogger(TaskRunner.class.getName());
    private String taskName;
    private final TaskListener listener;

    public static final boolean isWindows = Util.isWindows();
    private static final String windowsCommand = "cmd /c %s";
    private static final String nonWindowsCommand = "%s";

    public String workDirectory;
    public RetryContext retryContext;
    public int retryTimes = 3;
    public StringBuilder outputSb = new StringBuilder();
    public StringBuilder errorSb = new StringBuilder();

    public TaskRunner(String taskName, String workDirectory, TaskListener listener) {
        this.taskName = taskName;
        this.workDirectory = workDirectory;
        this.listener = listener;
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
        int exitCode = 1;
        while (exitCode != 0 && retryTimes > 0) {

            Process process = Runtime.getRuntime().exec(wholeCommand, null, dir);
            // TODO filter credentials in log
            LOGGER.log(Level.INFO, "Execute command {0} at {1} using {2}", new String[]{taskName, workDirectory, wholeCommand});

//        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
//        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
            errorGobbler.start();
            outputGobbler.start();

            boolean hasFinished = process.waitFor(120, TimeUnit.SECONDS);
            if (!hasFinished) {
                process.destroy();
            }
            exitCode = process.exitValue();
            retryTimes--;
        }
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

    private class StreamGobbler extends Thread {
        InputStream is;

        private StreamGobbler(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    getEndpoint(line);
                    synchronized (listener) {
                        listener.getLogger().println(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void getEndpoint(String output) {
            String http = "http://";
            if (output.contains(http)) {
                String substring = output.substring(output.indexOf(http));
                EnvVars.masterEnvVars.put("dsEndpoint", substring);
            }
        }
    }
}
