package com.applitools.eyes.visualgrid.services;

import com.applitools.ICheckSettings;
import com.applitools.ICheckSettingsInternal;
import com.applitools.eyes.AppOutput;
import com.applitools.eyes.MatchResult;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.RunningTest;
import com.applitools.eyes.visualgrid.model.RenderBrowserInfo;
import com.applitools.eyes.visualgrid.model.RenderStatusResults;
import com.applitools.eyes.visualgrid.model.VisualGridSelector;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CheckTask {
    private final String stepId = UUID.randomUUID().toString();
    private final RunningTest runningTest;
    private final ICheckSettings checkSettings;
    private final List<VisualGridSelector[]> regionSelectors;
    private final String source;

    private RenderStatusResults renderStatusResults;
    private AppOutput appOutput;

    public CheckTask(RunningTest runningTest, ICheckSettings checkSettings, List<VisualGridSelector[]> regionSelectors, String source) {
        this.runningTest = runningTest;
        this.checkSettings = checkSettings;
        this.regionSelectors = regionSelectors;
        this.source = source;
    }

    public String getStepId() {
        return stepId;
    }

    public ICheckSettings getCheckSettings() {
        return checkSettings;
    }

    public List<VisualGridSelector[]> getRegionSelectors() {
        return regionSelectors;
    }

    public String getSource() {
        return source;
    }

    public String getRenderer() {
        if (runningTest instanceof VisualGridRunningTest) {
            return ((VisualGridRunningTest) runningTest).getRenderer();
        }

        return null;
    }

    public RenderStatusResults getRenderStatusResults() {
        return renderStatusResults;
    }

    public void setAppOutput(RenderStatusResults renderResult) {
        this.renderStatusResults = renderResult;
        RectangleSize visualViewport = renderResult.getVisualViewport();
        appOutput = new AppOutput(((ICheckSettingsInternal) checkSettings).getName(),
                null,
                renderResult.getDomLocation(),
                renderResult.getImageLocation(),
                renderResult.getImagePositionInActiveFrame(),
                visualViewport);
    }

    public void setAppOutput(AppOutput appOutput) {
        this.appOutput = appOutput;
    }

    public AppOutput getAppOutput() {
        return appOutput;
    }

    public boolean isRenderFinished() {
        return renderStatusResults != null;
    }

    public boolean isReady() {
        return runningTest.isCheckTaskReady(this);
    }

    public String getTestId() {
        return runningTest.getTestId();
    }

    public RenderBrowserInfo getBrowserInfo() {
        return runningTest.getBrowserInfo();
    }

    public boolean isTestActive() {
        return !runningTest.isTestReadyToClose() && !runningTest.isTestAborted();
    }

    public void onComplete(MatchResult matchResult) {
        runningTest.checkCompleted(this, matchResult);
    }

    public void onFail(Throwable e) {
        runningTest.setTestInExceptionMode(e);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CheckTask that = (CheckTask) o;
        return stepId.equals(that.stepId) && runningTest.equals(that.runningTest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepId, runningTest);
    }
}
