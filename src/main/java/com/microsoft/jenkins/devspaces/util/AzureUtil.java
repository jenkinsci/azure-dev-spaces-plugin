/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.util;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.azure.util.AzureCredentialUtil;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import com.microsoft.jenkins.azurecommons.core.credentials.TokenCredentialData;
import hudson.model.Item;
import hudson.security.ACL;

import java.util.Collections;

public class AzureUtil {
    public static Azure getAzureClient(Item item, String azureCredentialsId) {
        AzureBaseCredentials credential = AzureCredentialUtil.getCredential(item, azureCredentialsId);
        // Resolve the class loader incompatibility issue. Works along with maskClasses in the POM
        TokenCredentialData token = TokenCredentialData.deserialize(credential.serializeToTokenData());
        return AzureClientFactory.getClient(token);
    }

    public static StandardUsernamePasswordCredentials getUserPass(Item item, String credentialsId) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        item,
                        ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialsId));
    }
}
