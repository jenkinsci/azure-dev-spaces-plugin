/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.devspaces.commands;

import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.devspaces.cli.AzTask;
import com.microsoft.jenkins.devspaces.cli.AzdsTask;

public class AzdsCommand implements ICommand<AzdsCommand.IAzdsData> {

    @Override
    public void execute(IAzdsData context) {
        String repoPath = context.getRepoPath();
        String spaceName = context.getSpaceName();
        String aksName = context.getAksName();
        String resourceGroupName = context.getResourceGroupName();
        AzTask.login();

        AzTask.applyAzdsForAks(spaceName, resourceGroupName, aksName, repoPath);

        AzdsTask.prep(repoPath);

        AzdsTask.up(repoPath);

        context.setCommandState(CommandState.Success);
    }

    public interface IAzdsData extends IBaseCommandData {
        String getRepoPath();

        String getSpaceName();

        String getAksName();

        String getResourceGroupName();
    }
}
