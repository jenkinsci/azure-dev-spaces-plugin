/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces;

import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.devspaces.commands.CreateDevSpaceCommand;
import com.microsoft.jenkins.devspaces.commands.GetEndpointCommand;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.util.Config;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.io.IOException;
import java.io.StringReader;

public class DevSpacesBuilderContext extends BaseCommandContext
        implements CreateDevSpaceCommand.ICreateDevSpaceData,
        GetEndpointCommand.IGetEndpointData {
    private String spaceName;
    private String sharedSpaceName;
    private String namespace;
    private String endpointVariable;
    private String kubeconfig;


    protected void configure(Run<?, ?> run,
                             FilePath workspace,
                             Launcher launcher,
                             TaskListener taskListener) {

        // Set up kubernetes client configuration
        StringReader reader = new StringReader(getKubeconfig());
        ApiClient client = null;
        try {
            client = Config.fromConfig(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration.setDefaultApiClient(client);

        CommandService.Builder builder = CommandService.builder();
        builder.withStartCommand(CreateDevSpaceCommand.class);
//        builder.withTransition(DeployHelmChartCommand.class, GetEndpointCommand.class);

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
    public String getSpaceName() {
        return this.spaceName;
    }

    @Override
    public String getSharedSpaceName() {
        return this.sharedSpaceName;
    }

    @Override
    public String getKubeconfig() {
        return this.kubeconfig;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public String getEndpointVariable() {
        return endpointVariable;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public void setSharedSpaceName(String sharedSpaceName) {
        this.sharedSpaceName = sharedSpaceName;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setEndpointVariable(String endpointVariable) {
        this.endpointVariable = endpointVariable;
    }

    public void setKubeconfig(String kubeconfig) {
        this.kubeconfig = kubeconfig;
    }

}
