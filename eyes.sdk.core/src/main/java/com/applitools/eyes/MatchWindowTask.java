package com.applitools.eyes;

import com.applitools.connectivity.ServerConnector;
import com.applitools.eyes.capture.AppOutputProvider;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.fluent.*;
import com.applitools.eyes.logging.Stage;
import com.applitools.eyes.logging.TraceLevel;
import com.applitools.eyes.visualgrid.model.IGetFloatingRegionOffsets;
import com.applitools.eyes.visualgrid.model.MutableRegion;
import com.applitools.eyes.visualgrid.model.VisualGridSelector;
import com.applitools.eyes.visualgrid.services.CheckTask;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MatchWindowTask {

    private static final int MATCH_INTERVAL = 500; // Milliseconds
    private EyesScreenshot lastScreenshot = null;
    private String lastScreenshotHash;
    private final int defaultRetryTimeout;

    protected Logger logger;
    protected ServerConnector serverConnector;
    protected AppOutputProvider appOutputProvider;
    protected MatchResult matchResult;
    protected EyesBase eyes;

    /**
     * @param logger            A logger instance.
     * @param serverConnector   Our gateway to the agent
     * @param retryTimeout      The default total time to retry matching (ms).
     * @param eyes              An EyesBase object.
     * @param appOutputProvider A callback for getting the application output when performing match.
     */
    public MatchWindowTask(Logger logger, ServerConnector serverConnector, int retryTimeout,
                           EyesBase eyes, AppOutputProvider appOutputProvider) {
        ArgumentGuard.notNull(serverConnector, "serverConnector");
        ArgumentGuard.greaterThanOrEqualToZero(retryTimeout, "retryTimeout");
        ArgumentGuard.notNull(appOutputProvider, "appOutputProvider");

        this.logger = logger;
        this.serverConnector = serverConnector;
        this.defaultRetryTimeout = retryTimeout;
        this.eyes = eyes;
        this.appOutputProvider = appOutputProvider;
    }

    public static void collectRegions(EyesBase eyes, EyesScreenshot screenshot,
                                      ICheckSettingsInternal checkSettingsInternal, ImageMatchSettings imageMatchSettings) {
        collectSimpleRegions(eyes, checkSettingsInternal, imageMatchSettings, screenshot);
        collectFloatingRegions(checkSettingsInternal, imageMatchSettings, screenshot);
        collectAccessibilityRegions(checkSettingsInternal, imageMatchSettings, screenshot);
    }

    public static void collectRegions(ImageMatchSettings imageMatchSettings, ICheckSettingsInternal checkSettingsInternal) {
        imageMatchSettings.setIgnoreRegions(convertSimpleRegions(checkSettingsInternal.getIgnoreRegions(), imageMatchSettings.getIgnoreRegions()));
        imageMatchSettings.setContentRegions(convertSimpleRegions(checkSettingsInternal.getContentRegions(), imageMatchSettings.getContentRegions()));
        imageMatchSettings.setLayoutRegions(convertSimpleRegions(checkSettingsInternal.getLayoutRegions(), imageMatchSettings.getLayoutRegions()));
        imageMatchSettings.setStrictRegions(convertSimpleRegions(checkSettingsInternal.getStrictRegions(), imageMatchSettings.getStrictRegions()));
        imageMatchSettings.setFloatingRegions(convertFloatingRegions(checkSettingsInternal.getFloatingRegions(), imageMatchSettings.getFloatingRegions()));
        imageMatchSettings.setAccessibility(convertAccessibilityRegions(checkSettingsInternal.getAccessibilityRegions(), imageMatchSettings.getAccessibility()));
    }

    private static AccessibilityRegionByRectangle[] convertAccessibilityRegions(GetAccessibilityRegion[] accessibilityRegions, AccessibilityRegionByRectangle[] currentRegions) {
        List<AccessibilityRegionByRectangle> mutableRegions = new ArrayList<>();
        if (currentRegions != null) {
            mutableRegions.addAll(Arrays.asList(currentRegions));
        }

        for (GetAccessibilityRegion getRegions : accessibilityRegions) {
            if (getRegions instanceof AccessibilityRegionByRectangle) {
                mutableRegions.addAll(getRegions.getRegions(null));
            }
        }

        return mutableRegions.toArray(new AccessibilityRegionByRectangle[0]);
    }

    private static Region[] convertSimpleRegions(GetSimpleRegion[] simpleRegions, Region[] currentRegions) {
        List<Region> mutableRegions = new ArrayList<>();
        if (currentRegions != null) {
            Collections.addAll(mutableRegions, currentRegions);
        }

        for (GetSimpleRegion simpleRegion : simpleRegions) {
            if (simpleRegion instanceof SimpleRegionByRectangle) {
                mutableRegions.addAll(simpleRegion.getRegions(null));
            }
        }

        return mutableRegions.toArray(new Region[0]);
    }

    private static FloatingMatchSettings[] convertFloatingRegions(GetFloatingRegion[] floatingRegions, FloatingMatchSettings[] currentRegions) {
        List<FloatingMatchSettings> mutableRegions = new ArrayList<>();
        if (currentRegions != null) {
            Collections.addAll(mutableRegions, currentRegions);
        }

        for (GetFloatingRegion getRegions : floatingRegions) {
            if (getRegions instanceof FloatingRegionByRectangle) {
                mutableRegions.addAll(getRegions.getRegions(null));
            }
        }

        return mutableRegions.toArray(new FloatingMatchSettings[0]);
    }

    public static void collectRegions(ImageMatchSettings imageMatchSettings, Location location, List<? extends IRegion> regions, List<VisualGridSelector[]> regionSelectors) {
        if (regions == null) return;

        int currentCounter = 0;
        int currentTypeIndex = 0;
        int currentTypeRegionCount = regionSelectors.get(0).length;

        List<List<MutableRegion>> mutableRegions = new ArrayList<>();
        mutableRegions.add(new ArrayList<MutableRegion>()); // Ignore Regions
        mutableRegions.add(new ArrayList<MutableRegion>()); // Layout Regions
        mutableRegions.add(new ArrayList<MutableRegion>()); // Strict Regions
        mutableRegions.add(new ArrayList<MutableRegion>()); // Content Regions
        mutableRegions.add(new ArrayList<MutableRegion>()); // Floating Regions
        mutableRegions.add(new ArrayList<MutableRegion>()); // Accessibility Regions

        for (IRegion region : regions) {
            boolean canAddRegion = false;
            while (!canAddRegion) {
                currentCounter++;
                if (currentCounter > currentTypeRegionCount) {
                    currentTypeIndex++;
                    currentTypeRegionCount = regionSelectors.get(currentTypeIndex).length;
                    currentCounter = 0;
                } else {
                    canAddRegion = true;
                }
            }
            MutableRegion mr = new MutableRegion(region);
            mutableRegions.get(currentTypeIndex).add(mr);
        }

        imageMatchSettings.setIgnoreRegions(filterEmptyEntries(mutableRegions.get(0), location));
        imageMatchSettings.setLayoutRegions(filterEmptyEntries(mutableRegions.get(1), location));
        imageMatchSettings.setStrictRegions(filterEmptyEntries(mutableRegions.get(2), location));
        imageMatchSettings.setContentRegions(filterEmptyEntries(mutableRegions.get(3), location));

        List<FloatingMatchSettings> floatingMatchSettings = new ArrayList<>();
        for (int i = 0; i < regionSelectors.get(4).length; i++) {
            MutableRegion mr = mutableRegions.get(4).get(i);
            if (mr.getArea() == 0) continue;
            VisualGridSelector vgs = regionSelectors.get(4)[i];

            if (vgs.getCategory() instanceof IGetFloatingRegionOffsets) {
                IGetFloatingRegionOffsets gfr = (IGetFloatingRegionOffsets) vgs.getCategory();
                FloatingMatchSettings fms = new FloatingMatchSettings(
                        mr.getLeft(),
                        mr.getTop(),
                        mr.getWidth(),
                        mr.getHeight(),
                        gfr.getMaxUpOffset(),
                        gfr.getMaxDownOffset(),
                        gfr.getMaxLeftOffset(),
                        gfr.getMaxRightOffset()
                );
                floatingMatchSettings.add(fms);
            }
        }
        imageMatchSettings.setFloatingRegions(floatingMatchSettings.toArray(new FloatingMatchSettings[0]));

        List<AccessibilityRegionByRectangle> accessibilityRegions = new ArrayList<>();
        VisualGridSelector[] visualGridSelectors = regionSelectors.get(5);
        for (int i = 0; i < visualGridSelectors.length; i++) {
            MutableRegion mr = mutableRegions.get(5).get(i);
            if (mr.getArea() == 0) continue;
            VisualGridSelector vgs = visualGridSelectors[i];

            if (vgs.getCategory() instanceof IGetAccessibilityRegionType) {
                IGetAccessibilityRegionType gar = (IGetAccessibilityRegionType) vgs.getCategory();
                AccessibilityRegionByRectangle accessibilityRegion = new AccessibilityRegionByRectangle(
                        mr.getLeft() - location.getX(),
                        mr.getTop() - location.getY(),
                        mr.getWidth(),
                        mr.getHeight(),
                        gar.getAccessibilityRegionType());
                accessibilityRegions.add(accessibilityRegion);
            }
        }
        imageMatchSettings.setAccessibility(accessibilityRegions.toArray(new AccessibilityRegionByRectangle[0]));
    }

    private static MutableRegion[] filterEmptyEntries(List<MutableRegion> list, Location location) {
        for (int i = list.size() - 1; i >= 0; i--) {
            MutableRegion mutableRegion = list.get(i);
            if (mutableRegion.getArea() == 0) {
                list.remove(i);
            } else {
                mutableRegion.offset(-location.getX(), -location.getY());
            }
        }
        return list.toArray(new MutableRegion[0]);
    }

    private static void collectFloatingRegions(ICheckSettingsInternal checkSettingsInternal,
                                               ImageMatchSettings imageMatchSettings,
                                               EyesScreenshot screenshot) {
        List<FloatingMatchSettings> floatingRegions = new ArrayList<>();
        for (GetFloatingRegion floatingRegion : checkSettingsInternal.getFloatingRegions()) {
            List<FloatingMatchSettings> regions = floatingRegion.getRegions(screenshot);
            floatingRegions.addAll(regions);
        }
        imageMatchSettings.setFloatingRegions(floatingRegions.toArray(new FloatingMatchSettings[0]));

    }

    /**
     * Repeatedly obtains an application snapshot and matches it with the next
     * expected output, until a match is found or the timeout expires.
     * @param userInputs             User input preceding this match.
     * @param region                 Window region to capture.
     * @param tag                    Optional tag to be associated with the match (can be {@code null}).
     * @param shouldRunOnceOnTimeout Force a single match attempt at the end of the match timeout.
     * @param checkSettingsInternal  The settings to use.
     * @return Returns the results of the match
     */
    public MatchResult matchWindow(Trigger[] userInputs, Region region, String tag, boolean shouldRunOnceOnTimeout,
                                   ICheckSettingsInternal checkSettingsInternal, String source) {
        ImageMatchSettings imageMatchSettings = createImageMatchSettings(checkSettingsInternal, this.eyes);
        int retryTimeout = checkSettingsInternal.getTimeout();
        if (retryTimeout < 0) {
            retryTimeout = defaultRetryTimeout;
        }

        EyesScreenshot screenshot = takeScreenshot(userInputs, region, tag, shouldRunOnceOnTimeout,
                checkSettingsInternal, imageMatchSettings, retryTimeout, source);

        updateLastScreenshot(screenshot);
        return matchResult;
    }

    private static void collectSimpleRegions(EyesBase eyes,
                                             ICheckSettingsInternal checkSettingsInternal,
                                             ImageMatchSettings imageMatchSettings,
                                             EyesScreenshot screenshot) {
        imageMatchSettings.setIgnoreRegions(collectSimpleRegions(eyes, checkSettingsInternal.getIgnoreRegions(), screenshot));
        imageMatchSettings.setLayoutRegions(collectSimpleRegions(eyes, checkSettingsInternal.getLayoutRegions(), screenshot));
        imageMatchSettings.setStrictRegions(collectSimpleRegions(eyes, checkSettingsInternal.getStrictRegions(), screenshot));
        imageMatchSettings.setContentRegions(collectSimpleRegions(eyes, checkSettingsInternal.getContentRegions(), screenshot));
    }

    private static Region[] collectSimpleRegions(EyesBase eyes, GetSimpleRegion[] regionProviders, EyesScreenshot screenshot) {

        List<Region> regions = new ArrayList<>();
        for (GetSimpleRegion regionProvider : regionProviders) {
            try {
                regions.addAll(regionProvider.getRegions(screenshot));
            } catch (OutOfBoundsException ex) {
                GeneralUtils.logExceptionStackTrace(eyes.getLogger(), Stage.CHECK, ex, eyes.getTestId());
            }
        }
        return regions.toArray(new Region[0]);
    }

    /**
     * Build match settings by merging the check settings and the default match settings.
     * @param checkSettingsInternal the settings to match the image by.
     * @param screenshot            the Screenshot wrapper object.
     * @return Merged match settings.
     */
    public static ImageMatchSettings createImageMatchSettings(ICheckSettingsInternal checkSettingsInternal, EyesScreenshot screenshot, EyesBase eyesBase) {
        eyesBase.getLogger().log(TraceLevel.Info, eyesBase.getTestId(), Stage.CHECK,
                Pair.of("configuration", eyesBase.getConfiguration()),
                Pair.of("checkSettings", checkSettingsInternal));
        ImageMatchSettings imageMatchSettings = createImageMatchSettings(checkSettingsInternal, eyesBase);
        if (imageMatchSettings != null) {
            collectSimpleRegions(eyesBase, checkSettingsInternal, imageMatchSettings, screenshot);
            collectFloatingRegions(checkSettingsInternal, imageMatchSettings, screenshot);
            collectAccessibilityRegions(checkSettingsInternal, imageMatchSettings, screenshot);
        }

        return imageMatchSettings;
    }

    /**
     * Build match settings by merging the check settings and the default match settings.
     * @param checkSettingsInternal the settings to match the image by.
     * @return Merged match settings.
     */
    public static ImageMatchSettings createImageMatchSettings(ICheckSettingsInternal checkSettingsInternal, EyesBase eyes) {
        ImageMatchSettings imageMatchSettings = null;
        if (checkSettingsInternal != null) {

            Configuration config = eyes.getConfigurationInstance();
            ImageMatchSettings defaultMatchSettings = config.getDefaultMatchSettings();

            imageMatchSettings = new ImageMatchSettings(defaultMatchSettings); // clone default match settings
            imageMatchSettings.setMatchLevel(checkSettingsInternal.getMatchLevel() != null ? checkSettingsInternal.getMatchLevel() : defaultMatchSettings.getMatchLevel());
            imageMatchSettings.setIgnoreCaret(checkSettingsInternal.getIgnoreCaret() != null ? checkSettingsInternal.getIgnoreCaret() : config.getIgnoreCaret());
            imageMatchSettings.setUseDom(checkSettingsInternal.isUseDom() != null ? checkSettingsInternal.isUseDom() : config.getUseDom());
            imageMatchSettings.setEnablePatterns(checkSettingsInternal.isEnablePatterns() != null ? checkSettingsInternal.isEnablePatterns() : config.getEnablePatterns());
            imageMatchSettings.setIgnoreDisplacements(checkSettingsInternal.isIgnoreDisplacements() != null ? checkSettingsInternal.isIgnoreDisplacements() : config.getIgnoreDisplacements());
            imageMatchSettings.setAccessibilitySettings(config.getAccessibilityValidation());
        }
        return imageMatchSettings;
    }

    private EyesScreenshot takeScreenshot(Trigger[] userInputs, Region region, String tag,
                                          boolean shouldMatchWindowRunOnceOnTimeout,
                                          ICheckSettingsInternal checkSettingsInternal,
                                          ImageMatchSettings imageMatchSettings,
                                          int retryTimeout, String source) {
        EyesScreenshot screenshot;
        lastScreenshotHash = null;

        // If the wait to load time is 0, or "run once" is true,
        // we perform a single check window.
        if (0 == retryTimeout || shouldMatchWindowRunOnceOnTimeout || eyes.isAsync) {
            if (shouldMatchWindowRunOnceOnTimeout) {
                GeneralUtils.sleep(retryTimeout);
            }
            screenshot = tryTakeScreenshot(userInputs, region, tag, checkSettingsInternal, imageMatchSettings, source);
        } else {
            screenshot = retryTakingScreenshot(userInputs, region, tag, checkSettingsInternal, imageMatchSettings,
                    retryTimeout, source);
        }
        return screenshot;
    }

    private EyesScreenshot retryTakingScreenshot(Trigger[] userInputs, Region region, String tag,
                                                 ICheckSettingsInternal checkSettingsInternal,
                                                 ImageMatchSettings imageMatchSettings, int retryTimeout, String source) {
        // Start the retry timer.
        long start = System.currentTimeMillis();

        EyesScreenshot screenshot = null;

        long retry = System.currentTimeMillis() - start;

        // The match retry loop.
        while (retry < retryTimeout) {

            // Wait before trying again.
            GeneralUtils.sleep(MATCH_INTERVAL);

            screenshot = tryTakeScreenshot(userInputs, region, tag, checkSettingsInternal, imageMatchSettings, source);

            if (matchResult.getAsExpected()) {
                break;
            }

            retry = System.currentTimeMillis() - start;
        }

        // if we're here because we haven't found a match yet, try once more
        if (!matchResult.getAsExpected()) {
            screenshot = tryTakeScreenshot(userInputs, region, tag, checkSettingsInternal, imageMatchSettings, source);
        }
        return screenshot;
    }

    private EyesScreenshot tryTakeScreenshot(Trigger[] userInputs, Region region, String tag,
                                             ICheckSettingsInternal checkSettingsInternal,
                                             ImageMatchSettings imageMatchSettings, String source) {
        AppOutput appOutput = appOutputProvider.getAppOutput(region, checkSettingsInternal, imageMatchSettings);
        EyesScreenshot screenshot = appOutput.getScreenshot();
        String currentScreenshotHash = GeneralUtils.getSha256hash(appOutput.getScreenshotBytes());
        if (currentScreenshotHash.equals(lastScreenshotHash)) {
            return screenshot;
        }

        if (eyes.isAsync) {
            RunningTest runningTest = (RunningTest) eyes;
            CheckTask checkTask = runningTest.issueCheck((CheckSettings) checkSettingsInternal, null, source);
            checkTask.setAppOutput(appOutput);
            eyes.performMatchAsync(checkTask);
            return null;
        }

        ImageMatchSettings matchSettings = createImageMatchSettings(checkSettingsInternal, screenshot, eyes);
        MatchWindowData data = eyes.prepareForMatch(checkSettingsInternal, Arrays.asList(userInputs), appOutput, tag, lastScreenshotHash != null,
                matchSettings, null, source);
        matchResult = eyes.performMatch(data);
        lastScreenshotHash = currentScreenshotHash;
        return screenshot;
    }

    private void updateLastScreenshot(EyesScreenshot screenshot) {
        if (screenshot != null) {
            lastScreenshot = screenshot;
        }
    }

    public EyesScreenshot getLastScreenshot() {
        return lastScreenshot;
    }

    private static void collectAccessibilityRegions(ICheckSettingsInternal checkSettingsInternal,
                                                    ImageMatchSettings imageMatchSettings,
                                                    EyesScreenshot screenshot) {
        List<AccessibilityRegionByRectangle> accessibilityRegions = new ArrayList<>();
        for (GetAccessibilityRegion regionProvider : checkSettingsInternal.getAccessibilityRegions()) {
            accessibilityRegions.addAll(regionProvider.getRegions(screenshot));
        }
        imageMatchSettings.setAccessibility(accessibilityRegions.toArray(new AccessibilityRegionByRectangle[0]));

    }
}