/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.util;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.azure.util.AzureCredentialUtil;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import com.microsoft.jenkins.azurecommons.core.credentials.TokenCredentialData;
import hudson.model.Item;

public class AzureUtil {
    public static Azure getAzureClient(Item item, String azureCredentialsId) {
        AzureBaseCredentials credential = AzureCredentialUtil.getCredential(item, azureCredentialsId);
        // Resolve the class loader incompatibility issue. Works along with maskClasses in the POM
        TokenCredentialData token = TokenCredentialData.deserialize(credential.serializeToTokenData());
        return AzureClientFactory.getClient(token);
    }
}
