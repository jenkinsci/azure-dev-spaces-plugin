/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.cli;

import java.io.IOException;
import java.util.regex.Pattern;

public class AzTask {
    public static final String AZURE_CLI_VERSION_COMMAND = "az -v";
    public static final String AZURE_CLI_VERSION_NAME = "Get Azure CLI version";
    public static final String SET_UP_DEV_SPACES_COMMAND = "az aks use-dev-spaces -g %s -n %s";
    public static final String SET_UP_DEV_SPACES_NAME = "Set up Dev Spaces for AKS";
    public static final String AZURE_CLI_LOGIN_SP_COMMAND = "az login --service-principal -u %s -p %s --tenant %s";
    public static final String AZURE_CLI_LOGIN_SP_NAME = "Login Azure CLI with Azure Service Principal";
    public static final String AZURE_CLI_LOGIN_COMMAND = "az login";
    public static final String AZURE_CLI_LOGIN_NAME = "Login Azure CLI Interactively";
    public static final String AZURE_CLI_LOGIN_USER_PASS_COMMAND = "az login -u %s -p %s";
    public static final String AZURE_CLI_LOGIN_USER_PASS_NAME = "Login Azure CLI with username and password";
    public static final String AZURE_CLI_AKS_AZDS_COMMAND = "az aks use-dev-spaces -s %s -y -g %s -n %s";
    public static final String AZURE_CLI_AKS_AZDS_NAME = "User Azure Dev Spaces with a managed Kubernetes cluster";

    public static void applyAzdsForAks(String spaceName, String resourceGroup, String aksName, String repoPath) {
        TaskRunner runner = new TaskRunner(AZURE_CLI_AKS_AZDS_NAME, repoPath);
        try {
            runner.run(String.format(AZURE_CLI_AKS_AZDS_COMMAND, spaceName, resourceGroup, aksName));
            System.out.println(runner.getOutput());
            System.out.println(runner.getError());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void login() {
        TaskRunner runner = new TaskRunner(AZURE_CLI_LOGIN_NAME, null);
        try {
            runner.run(AZURE_CLI_LOGIN_COMMAND);
            System.out.println(runner.getOutput());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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

    public static void loginWithUserPass(String username, String password) {
        TaskRunner runner = new TaskRunner(AZURE_CLI_LOGIN_USER_PASS_NAME, null);
        try {
            runner.run(String.format(AZURE_CLI_LOGIN_USER_PASS_COMMAND, username, password));
            System.out.println(runner.getOutput());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getVersion() {
        TaskRunner runner = new TaskRunner(AZURE_CLI_VERSION_NAME, null);
        try {
            runner.run(AZURE_CLI_VERSION_COMMAND);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        Pattern pattern = Pattern.compile("azure-cli");

        return null;
    }

    public static void setupDevSpaces(String resourceGroup, String aksName) {
        TaskRunner runner = new TaskRunner(SET_UP_DEV_SPACES_NAME, null);
        try {
            runner.run(String.format(SET_UP_DEV_SPACES_COMMAND, resourceGroup, aksName));
            System.out.println(runner.getOutput());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String getVersion(String s) {
        return s.substring(s.indexOf('(') + 1, s.indexOf(')'));
    }
}
