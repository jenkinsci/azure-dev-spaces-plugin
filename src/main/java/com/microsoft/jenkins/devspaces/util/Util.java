/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.util;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.microsoft.jenkins.kubernetes.credentials.KubeconfigCredentials;
import hudson.model.Item;
import hudson.security.ACL;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

public class Util {
    public static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    public static String getConfigContent(Item owner, String kubeConfigId) {
        if (StringUtils.isNotBlank(kubeConfigId)) {
            final KubeconfigCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            KubeconfigCredentials.class,
                            owner,
                            ACL.SYSTEM,
                            Collections.emptyList()),
                    CredentialsMatchers.withId(kubeConfigId));
            if (credentials == null) {
                throw new IllegalArgumentException("Cannot find kubeconfig credentials with id " + kubeConfigId);
            }
            credentials.bindToAncestor(owner);
            return credentials.getContent();
        }
        throw new IllegalStateException();
    }
}
