package com.microsoft.jenkins.devspaces.commands;

import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.EnvironmentInjector;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1NamespaceBuilder;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class CreateDevSpaceCommand implements ICommand<CreateDevSpaceCommand.ICreateDevSpaceData> {
    private static final String AZDS_ENABLE_LABEL = "azds.io/space";
    private static final String AZDS_PARENT_SPACE_LABEL = "azds.io/parent-space";
    private static final String TRUE = "true";
    private static final String AZDS_SPACE_PREFIX = "azdsspace";

    @Override
    public void execute(ICreateDevSpaceData context) {
        try {
            StringReader reader = new StringReader(context.getKubeconfig());
            ApiClient client = Config.fromConfig(reader);
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            String spaceName = context.getSpaceName();
            String parentSpaceName = context.getSharedSpaceName();

            String spacePrefix = String.format("%s.s", spaceName);
            EnvironmentInjector.inject(context.getJobContext().getRun(), context.getEnvVars(), AZDS_SPACE_PREFIX, spacePrefix);
            context.logStatus(String.format("bind environment variable %s with %s", AZDS_SPACE_PREFIX, spacePrefix));

            V1Namespace namespace = null;
            try {
                namespace = api.readNamespace(spaceName, "false", false, false);
            } catch (ApiException ignore) {
            }
            if (namespace != null) {
                V1ObjectMeta metadata = namespace.getMetadata();
                Map<String, String> labels = metadata.getLabels();
                if (labels != null && TRUE.equals(labels.get(AZDS_ENABLE_LABEL)) && parentSpaceName.equals(labels.get(AZDS_PARENT_SPACE_LABEL))) {
                    context.logStatus(String.format("using existing dev space %s", spaceName));
                    context.setCommandState(CommandState.Success);
                } else {
                    context.logStatus(String.format("dev space %s has already exists", spaceName));
                    context.setCommandState(CommandState.HasError);
                }
                return;
            }

            Map<String, String> labels = new HashMap<>();
            labels.put(AZDS_ENABLE_LABEL, TRUE);
            labels.put(AZDS_PARENT_SPACE_LABEL, parentSpaceName);
            namespace = new V1NamespaceBuilder()
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

        String getKubeconfig();
    }
}
