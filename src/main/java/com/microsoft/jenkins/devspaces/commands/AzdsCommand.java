/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.commands;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.devspaces.cli.AzTask;
import com.microsoft.jenkins.devspaces.cli.AzdsTask;
import com.microsoft.jenkins.devspaces.cli.TaskResult;
import com.microsoft.jenkins.devspaces.exceptions.AzureCliException;
import com.microsoft.jenkins.devspaces.util.AzureUtil;
import hudson.util.Secret;

public class AzdsCommand implements ICommand<AzdsCommand.IAzdsData> {

    @Override
    public void execute(IAzdsData context) {
        String repoPath = context.getRepoPath();
        String spaceName = context.getSpaceName();
        String aksName = context.getAksName();
        String resourceGroupName = context.getResourceGroupName();
        String userCredentialsId = context.getUserCredentialsId();
        TaskResult taskResult;

        StandardUsernamePasswordCredentials userPass = AzureUtil.getUserPass(context.getJobContext().getRun().getParent(), userCredentialsId);
        String username = userPass.getUsername();
        Secret password = userPass.getPassword();

        try {
            context.logStatus("Try to login");
            taskResult = AzTask.login();
//            AzTask.loginWithUserPass(username, password.getPlainText());
            context.logStatus(taskResult.getOutput());
            if (!taskResult.isSuccess()) {
                context.logError(taskResult.getError());
                return;
            }

            context.logStatus(String.format("prepare dev spaces for %s %s with %s at %s", resourceGroupName, aksName, spaceName, repoPath));
            taskResult = AzTask.applyAzdsForAks(spaceName, resourceGroupName, aksName, repoPath);
            context.logStatus(taskResult.getOutput());
            if (!taskResult.isSuccess()) {
                context.logError(taskResult.getError());
                return;
            }

            context.logStatus("azds prep");
            taskResult = AzdsTask.prep(repoPath);
            context.logStatus(taskResult.getOutput());
            if (!taskResult.isSuccess()) {
                context.logError(taskResult.getError());
                return;
            }

            context.logStatus("azds up");
            taskResult = AzdsTask.up(repoPath);
            context.logStatus(taskResult.getOutput());
            if (!taskResult.isSuccess()) {
                context.logError(taskResult.getError());
                return;
            }
        } catch (AzureCliException e) {
            return;
        }


        context.setCommandState(CommandState.Success);
    }

    public interface IAzdsData extends IBaseCommandData {
        String getRepoPath();

        String getSpaceName();

        String getAksName();

        String getResourceGroupName();

        String getUserCredentialsId();
    }
}
