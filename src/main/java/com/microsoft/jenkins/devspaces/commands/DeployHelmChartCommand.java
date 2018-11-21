package com.microsoft.jenkins.devspaces.commands;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import hapi.chart.ChartOuterClass;
import hapi.release.ReleaseOuterClass;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.kubernetes.client.ApiClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;
import org.microbean.helm.chart.DirectoryChartLoader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DeployHelmChartCommand implements ICommand<DeployHelmChartCommand.IDeployHelmChartData> {
    @Override
    public void execute(IDeployHelmChartData context) {
        String helmChartLocation = context.getHelmChartLocation();
        File file = new File(helmChartLocation);
//        File file = new File("D:\\coding\\samples\\dev-spaces\\samples\\java\\getting-started\\webfrontend\\charts\\webfrontend");
        if (!file.exists()) {
            context.logError(String.format("cannot find helm chart at %s", file.getAbsolutePath()));
            return;
        }
        URI uri = file.toURI();
        ChartOuterClass.Chart.Builder chart = null;
        try (final DirectoryChartLoader chartLoader = new DirectoryChartLoader()) {
            Path path = Paths.get(uri);
            chart = chartLoader.load(path);
        } catch (IOException e) {
            context.logError(e);
        }
        assert chart != null;
        String kubeConfig = context.getKubeconfig();
        try (final DefaultKubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeConfig));
             final Tiller tiller = new Tiller(client, "azds");
             final ReleaseManager releaseManager = new ReleaseManager(tiller)) {
            String namespace = context.getNamespace();
            String imageRepository = context.getImageRepository();
            String imageTag = context.getImageTag();
            final InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
            requestBuilder.setNamespace(namespace);
            requestBuilder.setTimeout(300L);
            requestBuilder.setName("test-charts-" + RandomStringUtils.randomAlphanumeric(8).toLowerCase()); // Set the Helm release name
            requestBuilder.setWait(true); // Wait for Pods to be ready

            final Map<String, Object> yaml = new LinkedHashMap<>();
            final Map<String, String> image = new LinkedHashMap<>();
            image.put("repository", imageRepository);
            image.put("tag", imageTag);
            yaml.put("image", image);

            final Map<String, List<String>> ingress = new LinkedHashMap<>();
            List<String> hosts = new ArrayList<>();
            hosts.add("localhost");
            ingress.put("hosts", hosts);
            yaml.put("ingress", ingress);
            final String yamlString = new Yaml().dump(yaml);
            requestBuilder.getValuesBuilder().setRaw(yamlString);

            final Future<InstallReleaseResponse> releaseFuture = releaseManager.install(requestBuilder, chart);
            assert releaseFuture != null;
            final ReleaseOuterClass.Release release = releaseFuture.get().getRelease();
            assert release != null;
            context.setCommandState(CommandState.Success);
        } catch (IOException | InterruptedException | ExecutionException e) {
            context.logError(e);
            context.setCommandState(CommandState.HasError);
        }
    }

    public interface IDeployHelmChartData extends IBaseCommandData {
        String getNamespace();

        String getImageRepository();

        String getImageTag();

        String getHelmChartLocation();

        String getKubeconfig();
    }
}
