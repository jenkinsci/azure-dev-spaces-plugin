# Azure Dev Spaces Plugin

Azure Dev Spaces Plugin helps to manage dev spaces in [Azure Dev Spaces](https://docs.microsoft.com/en-us/azure/dev-spaces/).

## How to Install 

You can install/update this plugin in Jenkins update center (Manage Jenkins -> Manage Plugins, search Azure Dev Spaces Plugin).

You can also manually install the plugin if you want to try the latest feature before it's officially released.
To manually install the plugin:

1. Clone the repo and build:
   ```
   mvn package
   ```
   
1. Open your Jenkins dashboard, go to Manage Jenkins -> Manage Plugins.

1. Go to Advanced tab, under Upload Plugin section, click Choose File.

1. Select `azure-dev-spaces.hpi` in `target` folder of your repo, click Upload.

1. Restart your Jenkins instance after install is completed.

## Prerequisites

To use this plugin to manage dev spaces, first you need to have an Azure Service Principal in your Jenkins instance.

1. Create an Azure Service Principal through [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?toc=%2fazure%2fazure-resource-manager%2ftoc.json) or [Azure portal](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal).

1. Open Jenkins dashboard, go to Credentials, add a new Microsoft Azure Service Principal with the credential information you just created.

## Create a dev space

### Freestyle job

1. Choose to add a `Build` action 'Create dev spaces'.

1. Select your Azure credential in Azure credential section.

1. Select the resource group and Azure kubernetes service in your subscription.

1. Set value for your parent dev space name and dev space name.

1. Select kubernetes configuration, this is 

1. In the "Kubeconfig" dropdown, select the kubeconfig stored in Jenkins. You can click the "Add" button on the right to add new kubeconfig (Kind: Kubernetes configuration (kubeconfig)). You can enter the kubeconfig content directly in it.

### Pipeline

Pipeline step command is like below, follow freestyle job to fill variables.

```
devSpacesCreate aksName: '', azureCredentialsId: '', resourceGroupName: '', sharedSpaceName: '', spaceName: '', kubeconfigId: ''
```

## Clean up a dev space

### Freestyle job

1. Choose to add a `Post-build Actions` action 'Cleanup dev spaces'.

1. Select your Azure credential in Azure credential section.

1. Select the resource group and Azure kubernetes service in your subscription.

1. Set value for dev space name needed to be cleaned up.

1. In the "Kubeconfig" dropdown, select the kubeconfig stored in Jenkins. You can click the "Add" button on the right to add new kubeconfig (Kind: Kubernetes configuration (kubeconfig)). You can enter the kubeconfig content directly in it.

1. Save the project and build it.

### Pipeline

Pipeline step command is like below, follow freestyle job to fill variables.

```
devSpacesCleanup aksName: '', azureCredentialsId: '', devSpaceName: '', resourceGroupName: '', kubeConfigId: '', helmReleaseName: '',
```

> The helmReleaseName parameter is optional. You should provide it if you use helm to deploy to AKS and it will clean up the helm release too.

# Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
