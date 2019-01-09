package com.microsoft.jenkins.devspaces.commands;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.devspaces.HelmContext;
import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.DockerConfigBuilder;
import hapi.chart.ChartOuterClass;
import hapi.release.ReleaseOuterClass;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.ListReleasesRequest;
import hapi.services.tiller.Tiller.ListReleasesResponse;
import hapi.services.tiller.Tiller.UpdateReleaseRequest;
import hudson.EnvVars;
import hudson.model.Item;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.grpc.StatusRuntimeException;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1SecretBuilder;
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
import java.util.Iterator;
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

            HelmContext helmContext = new HelmContext();
            helmContext.setNamespace(namespace);
            helmContext.setReleaseName(generateHelmReleaseName(namespace, helmChartLocation));
            helmContext.setChart(chart);
            helmContext.setTimeout(300);
            helmContext.setRawValue(yamlString);

            context.logStatus(helmChartLocation);

            createOrUpdateHelm(releaseManager, helmContext);

            context.setCommandState(CommandState.Success);
        } catch (IOException e) {
            context.logError(e);
            context.setCommandState(CommandState.HasError);
        }
    }

    private String generateHelmReleaseName(String namespace, String chartLocation) {
        String chartName = chartLocation.substring(chartLocation.lastIndexOf(System.getProperty("file.separator")) + 1);
        return String.format("%s-%s", namespace, chartName);
    }

    private void createOrUpdateHelm(ReleaseManager releaseManager, HelmContext helmContext) {
        if (isHelmReleaseExist(releaseManager, helmContext)) {
            updateHelmRelease(releaseManager, helmContext);
        } else {
            try {
                installHelmRelease(releaseManager, helmContext);
                try {
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateHelmRelease(releaseManager, helmContext);
            } catch (ExecutionException e) {
                String message = e.getMessage();
                if (message.contains("already exists")) {
                    updateHelmRelease(releaseManager, helmContext);
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isHelmReleaseExist(ReleaseManager releaseManager, HelmContext helmContext) {
        ListReleasesRequest.Builder builder = ListReleasesRequest.newBuilder();
        builder.setNamespace(helmContext.getNamespace());
        Iterator<ListReleasesResponse> responses = releaseManager.list(builder.build());
        while (responses.hasNext()) {
            ListReleasesResponse response = responses.next();
            List<ReleaseOuterClass.Release> releasesList = response.getReleasesList();
            for (ReleaseOuterClass.Release release : releasesList) {
                System.out.println("release name " + release.getName());
                if (helmContext.getReleaseName().equals(release.getName())) {
                    return true;
                }
            }
        }
//        boolean isExist = false;
//        try {
//            isExist = responses.hasNext();
//            System.out.println("has release " + helmContext.getReleaseName() + " " + isExist);
//        } catch (StatusRuntimeException e) {
//            System.out.println("==================");
//            System.out.println(e.getTrailers().toString());
//        }
//        return isExist;
        return false;
    }

    private void installHelmRelease(ReleaseManager releaseManager, HelmContext helmContext) throws ExecutionException {
        final InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
        requestBuilder.setNamespace(helmContext.getNamespace());
        requestBuilder.setTimeout(helmContext.getTimeout());
        requestBuilder.setName(helmContext.getReleaseName());
        requestBuilder.getValuesBuilder().setRaw(helmContext.getRawValue());
        requestBuilder.setWait(true); // Wait for Pods to be ready

        try {
            Future<hapi.services.tiller.Tiller.InstallReleaseResponse> install = releaseManager.install(requestBuilder, helmContext.getChart());
            hapi.services.tiller.Tiller.InstallReleaseResponse installReleaseResponse = install.get();
            ReleaseOuterClass.Release release = installReleaseResponse.getRelease();
            assert release != null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateHelmRelease(ReleaseManager releaseManager, HelmContext helmContext) {
        UpdateReleaseRequest.Builder builder = UpdateReleaseRequest.newBuilder();

        builder.setName(helmContext.getReleaseName());
        builder.setTimeout(helmContext.getTimeout());
        builder.getValuesBuilder().setRaw(helmContext.getRawValue());
        builder.setRecreate(true);
        builder.setForce(true);
        builder.setWait(true);

        try {
            Future<hapi.services.tiller.Tiller.UpdateReleaseResponse> update = releaseManager.update(builder, helmContext.getChart());
            hapi.services.tiller.Tiller.UpdateReleaseResponse updateReleaseResponse = update.get();
            ReleaseOuterClass.Release release = updateReleaseResponse.getRelease();
            assert release != null;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
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
        StringReader reader = new StringReader(kubeconfig);
        ApiClient client = io.kubernetes.client.util.Config.fromConfig(reader);
        Configuration.setDefaultApiClient(client);
        CoreV1Api coreV1Api = new CoreV1Api();
        V1Secret secret1 = null;
        try {
            secret1 = coreV1Api.readNamespacedSecret(secretName, kubernetesNamespace, "ture", false, false);
        } catch (ApiException e1) {
            int code = e1.getCode();
            if (code == 404) {
                //ignore
            }
            System.out.println(e1.toString());
        }
        try {
            if (secret1 == null) {
                coreV1Api.createNamespacedSecret(kubernetesNamespace, secret, "false");
            } else {
                coreV1Api.replaceNamespacedSecret(secretName, kubernetesNamespace, secret, "false");
            }
        } catch (ApiException e) {
            System.out.println(e.toString());
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
