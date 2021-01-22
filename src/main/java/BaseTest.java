import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.sandata.core.config.Environment;
import com.sandata.core.config.TestConfiguration;
import com.sandata.core.config.TestContext;
import com.sandata.core.config.TestType;
import com.sandata.core.report.ExtentManager;
import com.sandata.core.report.ExtentTestManager;
import com.sandata.core.report.TestListener;
import com.sandata.qtest.QTest;
import com.sandata.qtest.QTestAPI;
import com.sandata.qtest.QTestCaseModel;
import com.sandata.utilities.JsonReader;
import com.sandata.utilities.ReflectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Listeners(TestListener.class)
public class BaseTest {

    public static final Logger LOGGER = Logger.getLogger(BaseTest.class);
    public static final String QTEST_ENABLE_VARIABLE = "qtest.enable";
    private static final String CONS_DEFAULT_ENVIRONMENT_ID = "DefaultENVID";
    public static TestConfiguration testConfig = TestContext.initTestConfig();

    static {
        TestContext.set(testConfig);
    }

    private final Map<String, QTestCaseModel> testCases = new HashMap<>();
    public String testType;
    protected QTestAPI qTestRunner;
    protected ExtentTest test;
    protected Wrapper baseObj;
    private String dataFile;
    private String testcaseID;
    private String projectName;
    private boolean headless;
    private boolean useCustomProfile = false;
    private boolean grid = false;

    private static String getTestResultStatus(int status) {
        switch (status) {
            case ITestResult.SUCCESS:
                return "SUCCESS";
            case ITestResult.FAILURE:
                return "FAILURE";
            case ITestResult.SKIP:
                return "SKIP";
            case ITestResult.STARTED:
                return "STARTED";
            case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                return "SUCCESS_PERCENTAGE_FAILURE";
            default:
                return "UNKNOWN";
        }
    }

    private synchronized TestConfiguration getTestConfig() {
        return TestContext.get();
    }

    public String getTestType() {
        return "ui";
    }

    public String getEnvironmentStage() {
        return testConfig.getEnvStage();
    }

    public String getTestcaseID() {
        return this.testcaseID;
    }

    @BeforeSuite(alwaysRun = true)
    @Parameters({"testType"})
    public void beforeSuite(ITestContext context, @Optional("ui") String testType) throws IOException {
        this.testType = testType;
        try {
            freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException ex) {
            LOGGER.info(ex.getMessage());
        }

        ExtentManager.init(context);
        if (isQTestEnable()) qTestRunner = QTestAPI.getInstance();
    }

    @BeforeTest(alwaysRun = true)
    public void beforeTest(ITestContext context) {
        loadConfigurationFromXMLSuite(context);
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass(ITestContext context) {
        LOGGER.info("DefaultENVID is " + System.getProperty(CONS_DEFAULT_ENVIRONMENT_ID));
        LOGGER.info("------------------ @BeforeClass fired------------------------");
        String testCaseName = this.getClass().getSimpleName();
        LOGGER.info("---- Starting TC - " + testCaseName);

        //Pattern p = Pattern.compile("([A-Z0-9]*)_(TC_\\d{1,6})"); -- For keep track
        Pattern p = Pattern.compile("([A-Z]*_|^)([A-Z0-9]*)_.*?_((TC_|US_)\\d{1,6})?");
        Pattern dataPattern = Pattern.compile("^[A-Z]{5,}");
        Pattern tcIDPattern = Pattern.compile("(TC_|US_)\\d{1,6}");
        Matcher m = p.matcher(testCaseName);
        Matcher dataFileMatcher = dataPattern.matcher(testCaseName);
        Matcher tcIDMatcher = tcIDPattern.matcher(testCaseName);
        if (m.find()) {
            dataFile = dataFileMatcher.find() ? dataFileMatcher.group(0) : "";
            projectName = m.group(2);
            testcaseID = tcIDMatcher.find() ? tcIDMatcher.group(0) : "";
            LOGGER.info("\nProject Name: " + projectName + ", testCaseID: " + testcaseID + ", testData: " + dataFile);
        } else {
            logError(String.format("Cannot find the project name and test case id for test case: %s"
                    , testCaseName));
        }
        // Init Test Configuration based on Json file configuration
        initTestConfiguration();

        if (getTestConfig().getConfiguration().getTestType().equalsIgnoreCase(TestType.UI)) {
            WebDriver driver = getTestConfig().getDriver();
            String url = getTestConfig().getEnvironment().getApplication_URL();
            driver.get(url);
            LOGGER.info("Application URL " + url);
        }
        baseObj = new Wrapper();
    }

    private synchronized void initTestConfiguration() {
        testConfig.setTCID(testcaseID);
        this.testType = getTestType();
        if (testType.equalsIgnoreCase("default")) {
            this.testType = testConfig.getConfiguration().getTestType();
        } else {
            testConfig.getConfiguration().setTestType(this.testType);
        }

        if (testConfig.getConfiguration().getTestType().equalsIgnoreCase(TestType.UI)) {
            testConfig = TestContext.initWebDriver(testConfig, headless, grid, useCustomProfile);
        }

        if (dataFile.replaceAll("_", "").isEmpty()) {
            testConfig.setTestData(JsonReader.loadTestCaseData(projectName, testcaseID));
        } else {
            testConfig.setTestData(JsonReader.loadTestCaseData(dataFile.replaceAll("_", ""), projectName, testcaseID));
        }

        String envID = testConfig.getTestData().getRunningEnvID(testConfig.getEnvStage());
        Environment environment = JsonReader.loadEnvironment(testConfig.getEnvStage(), envID);
        System.setProperty(CONS_DEFAULT_ENVIRONMENT_ID, envID);
        LOGGER.info("Running on environment stage: " + testConfig.getEnvStage());

        if (environment != null) {
            testConfig.setEnvironment(environment);
        } else {
            LOGGER.error("Test aborted as there is no environment definition with ID " + envID);
        }
        TestContext.set(testConfig);
    }

    private boolean isQTestEnable() {
        return System.getProperty(QTEST_ENABLE_VARIABLE, "false").equalsIgnoreCase("true");
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method, ITestContext testContext, Object[] testData) {
        LOGGER.info("------------------ @BeforeMethod fired------------------------");
        Test testMethod = method.getAnnotation(Test.class);
        String testName = testMethod.testName().equals("") ? method.getName() : testMethod.testName();
        if (testData.length > 0) {
            testName = testName + " [" + testData[0] + "]";
        }
        LOGGER.info("Running test method" + testName);
        test = ExtentTestManager.startTest(testName, testMethod.description());
        test.assignCategory(testMethod.groups());

        if (isQTestEnable())
            loadAssociatedQTestKeysFromTestMethod(testContext, method);
    }

    private void loadAssociatedQTestKeysFromTestMethod(ITestContext testContext, Method testMethod) {
        String newSuiteName = testContext.getCurrentXmlTest().getName();
        QTest qTest = testMethod.getAnnotation(QTest.class);
        if (qTest != null) {
            String[] tcKeys = qTest.keys();
            String className = this.getClass().getName();
            String automationContent = className + "#" + testMethod.getName();
            for (String tcKey : tcKeys) {
                if (!testCases.containsKey(tcKey)) {
                    QTestCaseModel qTestModel
                            = QTestAPI.getInstance().loadAutomatedTestCaseInfo(tcKey, automationContent);
                    assert qTestModel != null;
                    qTestModel.setNewSuiteName(newSuiteName);
                    testCases.put(tcKey, qTestModel);
                }
            }
        }
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(Method method, ITestResult result) {
        //Create new test run (if not exist), then update the execution result to all the linked TC-IDs
        Test testNGAnnotation = method.getAnnotation(Test.class);
        QTest qTest = method.getAnnotation(QTest.class);
        if (isQTestEnable() && qTest != null && testNGAnnotation != null) {
            for (String tcKey : qTest.keys()) {
                QTestCaseModel qTestModel = testCases.get(tcKey);
                if (qTestModel == null)
                    baseObj.fail(String.format("Failed on updating qTest result - Unable to find the TC-key %s", tcKey));
                if (qTestModel.getFinalResult() == null) {
                    qTestModel.setFinalResult(result);
                } else if (result.getStatus() == ITestResult.FAILURE) {
                    qTestModel.setFinalResult(result);
                }
                qTestModel.addNote(buildTestNoteWithDescription(result));
            }
        }
    }

    private String buildTestNoteWithDescription(ITestResult result) {
        String note = getTestResultStatus(result.getStatus());
        String description = null;
        if (ArrayUtils.isNotEmpty(result.getParameters()) && Objects.nonNull(result.getParameters()[0])) {
            try {
                description = ReflectionUtils.getFieldValue(result.getParameters()[0], "description");
            } catch (Exception e) {
                // Ignore as there is no description information inputted for this test case.
            }
        }

        if (StringUtils.isBlank(description)) {
            note += " - <No Description>";
        } else {
            note += " - " + description;
        }

        if (result.getStatus() != ITestResult.SUCCESS) {
            String errorMessage = result.getThrowable() != null ? result.getThrowable().getMessage() : "";
            note += " - Error: " + errorMessage;
        }
        return note;
    }

    @AfterClass(alwaysRun = true)
    protected void afterClass() {
        LOGGER.info("------------------ @AfterClass fired------------------------");
        if (getTestConfig().getDriver() != null) {
            LOGGER.info("Closing Web browser");
            getTestConfig().stopDriver();
            LOGGER.info("Closing Web browser successfully");
        }
        TestContext.remove();
        LOGGER.info("Writing to HTML report");
        ExtentManager.getReporter().flush();
        if (isQTestEnable()) {
            LOGGER.info("Upload result to qTest server");
            for (QTestCaseModel qTestCaseModel : testCases.values()) {
                qTestCaseModel.setReportFilePath(ExtentManager.reportFilePath);
                QTestAPI.getInstance().submitResultToQTest(qTestCaseModel);
            }
        }
    }

    @AfterTest(alwaysRun = true)
    protected void afterTest() {
        //TODO: Reserved
    }

    @AfterSuite(alwaysRun = true)
    protected void afterSuite() {
        LOGGER.info("------------------	@AfterSuite fired -----------------------");
        LOGGER.info("Writing to HTML report");
        ExtentManager.getReporter().flush();
        LOGGER.info("------ Done");
        try {
            LOGGER.info("Copy to AutomationReport.html");
            FileUtils.copyFile(new File(ExtentManager.reportFilePath),
                    new File(ExtentManager.windowsPath + File.separator + ExtentManager.reportFileName + ".html"));
            LOGGER.info("--- Done");
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void logError(String message) {
        test.log(Status.ERROR, message);
        LOGGER.error(message);
        Assert.fail(message);
    }

    public void logPass(String message) {
        test.log(Status.PASS, message);
        LOGGER.info(message);
    }

    public void logException(String message, Exception exp) {
        test.log(Status.ERROR, exp.getMessage());
        logError(message);
    }

    public void logStepInfo(String info) {
        test.log(Status.INFO, info);
    }

    public void setUseCustomProfile(boolean useCustomProfile) {
        this.useCustomProfile = useCustomProfile;
    }

    private void loadConfigurationFromXMLSuite(ITestContext context) {
        String overrideENVID = context.getCurrentXmlTest().getParameter(CONS_DEFAULT_ENVIRONMENT_ID);
        if (overrideENVID != null) {
            System.setProperty(CONS_DEFAULT_ENVIRONMENT_ID, overrideENVID);
        }
        String xmlHeadless = context.getCurrentXmlTest().getParameter("headless");
        if (Boolean.parseBoolean(xmlHeadless)) {
            this.headless = true;
        }
        String xmlGrid = context.getCurrentXmlTest().getParameter("grid");
        if (Boolean.parseBoolean(xmlGrid)) {
            this.grid = true;
        }
        String xmlTestType = context.getCurrentXmlTest().getParameter("testType");
        if (xmlTestType == null) {
            this.testType = "default";
        }
    }
}

