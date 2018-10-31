/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.util;

import com.microsoft.azure.management.compute.ContainerServiceOrchestratorTypes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Constants {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String INVALID_OPTION = "*";
    public static final String EMPTY_SELECTION = "- none -";

    public static final String AKS = "AKS";
    public static final String AKS_PROVIDER = "Microsoft.ContainerService";
    public static final String AKS_RESOURCE_TYPE = "managedClusters";

    public static final Set<ContainerServiceOrchestratorTypes> SUPPORTED_ORCHESTRATOR = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ContainerServiceOrchestratorTypes.KUBERNETES,
            ContainerServiceOrchestratorTypes.DCOS,
            ContainerServiceOrchestratorTypes.SWARM
    )));
}
