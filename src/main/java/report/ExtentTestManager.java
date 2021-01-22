package report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import java.util.HashMap;
import java.util.Map;

public class ExtentTestManager {
    static Map extentTestMap = new HashMap();
    public static ExtentReports extent;

    private ExtentTestManager() { }

    public static synchronized ExtentTest startTest(String testName) {
        return startTest(testName, "");
    }

    public static synchronized ExtentTest startTest(String testName, String desc) {
        ExtentTest test = extent.createTest(testName, desc);
        extentTestMap.put((int) (long) (Thread.currentThread().getId()), test);
        return test;
    }

    public static synchronized ExtentTest getTest() {
        return (ExtentTest) extentTestMap.get((int) (long) (Thread.currentThread().getId()));
    }

    public static synchronized void logTestStep(String message) {
        getTest().log(Status.INFO,
                String.format("<b style= 'font-size: 15px;color:black'> %s </b>", message));
    }

    public static synchronized void logPass(String message) {
        getTest().log(Status.PASS,
                String.format("<b style= 'font-size: 15px;color:green'> %s </b>", message));
    }

    public static synchronized void logFailure(String message) {
        getTest().log(Status.FAIL,
                String.format("<b style= 'font-size: 15px;color:red'> %s </b>", message));
    }

    public static synchronized String getFolderPath() {
        return ExtentManager.windowsPath;
    }
}
