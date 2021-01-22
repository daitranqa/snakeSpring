package report;


import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.log4j.Logger;
import org.testng.ITestContext;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentManager {
    private static final Logger LOGGER = Logger.getLogger(ExtentManager.class);

    public static String reportFileName = "Automation_Report";
    public static String windowsPath = System.getProperty("user.dir") + File.separator + "TestResult";
    public static String reportFilePath;
    private static volatile ExtentReports extent = null;

    private ExtentManager(){}

    public static synchronized ExtentReports getReporter() {
        if (extent == null) {
            try {
                freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
            } catch (ClassNotFoundException ex){
                LOGGER.error(ex.getMessage());
            }

            extent = new ExtentReports();
            ExtentHtmlReporter htmlReport = new ExtentHtmlReporter(getFileLocationBasedOnPlatform(""));
            htmlReport.config().setTheme(Theme.STANDARD);
            extent.attachReporter(htmlReport);
        }
        return extent;
    }

    public static synchronized ExtentReports init(ITestContext context){
        extent = new ExtentReports();
        String suiteName = context.getCurrentXmlTest().getSuite().getName().replaceAll(" ","");
        String testName = context.getCurrentXmlTest().getName().replaceAll(" ","");
        String reportName = suiteName +"_" + testName;
        LOGGER.info(String.format("Running the test suite: %s, test name %s", suiteName, testName));
        ExtentHtmlReporter htmlReport = new ExtentHtmlReporter(getFileLocationBasedOnPlatform(reportName));

        htmlReport.config().setTheme(Theme.DARK);
        htmlReport.config().setDocumentTitle(reportName);
        htmlReport.config().setReportName(reportName + " [Environment="+ System.getProperty("environment")
                                            + "-ENVID="+System.getProperty("DefaultENVID")+"]");
        extent.attachReporter(htmlReport);
        ExtentTestManager.extent = extent;
        extent.setSystemInfo("Suite Name", suiteName);
        extent.setSystemInfo("Test name", testName);

        return extent;
    }
    private static void createReportPath(String path) {
        File testDirectory = new File(path);
        if (!testDirectory.exists()) {
            if (testDirectory.mkdir()) LOGGER.info(String.format("Directory: %s is created.", path));
        } else LOGGER.info("Directory already exists");
    }

    private static String getFileLocationBasedOnPlatform(String suiteName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String ts = sdf.format(new Date());
        if(suiteName.equalsIgnoreCase("")) suiteName = reportFileName;
        String path = windowsPath + File.separator + suiteName.replaceAll(" ","") + "_" + ts +".html";
        reportFilePath = path;
        createReportPath(windowsPath);
        return path;
    }

}
