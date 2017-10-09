package au.org.ala.ecodata

import javax.management.ObjectName
import java.lang.management.ManagementFactory

class ConnectionPoolStatsJob {
    static triggers = {
        simple repeatInterval: 5 * 1000l // execute job once per minute
    }

    def execute() {
        ObjectName connectionPool = new ObjectName("org.mongodb.driver:type=ConnectionPool,clusterId=1,host=localhost,port=27017")
        int maxSize = ManagementFactory.getPlatformMBeanServer().getAttribute(connectionPool, "MaxSize")
        int checkedOut = ManagementFactory.getPlatformMBeanServer().getAttribute(connectionPool, "CheckedOutCount")

        if (log.isDebugEnabled()) {
            log.debug("Checked out ${checkedOut} of ${maxSize} connections")
        }

        if ((maxSize - checkedOut) <= 5) {
            log.warn("Warning: Checked out ${checkedOut} of ${maxSize} connections")
        }

    }
}