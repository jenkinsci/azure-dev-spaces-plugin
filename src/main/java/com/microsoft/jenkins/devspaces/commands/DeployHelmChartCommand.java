package com.microsoft.jenkins.devspaces.commands;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.DockerConfigBuilder;
import hapi.chart.ChartOuterClass;
import hapi.release.ReleaseOuterClass;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import hudson.EnvVars;
import hudson.model.Item;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1SecretBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;
import org.microbean.helm.chart.DirectoryChartLoader;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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

        String secretNamespace = context.getSecretNamespace();
        String secretNameCfg = context.getSecretName();
        String defaultSecretNameSeed = context.getJobContext().getRun().getDisplayName();
        EnvVars envVars = context.getEnvVars();
        String secretName = null;
        try {
            List<ResolvedDockerRegistryEndpoint> dockerRegistryEndpoints = context.resolveEndpoints(context.getJobContext().getOwner());
            if (!dockerRegistryEndpoints.isEmpty()) {
                secretName =
                        KubernetesClientWrapper.prepareSecretName(secretNameCfg, defaultSecretNameSeed, envVars);

                createOrReplaceSecrets(secretNamespace, secretName, dockerRegistryEndpoints, context.getKubeconfig());

//                envVars.put(Constants.KUBERNETES_SECRET_NAME_PROP, secretName);
//                result.extraEnvVars.put(Constants.KUBERNETES_SECRET_NAME_PROP, secretName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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

            if (secretName != null) {
                final Map<String, String> imagePullSecret = new LinkedHashMap<>();
                imagePullSecret.put("name", secretName);
                final List<Map<String, String>> imagePullSecrets = new ArrayList<>();
                imagePullSecrets.add(imagePullSecret);
                yaml.put("imagePullSecrets", imagePullSecrets);
            }

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

    public void createOrReplaceSecrets(
            String kubernetesNamespace,
            String secretName,
            List<ResolvedDockerRegistryEndpoint> credentials,
            String kubeconfig) throws IOException {

        DockerConfigBuilder dockerConfigBuilder = new DockerConfigBuilder(credentials);
        String dockercfg = dockerConfigBuilder.buildDockercfgString();
//        String dockercfg = dockerConfigBuilder.buildDockercfgBase64();

        Map<String, byte[]> data = new HashMap<>();
        data.put(".dockercfg", dockercfg.getBytes(StandardCharsets.UTF_8));
//        data.put(".dockercfg", dockercfg.getBytes(StandardCharsets.UTF_8));
//        V1Secret secret = new V1SecretBuilder()
//                .withNewMetadata()
//                .withName(secretName)
        V1Secret secret = new V1SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(kubernetesNamespace)
                .endMetadata()
                .withData(data)
                .withType("kubernetes.io/dockercfg")
                .build();
        // TODO createOrUpdate?
        StringReader reader = new StringReader(kubeconfig);
        ApiClient client = io.kubernetes.client.util.Config.fromConfig(reader);
        client.setDebugging(true);
        Configuration.setDefaultApiClient(client);
        CoreV1Api coreV1Api = new CoreV1Api();
        try {
            coreV1Api.createNamespacedSecret(kubernetesNamespace, secret, "true");
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    public interface IDeployHelmChartData extends IBaseCommandData {
        String getNamespace();

        String getImageRepository();

        String getImageTag();

        String getHelmChartLocation();

        String getKubeconfig();

        String getSecretNamespace();

        String getSecretName();

        List<ResolvedDockerRegistryEndpoint> resolveEndpoints(Item context) throws IOException;
    }
}
