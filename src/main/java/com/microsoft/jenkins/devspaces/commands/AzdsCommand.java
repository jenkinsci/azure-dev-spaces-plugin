/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.commands;

import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.devspaces.cli.AzTask;
import com.microsoft.jenkins.devspaces.cli.AzdsTask;
import com.microsoft.jenkins.devspaces.cli.TaskResult;
import com.microsoft.jenkins.devspaces.exceptions.AzureCliException;
import com.microsoft.jenkins.devspaces.util.Constants;
import hudson.EnvVars;
import org.apache.commons.lang3.StringUtils;

public class AzdsCommand implements ICommand<AzdsCommand.IAzdsData> {

    @Override
    public void execute(IAzdsData context) {
        String repoPath = context.getRepoPath();
        String spaceName = context.getSpaceName();
        String sharedSpaceName = context.getSharedSpaceName();
        String aksName = context.getAksName();
        String resourceGroupName = context.getResourceGroupName();
//        String userCredentialsId = context.getUserCredentialsId();
        TaskResult taskResult;

        JobContext jobContext = context.getJobContext();

//        StandardUsernamePasswordCredentials userPass = AzureUtil.getUserPass(jobContext.getRun().getParent(), userCredentialsId);
//        String username = userPass.getUsername();
//        Secret password = userPass.getPassword();

        AzdsTask azdsTask = new AzdsTask(jobContext.getTaskListener());
        AzTask azTask = new AzTask(jobContext.getTaskListener());

        try {
            context.logStatus("Try to login");
            taskResult = azTask.login();
//            AzTask.loginWithUserPass(username, password.getPlainText());
            context.logStatus(taskResult.getOutput());
//            if (!taskResult.isSuccess()) {
//                context.logError(taskResult.getError());
//                return;
//            }
            String space = StringUtils.isBlank(sharedSpaceName) ? spaceName : sharedSpaceName + "/" + spaceName;

            context.logStatus(String.format("prepare dev spaces for %s %s with %s at %s", resourceGroupName, aksName, spaceName, repoPath));
            taskResult = azTask.applyAzdsForAks(space, resourceGroupName, aksName, repoPath);
            context.logStatus(taskResult.getOutput());
            if (!taskResult.isSuccess()) {
                context.logError(taskResult.getError());
                return;
            }

            context.logStatus("azds prep");
            taskResult = azdsTask.prep(repoPath);
            context.logStatus(taskResult.getOutput());
            if (!taskResult.isSuccess()) {
                context.logError(taskResult.getError());
                return;
            }

            context.logStatus("azds up");
            taskResult = azdsTask.up(repoPath);
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

        String getSharedSpaceName();

        String getAksName();

        String getResourceGroupName();

        String getUserCredentialsId();
    }
}
