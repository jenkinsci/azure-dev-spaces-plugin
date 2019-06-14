# Azure Dev Spaces Plugin

Azure Dev Spaces Plugin helps to manage [Azure Dev Spaces](https://docs.microsoft.com/en-us/azure/dev-spaces/) in a Jenkins pipeline.

## Installation 

You can install/update this plugin in Jenkins Plugin Manager. 
1. Sign into Jenkins
1. Choose **Manage Jenkins > Manage Plugins > Available tab**
1. Filter for the `Azure Dev Spaces Plugin`
1. Choose **Download now and install after restart**
1. Restart Jenkins

If you don't see the plugin in the list, check to see if it is already installed.

You can also can manually build and install the plugin. Manual builds are helpful if you want to try out the latest features before they are released.

To manually install the plugin:

1. Clone the repo and build

    ```bash
    mvn package
    ```

2. Sign into Jenkins, then go to **Manage Jenkins > Manage Plugins**.

3. On the **Advanced** tab, under **Upload Plugin** section, select **Choose File**.

4. Select `azure-dev-spaces.hpi` in `target` folder of your repo, and then choose **Upload**.

5. Restart Jenkins after installation is completed.

## Prerequisites for use

Azure Dev Spaces plugin requires an Azure service principal to access Azure resources. To create the service principal and add a new credential to Jenkins, refer to the [Create service principal](https://docs.microsoft.com/en-us/azure/jenkins/tutorial-jenkins-deploy-web-app-azure-app-service#create-service-principal) section in the Deploy to Azure App Service tutorial. 


## Create a dev space

### Freestyle job

1. Create a new freestyle job, or open an existing job.

2. On the **Build** tab, in the **Build** section, choose **Add build step**. Fron the list, choose **Create dev spaces**.

3. Select your Azure credential, resource group, and AKS cluster from the lists. 

4. Set value for your parent dev space name and dev space name.

5. In the **Kubeconfig** list, select the kubeconfig stored in Jenkins. Select the **Add** button to add new kubeconfig. Select **Kubernetes configuration (kubeconfig)** from the **Kind** list.

To get the AKS credentials, use `az aks get-credentials -g <resourcegroup> - <aksclustername> -f -`. The output will look similar to this (truncated for  brevity, sensitive info redacted):

```bash
apiVersion: v1
clusters:
- cluster:
    certificate-authority-data: LS0tLS1C...JRklDQVRFLS0tLS0K
    server: https://jdsaks-jenkinsdevspace-xxxxxxx-xxxxxxxxx.hcp.westus2.azmk8s.io:443
  name: jdsAKS
contexts:
- context:
    cluster: jdsAKS
    user: clusterUser_jenkinsdevspace_jdsAKS
  name: jdsAKS
current-context: jdsAKS
kind: Config
preferences: {}
users:
- name: clusterUser_jenkinsdevspace_jdsAKS
  user:
    client-certificate-data: LS0tLS1CRU...FURS0tLS0tCg==
    client-key-data: LS0tLS1CR...LS0tLS0K
    token: 9c8971bfxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Copy the entire output and then paste it into the **Content** box.


### Pipeline

The pipeline command to create a dev space is:

```Groovy
devSpacesCreate aksName: '', azureCredentialsId: '', resourceGroupName: '', sharedSpaceName: '', spaceName: ''
```
Example:
```Groovy
stage('create dev space') {
    devSpacesCreate 
        aksName: <aks cluster name>, 
        azureCredentialsId: <ID of service principal credential>, 
        kubeconfigId: <ID of kubeconfig credential>, 
        resourceGroupName: <aks resource group>, 
        sharedSpaceName: <parent dev space name>, 
        spaceName: <aks namespace>
}
```

## Clean up a dev space

### Freestyle job

1. In the job configuration screen, scroll down to **Post-build Actions**. Add a post-build action **Cleanup dev spaces**.

2. Select an Azure credential, resource group, and AKS cluster. 

3. In **Dev Space Name **, enter the name of the dev space to clean uup.

4. Select or add a **Kubeconfig**. See step 5, above, for details.

1. Save the project and then build it.

### Pipeline

The pipeline command to clean up (delete) a dev space is:

```Groovy
devSpacesCleanup aksName: '', azureCredentialsId: '', devSpaceName: '', resourceGroupName: ''
```

Example:
```Groovy
stage('cleanup') {
    devSpacesCleanup 
        aksName: <aks cluster name>, 
        azureCredentialsId: <ID of service principal credential>, 
        devSpaceName: <name of dev space to delete>, 
        kubeConfigId: <ID of kubeconfig credential>, 
        resourceGroupName: <aks resource group>,
        helmReleaseName: releaseName 
}
```

# Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
