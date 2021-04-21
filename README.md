# CUSTOM README PART

This repo is used for creating a Selenium4 version of the eyes.sdk.java3 of AppliTools.
It is forked on GitHub and can be updated there.

The Bamboo Build is set to build the Develop branch, you need to set the maven release in your last 
commit. Always use the version as set by AppliTools and add the `-SEL4-SNAPSHOT`

Update all versions: `mvn --batch-mode release:update-versions -DdevelopmentVersion=3.199.1-SEL4-SNAPSHOT`

Make a release `mvn release:clean release:prepare -DskipTests=true`

# eyes.sdk.java3
Applitools Eyes Java SDK Version 3.

For a tutorial on how to use the SDK, check out the Applitools website:

- Selenium Java example: https://applitools.com/resources/tutorial/selenium/java#step-2

- Java Appium Native example: https://applitools.com/resources/tutorial/appium/native_java#step-2

- Java Appium Web example: https://applitools.com/resources/tutorial/appium/java#step-2

- Java Screenshots example: https://applitools.com/resources/tutorial/screenshots/java#step-2
