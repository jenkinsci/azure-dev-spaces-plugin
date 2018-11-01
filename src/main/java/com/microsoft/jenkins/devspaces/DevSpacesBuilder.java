/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ContainerService;
import com.microsoft.azure.management.compute.ContainerServiceOrchestratorTypes;
import com.microsoft.azure.management.resources.GenericResource;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.jenkins.devspaces.util.AzureUtil;
import com.microsoft.jenkins.devspaces.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class DevSpacesBuilder extends Builder implements SimpleBuildStep {
    private String azureCredentialsId;
    @DataBoundSetter
    private String aksName;
    @DataBoundSetter
    private String resourceGroupName;
    @DataBoundSetter
    private String repoPath;
    @DataBoundSetter
    private String spaceName;
    @DataBoundSetter
    private String userCredentialsId;

    @DataBoundConstructor
    public DevSpacesBuilder(String azureCredentialsId) {
        this.azureCredentialsId = azureCredentialsId;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        DevSpacesContext commandContext = new DevSpacesContext();

        this.repoPath = StringUtils.isBlank(repoPath) ? workspace.getRemote() : workspace.child(repoPath).getRemote();
        commandContext.setRepoPath(this.repoPath);
        commandContext.setSpaceName(this.spaceName);
        commandContext.setAksName(this.aksName);
        commandContext.setResourceGroupName(this.resourceGroupName);
        commandContext.setUserCredentialsId(this.userCredentialsId);

        commandContext.configure(run, workspace, launcher, listener);

        commandContext.executeCommands();
    }

    @Override
    public final DescriptorImpl getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("devSpaces")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "dev spaces";
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath Item owner) {
            StandardListBoxModel model = new StandardListBoxModel();
            model.add("test", Constants.INVALID_OPTION);
            model.includeAs(ACL.SYSTEM, owner, AzureBaseCredentials.class);
            return model;
        }

        public ListBoxModel doFillResourceGroupNameItems(@AncestorInPath final Item owner,
                                                         @QueryParameter final String azureCredentialsId) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            // list all resource groups
            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = AzureUtil.getAzureClient(owner, azureCredentialsId);
                for (final ResourceGroup rg : azureClient.resourceGroups().list()) {
                    model.add(rg.name());
                }
            }
            return model;
        }

        public ListBoxModel doFillAksNameItems(@AncestorInPath final Item owner,
                                               @QueryParameter final String azureCredentialsId,
                                               @QueryParameter final String resourceGroupName) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            if (StringUtils.isBlank(azureCredentialsId)
                    || Constants.INVALID_OPTION.equals(azureCredentialsId)
                    || StringUtils.isBlank(resourceGroupName)
                    || Constants.INVALID_OPTION.equals(resourceGroupName)) {
                model.add(
                        "select group first",
                        Constants.INVALID_OPTION);
                return model;
            }
            final Azure azureClient = AzureUtil.getAzureClient(owner, azureCredentialsId);
            PagedList<ContainerService> containerServices =
                    azureClient.containerServices().listByResourceGroup(resourceGroupName);
            for (ContainerService containerService : containerServices) {
                ContainerServiceOrchestratorTypes orchestratorType = containerService.orchestratorType();
                if (Constants.SUPPORTED_ORCHESTRATOR.contains(orchestratorType)) {
                    model.add(containerService.name());
                }
            }

            PagedList<GenericResource> resources =
                    azureClient.genericResources().listByResourceGroup(resourceGroupName);
            for (GenericResource resource : resources) {
                if (Constants.AKS_PROVIDER.equals(resource.resourceProviderNamespace())
                        && Constants.AKS_RESOURCE_TYPE.equals(resource.resourceType())) {
                    model.add(resource.name());
                }
            }
            if (model.isEmpty()) {
                model.add("not found", Constants.INVALID_OPTION);
            }
            return model;
        }

        public ListBoxModel doFillUserCredentialsIdItems(@AncestorInPath final Item owner) {
            StandardListBoxModel model = new StandardListBoxModel();
            model.add("test", Constants.INVALID_OPTION);
            model.includeAs(ACL.SYSTEM, owner, StandardUsernamePasswordCredentials.class);
            return model;
        }
    }

    public String getAzureCredentialsId() {
        return azureCredentialsId;
    }

    public String getAksName() {
        return aksName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public String getUserCredentialsId() {
        return userCredentialsId;
    }
}
