package report;

import com.aventstack.extentreports.Status;
import com.sandata.core.Wrapper;
import com.sandata.core.config.TestContext;
import com.sandata.core.config.TestType;
import org.apache.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {
    public static final Logger LOGGER = Logger.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult iTestResult) {
/* This should be run when test suite start.
        // Access QTest and get all test cases if set update_QTest is TRUE
        LOGGER.info("Mark Passed On QTest = " + markPassedFlag);
        if (markPassedFlag.equals("TRUE")) {
            String userQTest = System.getProperty("userQTest");
            String passQTest = System.getProperty("passQTest");
            String url = System.getProperty("url");
            String projectName = System.getProperty("projectName");
            String parentModuleId = System.getProperty("parentModuleId");
            String moduleName = System.getProperty("moduleName");
            if (userQTest == null || passQTest == null || url == null || projectName == null
                    || parentModuleId == null || moduleName == null) {
                Assert.fail(String.format("Environment variable(s) for QTest is NULL, please check again " +
                                "userQTest=%spassQTest=%surl=%sprojectName=%sparentModuleId=%smoduleName=%s",
                        userQTest, passQTest, url, projectName, parentModuleId, moduleName));
            }
            qTestAPI = new QTestAPI(userQTest, passQTest, url, projectName, parentModuleId, moduleName);
            // get All Test Cases on qTest corresponding to value in TC no field
            qTestAPI.getAllTestCasesInfoByOldIds("TC no");
        }
*/


        //TODO: Reserved
/* Once Test start check if we need to create new test cycle for test name, test suite for package
        if (interFace != null && api != null && !testContext.getCurrentXmlTest().getXmlPackages().isEmpty()
                && markPassedFlag.equals("TRUE")) {
            String testName = testContext.getCurrentXmlTest().getName();
            qTestAPI.createTestCycle(testName + " Cycle");
            for (XmlPackage xmlPackage : testContext.getCurrentXmlTest().getXmlPackages()) {
                String suiteName = xmlPackage.getName();
                qTestAPI.createTestSuiteFromCycle(suiteName);
                LOGGER.info(suiteName);
            }
        }
*/

    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        ExtentTestManager.getTest().log(Status.PASS, iTestResult.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        LOGGER.info("------------------ @Script is failed - Try capture the screenshot if any------------------------");
        String methodName = iTestResult.getMethod().getMethodName().trim();
        LOGGER.info(String.format("%s is failed", methodName));
        ExtentTestManager.getTest().log(Status.FAIL, iTestResult.getMethod().getMethodName());
        ExtentTestManager.getTest().fail(iTestResult.getThrowable());
        if (TestContext.get().getConfiguration().getTestType().equalsIgnoreCase(TestType.UI)){
            new Wrapper().getScreenShotOnFailure(iTestResult.getMethod().getMethodName());
            LOGGER.info("Successfully capture screenshot on failure");
        }

//        if (result.getStatus() == ITestResult.FAILURE) {
////			logger.log(LogStatus.FAIL, result.getThrowable());
//            // set flag of test case status on qTest
//            flagToSetStatusTestCaseOnQTest = QTestAPI.STATUS.FAIL;
//        } else if (result.getStatus() == ITestResult.SKIP || result.getStatus() == -1) {
////			logger.log(LogStatus.SKIP, "Test skipped " + result.getThrowable());
//            flagToSetStatusTestCaseOnQTest = QTestAPI.STATUS.SKIP;
//        } /*else if (result.getStatus() == ITestResult.SUCCESS) {

    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        //TODO: reserved
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {
        //TODO: reserved
    }

    @Override
    public void onStart(ITestContext iTestContext) {
        //TODO: reserved
        // On method start check if we need to create new test run of tc id annoation
    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        //TODO: reserved
        // Sending the  final result to new test run/ updating existing test run

/*
        // update Status of Test Case on QTest if set update_QTest is TRUE
        try {
            if (markPassedFlag.equals("TRUE")) {
                String endDateTime = OffsetDateTime.now(zoneOffset).toString();
                if (flagToSetStatusTestCaseOnQTest.equals(QTestAPI.STATUS.PASS)) {
                    qTestAPI.addTestLog(testCaseQTestInfo, QTestAPI.STATUS.PASS.toString(), startDateTime, endDateTime);
                }
                else {
                    if (flagToSetStatusTestCaseOnQTest.equals(QTestAPI.STATUS.FAIL)) {
                        qTestAPI.addTestLog(testCaseQTestInfo, QTestAPI.STATUS.FAIL.toString(), startDateTime, endDateTime);
                    }
                    else {
                        qTestAPI.addTestLog(testCaseQTestInfo, QTestAPI.STATUS.SKIP.toString(), startDateTime, endDateTime);
                    }
                }
            }
        } catch (Exception e) {
            e.getStackTrace();
            logger.log(LogStatus.INFO, "Test Failed but not marked in QTest - markPassedFlag variable probably NULL");
        }
        String testCaseName = this.getClass().getSimpleName();
        LOGGER.info("******************* END TEST CLASS - " + testCaseName + " - *******************");
*/
    }
}
