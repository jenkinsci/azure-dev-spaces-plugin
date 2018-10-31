/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.exceptions;

public class AzureCliException extends Exception {
    public AzureCliException(Throwable throwable) {
        super("Execute Azure cli exception", throwable);
    }
}
