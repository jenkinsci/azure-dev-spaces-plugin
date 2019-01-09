package com.microsoft.jenkins.devspaces;

import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.devspaces.commands.CleanupDevSpaceCommand;
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

public class DevSpacesPublisherContext extends BaseCommandContext
        implements CleanupDevSpaceCommand.ICleanupDevSpaceData {
    private String spaceName;
    private String aksName;
    private String resourceGroupName;
    private String kubeConfig;

    protected void configure(Run<?, ?> run,
                             FilePath workspace,
                             Launcher launcher,
                             TaskListener taskListener) {

        // Set up kubernetes client configuration
        StringReader reader = new StringReader(getKubeConfig());
        ApiClient client = null;
        try {
            client = Config.fromConfig(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration.setDefaultApiClient(client);

        CommandService.Builder builder = CommandService.builder();
        builder.withStartCommand(CleanupDevSpaceCommand.class);

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

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public void setAksName(String aksName) {
        this.aksName = aksName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public void setKubeConfig(String kubeConfig) {
        this.kubeConfig = kubeConfig;
    }

    @Override
    public String getSpaceName() {
        return this.spaceName;
    }

    @Override
    public String getResourceGroupName() {
        return this.resourceGroupName;
    }

    @Override
    public String getAksName() {
        return this.aksName;
    }

    public String getKubeConfig() {
        return kubeConfig;
    }
}
