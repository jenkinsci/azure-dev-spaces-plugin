package com.microsoft.jenkins.devspaces.commands;

import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1NamespaceBuilder;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CreateDevSpaceCommand implements ICommand<CreateDevSpaceCommand.ICreateDevSpaceData> {
    private static final String AZDS_ENABLE_LABEL = "azds.io/space";
    private static final String AZDS_PARENT_SPACE_LABEL = "azds.io/parent-space";
    private static final String TRUE = "true";

    @Override
    public void execute(ICreateDevSpaceData context) {
        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            String spaceName = context.getSpaceName();
            String parentSpaceName = context.getSharedSpaceName();

//            V1Namespace namespace = api.readNamespace(spaceName, "false", false, false);
//            if (namespace != null) {
//                V1ObjectMeta metadata = namespace.getMetadata();
//                Map<String, String> labels = metadata.getLabels();
//                if (labels != null && TRUE.equals(labels.get(AZDS_ENABLE_LABEL)) && parentSpaceName.equals(labels.get(AZDS_PARENT_SPACE_LABEL))) {
//                    context.logStatus(String.format("using existing dev space %s", spaceName));
//                    context.setCommandState(CommandState.Success);
//                } else {
//                    context.logStatus(String.format("dev space %s has already exists", spaceName));
//                    context.setCommandState(CommandState.HasError);
//                }
//                return;
//            }

            Map<String, String> labels = new HashMap<>();
            labels.put(AZDS_ENABLE_LABEL, TRUE);
            labels.put(AZDS_PARENT_SPACE_LABEL, parentSpaceName);
            V1Namespace namespace = new V1NamespaceBuilder()
                    .withNewMetadata()
                    .withName(spaceName)
                    .withLabels(labels)
                    .endMetadata()
                    .build();
            context.logStatus(String.format("try to create dev space %s with parent space %s", spaceName, parentSpaceName));
            V1Namespace createdNamespace = api.createNamespace(namespace, "true");
            context.logStatus(createdNamespace.toString());
            context.setCommandState(CommandState.Success);
        } catch (IOException | ApiException e) {
            context.logError(e);
            context.setCommandState(CommandState.HasError);
        }
    }

    public interface ICreateDevSpaceData extends IBaseCommandData {
        String getSpaceName();

        String getSharedSpaceName();
    }
}
