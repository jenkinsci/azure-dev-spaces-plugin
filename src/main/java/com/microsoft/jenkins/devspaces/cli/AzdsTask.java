/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.cli;

import com.microsoft.jenkins.devspaces.exceptions.AzureCliException;

import java.io.IOException;

public class AzdsTask {
    public static final String AZDS_PREP_COMMAND = "azds prep --public";
    public static final String AZDS_PREP_NAME = "Prepare directory for use with Azure Dev Spaces";
    public static final String AZDS_UP_COMMAND = "azds up -d";
    public static final String AZDS_UP_NAME = "Start or refresh a dev space workload";
    public static final String AZDS_SELECT_SPACE_COMMAND = "azds space -n %s -y";
    public static final String AZDS_SELECT_SPACE_NAME = "Select a new or existing dev space";
    public static final String AZDS_LIST_SPACE_COMMAND = "azds list";
    public static final String AZDS_LIST_SPACE_NAME = "List dev spaces for the current target";

    public static TaskResult selectSpace(String spaceName, String repoPath) throws AzureCliException {
        TaskRunner runner = new TaskRunner(AZDS_SELECT_SPACE_NAME, repoPath);
        try {
            return runner.run(String.format(AZDS_SELECT_SPACE_COMMAND, spaceName));
        } catch (IOException | InterruptedException e) {
            throw new AzureCliException(e);
        }
    }

    public static void listSpace(String repoPath) {
        TaskRunner runner = new TaskRunner(AZDS_LIST_SPACE_NAME, repoPath);
        try {
            runner.run(AZDS_LIST_SPACE_COMMAND);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static TaskResult prep(String repoPath) throws AzureCliException {
        TaskRunner runner = new TaskRunner(AZDS_PREP_NAME, repoPath);
        try {
            return runner.run(AZDS_PREP_COMMAND);
        } catch (IOException | InterruptedException e) {
            throw new AzureCliException(e);
        }
    }

    public static TaskResult up(String repoPath) throws AzureCliException {
        TaskRunner runner = new TaskRunner(AZDS_UP_NAME, repoPath);
        try {
            return runner.run(AZDS_UP_COMMAND);
        } catch (IOException | InterruptedException e) {
            throw new AzureCliException(e);
        }
    }
}
