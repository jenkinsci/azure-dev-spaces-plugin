/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.cli;

import com.microsoft.jenkins.devspaces.exceptions.AzureCliException;

import java.io.IOException;

public class AzTask {
    public static final String AZURE_CLI_VERSION_COMMAND = "az -v";
    public static final String AZURE_CLI_VERSION_NAME = "Get Azure CLI version";
    public static final String AZURE_CLI_LOGIN_SP_COMMAND = "az login --service-principal -u %s -p %s --tenant %s";
    public static final String AZURE_CLI_LOGIN_SP_NAME = "Login Azure CLI with Azure Service Principal";
    public static final String AZURE_CLI_LOGIN_COMMAND = "az login";
    public static final String AZURE_CLI_LOGIN_NAME = "Login Azure CLI Interactively";
    public static final String AZURE_CLI_LOGIN_USER_PASS_COMMAND = "az login -u %s -p %s";
    public static final String AZURE_CLI_LOGIN_USER_PASS_NAME = "Login Azure CLI with username and password";
    public static final String AZURE_CLI_SET_UP_AZDS_COMMAND = "az aks use-dev-spaces -s %s -y -g %s -n %s";
    public static final String AZURE_CLI_SET_UP_AZDS_NAME = "User Azure Dev Spaces with a managed Kubernetes cluster";

    public static TaskResult applyAzdsForAks(String spaceName, String resourceGroup, String aksName, String repoPath) throws AzureCliException {
        TaskRunner runner = new TaskRunner(AZURE_CLI_SET_UP_AZDS_NAME, repoPath);
        try {
            return runner.run(String.format(AZURE_CLI_SET_UP_AZDS_COMMAND, spaceName, resourceGroup, aksName));
        } catch (IOException | InterruptedException e) {
            throw new AzureCliException(e);
        }
    }

    public static TaskResult login() throws AzureCliException {
        TaskRunner runner = new TaskRunner(AZURE_CLI_LOGIN_NAME, null);
        try {
            return runner.run(AZURE_CLI_LOGIN_COMMAND);
        } catch (IOException | InterruptedException e) {
            throw new AzureCliException(e);
        }
    }

    public static void loginWithSP(String clientId, String tenantId, String key) {
        TaskRunner runner = new TaskRunner(AZURE_CLI_LOGIN_SP_NAME, null);
        try {
            runner.run(String.format(AZURE_CLI_LOGIN_SP_COMMAND, clientId, key, tenantId));
            System.out.println(runner.getOutput());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static TaskResult loginWithUserPass(String username, String password) throws AzureCliException {
        TaskRunner runner = new TaskRunner(AZURE_CLI_LOGIN_USER_PASS_NAME, null);
        try {
            return runner.run(String.format(AZURE_CLI_LOGIN_USER_PASS_COMMAND, username, password));
        } catch (IOException | InterruptedException e) {
            throw new AzureCliException(e);
        }
    }

    public static String getVersion() {
        TaskRunner runner = new TaskRunner(AZURE_CLI_VERSION_NAME, null);
        try {
            runner.run(AZURE_CLI_VERSION_COMMAND);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getVersion(String s) {
        return s.substring(s.indexOf('(') + 1, s.indexOf(')'));
    }
}
