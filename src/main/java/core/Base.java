package core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;

public class Base {
    public static final Logger LOGGER = LogManager.getLogger();

    @BeforeClass
    public void tearUp() {
        LOGGER.info("Starting to perform test");
    }
}
