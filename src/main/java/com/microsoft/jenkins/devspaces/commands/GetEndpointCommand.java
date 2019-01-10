package com.microsoft.jenkins.devspaces.commands;

import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.EnvironmentInjector;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1IngressList;
import io.kubernetes.client.models.V1beta1IngressRule;
import io.kubernetes.client.util.Config;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class GetEndpointCommand implements ICommand<GetEndpointCommand.IGetEndpointData> {
    public static final String DEFAULT_ENDPOINT_VARIABLE = "adsEndpoint";
    private static final String ENDPOINT_PREFIX_PATTERN = "%s.s";

    @Override
    public void execute(IGetEndpointData context) {

        try {
            StringReader reader = new StringReader(context.getKubeconfig());
            ApiClient client = Config.fromConfig(reader);
            Configuration.setDefaultApiClient(client);

            ExtensionsV1beta1Api extensionsV1beta1Api = new ExtensionsV1beta1Api();
            String namespace = context.getNamespace();
            V1beta1IngressList ingressList = extensionsV1beta1Api.listNamespacedIngress(namespace, "true", null, null, null, null, null, null, null, null);
            List<V1beta1Ingress> items = ingressList.getItems();
            String endpointVariable = DEFAULT_ENDPOINT_VARIABLE;
            for (V1beta1Ingress item : items) {
                List<V1beta1IngressRule> rules = item.getSpec().getRules();
                for (V1beta1IngressRule rule : rules) {
                    String host = rule.getHost();
                    if (host.startsWith(String.format(ENDPOINT_PREFIX_PATTERN, namespace))) {
                        EnvironmentInjector.inject(context.getJobContext().getRun(), context.getEnvVars(), endpointVariable, host);
                        context.logStatus(String.format("bind environment variable %s with %s", endpointVariable, host));
                    }
                }
            }
            context.setCommandState(CommandState.Success);
        } catch (IOException | ApiException e) {
            context.logError(e);
            context.setCommandState(CommandState.HasError);
        }
    }

    public interface IGetEndpointData extends IBaseCommandData {
        String getNamespace();

        String getKubeconfig();
    }
}
