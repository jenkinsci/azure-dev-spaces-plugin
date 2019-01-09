package com.microsoft.jenkins.devspaces.commands;

import com.google.gson.JsonSyntaxException;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1DeleteOptions;

public class CleanupDevSpaceCommand implements ICommand<CleanupDevSpaceCommand.ICleanupDevSpaceData> {
    private CoreV1Api api = new CoreV1Api();

    @Override
    public void execute(ICleanupDevSpaceData context) {
        // TODO parent dev space deletion
        String spaceName = context.getSpaceName();
        V1DeleteOptions deleteOptions = new V1DeleteOptions();
        try {
            api.deleteNamespace(spaceName, deleteOptions, "true", 0, true, null);
        } catch (JsonSyntaxException je) {
            // Ignore it temporarily for https://github.com/kubernetes-client/java/issues/86
        } catch (ApiException e) {
            e.printStackTrace();
            context.logError("Failed to clean up devSpace ", e);
            context.setCommandState(CommandState.HasError);
        }
        context.setCommandState(CommandState.Success);
    }

    public interface ICleanupDevSpaceData extends IBaseCommandData {
        String getSpaceName();

        String getResourceGroupName();

        String getAksName();
    }
}
