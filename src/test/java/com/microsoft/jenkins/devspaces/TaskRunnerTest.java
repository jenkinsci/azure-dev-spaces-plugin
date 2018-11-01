package com.microsoft.jenkins.devspaces;

import com.microsoft.jenkins.devspaces.cli.TaskRunner;
import com.microsoft.jenkins.devspaces.util.Constants;
import hudson.model.TaskListener;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class TaskRunnerTest {
    @Test
    @Ignore
    public void testRun() throws IOException, InterruptedException {
        TaskRunner runner = new TaskRunner("test", "src/test/resources", null);
        runner.run("hello");
        Assert.assertEquals("Hello, gavin" + Constants.LINE_SEPARATOR, runner.getOutput());
    }

    @Test
    @Ignore
    public void testRunWithInput() throws IOException, InterruptedException {
        TaskRunner runner = new TaskRunner("test", "src/test/resources", null);
        runner.run("hello", new String[]{"gavin"});
        Assert.assertEquals("Hello, gavin" + Constants.LINE_SEPARATOR, runner.getOutput());
    }
}
