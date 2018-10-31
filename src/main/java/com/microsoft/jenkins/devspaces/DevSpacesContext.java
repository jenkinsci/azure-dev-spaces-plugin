/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces;

import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.devspaces.commands.AzdsCommand;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

public class DevSpacesContext extends BaseCommandContext
        implements AzdsCommand.IAzdsData {
    private String repoPath;
    private String spaceName;
    private String aksName;
    private String resourceGroupName;
    private String userCredentialsId;

    protected void configure(Run<?, ?> run,
                             FilePath workspace,
                             Launcher launcher,
                             TaskListener taskListener) {

        CommandService.Builder builder = CommandService.builder();
        builder.withStartCommand(AzdsCommand.class);

        super.configure(run, workspace, launcher, taskListener, builder.build());
    }

    @Override
    public StepExecution startImpl(StepContext context) throws Exception {
        return null;
    }

    @Override
    public IBaseCommandData getDataForCommand(ICommand command) {
        return this;
    }

    @Override
    public String getRepoPath() {
        return this.repoPath;
    }

    @Override
    public String getSpaceName() {
        return this.spaceName;
    }

    @Override
    public String getAksName() {
        return this.aksName;
    }

    @Override
    public String getResourceGroupName() {
        return this.resourceGroupName;
    }

    @Override
    public String getUserCredentialsId() {
        return this.userCredentialsId;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public void setAksName(String aksName) {
        this.aksName = aksName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public void setUserCredentialsId(String userCredentialsId) {
        this.userCredentialsId = userCredentialsId;
    }
}
