/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.util;

public class Util {
    public static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }
}
