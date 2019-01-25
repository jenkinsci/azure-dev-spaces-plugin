package com.microsoft.jenkins.devspaces.commands;

import com.google.gson.JsonSyntaxException;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import hapi.services.tiller.Tiller.UninstallReleaseRequest;
import hapi.services.tiller.Tiller.UninstallReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1DeleteOptions;
import org.apache.commons.lang3.StringUtils;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CleanupDevSpaceCommand implements ICommand<CleanupDevSpaceCommand.ICleanupDevSpaceData> {
    private CoreV1Api api = new CoreV1Api();
    private static final String DEFAULT_TILLER_NAMESPACE = "azds";

    @Override
    public void execute(ICleanupDevSpaceData context) {
        String kubeconfig = context.getKubeconfig();
        String helmTillerNamespace = StringUtils.defaultIfBlank(context.getHelmTillerNamespace(), DEFAULT_TILLER_NAMESPACE);


        String helmReleaseName = context.getHelmReleaseName();
        if (StringUtils.isNotBlank(helmReleaseName)) {
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeconfig));
                 final Tiller tiller = new Tiller(client, helmTillerNamespace);
                 final ReleaseManager releaseManager = new ReleaseManager(tiller)) {
                UninstallReleaseRequest.Builder uninstallBuilder = UninstallReleaseRequest.newBuilder();
                uninstallBuilder.setName(helmReleaseName);
                int helmTimeout = context.getHelmTimeout();
                uninstallBuilder.setTimeout(helmTimeout);
                UninstallReleaseRequest uninstallRequest = uninstallBuilder.build();

                Future<UninstallReleaseResponse> uninstall = releaseManager.uninstall(uninstallRequest);
                uninstall.get();

                context.setCommandState(CommandState.Success);
            } catch (ExecutionException | InterruptedException | IOException e) {
                context.logError(e);
                context.setCommandState(CommandState.HasError);
            }

        }

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
        context.logStatus("Clean up dev space " + spaceName);
        context.setCommandState(CommandState.Success);
    }

    public interface ICleanupDevSpaceData extends IBaseCommandData {
        String getSpaceName();

        String getKubeconfig();

        String getHelmReleaseName();

        String getHelmTillerNamespace();

        int getHelmTimeout();
    }
}
