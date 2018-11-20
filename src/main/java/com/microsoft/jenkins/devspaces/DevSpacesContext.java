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
import com.microsoft.jenkins.devspaces.commands.CreateDevSpaceCommand;
import com.microsoft.jenkins.devspaces.commands.DeployHelmChartCommand;
import com.microsoft.jenkins.devspaces.commands.GetEndpointCommand;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

public class DevSpacesContext extends BaseCommandContext
        implements AzdsCommand.IAzdsData,
        CreateDevSpaceCommand.ICreateDevSpaceData,
        DeployHelmChartCommand.IDeployHelmChartData,
        GetEndpointCommand.IGetEndpointData {
    private String repoPath;
    private String spaceName;
    private String sharedSpaceName;
    private String aksName;
    private String resourceGroupName;
    @Deprecated
    private String userCredentialsId;
    private String helmChartLocation;
    private String imageRepository;
    private String imageTag;
    private String namespace;
    private String endpointVariable;

    protected void configure(Run<?, ?> run,
                             FilePath workspace,
                             Launcher launcher,
                             TaskListener taskListener) {

        CommandService.Builder builder = CommandService.builder();
//        builder.withStartCommand(AzdsCommand.class);

        builder.withStartCommand(CreateDevSpaceCommand.class);
        builder.withTransition(CreateDevSpaceCommand.class, DeployHelmChartCommand.class);
        builder.withTransition(DeployHelmChartCommand.class, GetEndpointCommand.class);

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
    public String getSharedSpaceName() {
        return this.sharedSpaceName;
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

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public String getImageRepository() {
        return this.imageRepository;
    }

    public String getHelmChartLocation() {
        return helmChartLocation;
    }

    @Override
    public String getEndpointVariable() {
        return endpointVariable;
    }

    @Override
    public String getImageTag() {
        return this.imageTag;
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

    public void setSharedSpaceName(String sharedSpaceName) {
        this.sharedSpaceName = sharedSpaceName;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setHelmChartLocation(String helmChartLocation) {
        this.helmChartLocation = helmChartLocation;
    }

    public void setImageRepository(String imageRepository) {
        this.imageRepository = imageRepository;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public void setEndpointVariable(String endpointVariable) {
        this.endpointVariable = endpointVariable;
    }
}
