package com.applitools.eyes.visualGridClient.services;

import com.applitools.ICheckSettings;
import com.applitools.eyes.Logger;
import com.applitools.eyes.MatchResult;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.visualGridClient.model.CompletableTask;
import com.applitools.eyes.visualGridClient.model.RenderBrowserInfo;
import com.applitools.eyes.visualGridClient.model.RenderStatusResults;
import com.applitools.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class Task implements Callable<TestResults>, CompletableTask {


    private static AtomicBoolean isThrown = new AtomicBoolean(false);
    private final Logger logger;
    private MatchResult matchResult;
    private boolean isSent;

    public enum TaskType {OPEN, CHECK, CLOSE, ABORT}

    private TestResults testResults;

    private IEyesConnector eyesConnector;
    private TaskType type;

    private RenderStatusResults renderResult;
    private List<TaskListener> listeners = new ArrayList<>();
    private ICheckSettings checkSettings;

    private RunningTest runningTest;

    private AtomicBoolean isTaskComplete = new AtomicBoolean(false);

    interface TaskListener {

        void onTaskComplete(Task task);

        void onTaskFailed(Exception e, Task task);

        void onRenderComplete();

    }

    public Task(TestResults testResults, IEyesConnector eyesConnector, TaskType type, TaskListener runningTestListener,
                ICheckSettings checkSettings, RunningTest runningTest) {
        this.testResults = testResults;
        this.eyesConnector = eyesConnector;
        this.type = type;
        this.listeners .add(runningTestListener);
        this.logger = runningTest.getLogger();
        this.checkSettings = checkSettings;
        this.runningTest = runningTest;
    }

    public RenderBrowserInfo getBrowserInfo() {
        return runningTest.getBrowserInfo();
    }

    public TaskType getType() {
        return type;
    }

    boolean isSent() {
        return isSent;
    }

    void setIsSent() {
        this.isSent = true;
    }

    @Override
    public TestResults call() {
        try {
            testResults = null;
            switch (type) {
                case OPEN:
                    logger.log("Task.run opening task");
                    String userAgent = renderResult.getUserAgent();
                    eyesConnector.setUserAgent(userAgent);
                    eyesConnector.open(runningTest.getConfiguration());
                    break;

                case CHECK:
                    logger.log("Task.run check task");
                    matchResult = eyesConnector.matchWindow(renderResult.getImageLocation(), checkSettings);
                    break;

                case CLOSE:
                    logger.log("Task.run close task");
                    testResults = eyesConnector.close(runningTest.getConfiguration().isThrowExceptionOn());
                    break;

                case ABORT:
                    logger.log("Task.run abort task");
                    eyesConnector.abortIfNotClosed();
            }
            return testResults;
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
            notifyFailureAllListeners(e);
        } finally {
            this.isTaskComplete.set(true);
            //call the callback
            notifySuccessAllListeners();
        }
        return null;
    }

    private void notifySuccessAllListeners() {
        for (TaskListener listener : listeners) {
            listener.onTaskComplete(this);
        }
    }

    private void notifyFailureAllListeners(Exception e) {
        for (TaskListener listener : listeners) {
            listener.onTaskFailed(e, this);
        }
    }
    private void notifyRenderCompleteAllListeners(){
        for (TaskListener listener : listeners) {
            listener.onRenderComplete();
        }
    }

    public IEyesConnector getEyesConnector() {
        return eyesConnector;
    }

    private static boolean isThrown() {
        return Task.isThrown.get();
    }

    public void setRenderResult(RenderStatusResults renderResult) {
        logger.verbose("enter");
        this.renderResult = renderResult;
        notifyRenderCompleteAllListeners();
        logger.verbose("exit");
    }

    public boolean isTaskReadyToCheck() {
        return this.renderResult != null;
    }

    public RunningTest getRunningTest() {
        return runningTest;
    }

    public boolean getIsTaskComplete() {
        return isTaskComplete.get();
    }

    public void addListener(TaskListener listener){
        this.listeners.add(listener);
    }
}

