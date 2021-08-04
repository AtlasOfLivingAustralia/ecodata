package au.org.ala.ecodata

import javax.management.ObjectName
import java.lang.management.ManagementFactory
import com.mongodb.management.JMXConnectionPoolListener
import com.mongodb.ServerAddress
import com.mongodb.connection.ClusterId
import com.mongodb.connection.ConnectionPoolSettings
import com.mongodb.connection.ServerId

class ConnectionPoolStatsJob {
    static triggers = {
       // simple repeatInterval: 5 * 60 * 1000l // execute job once every five minutes
    }


    def execute() {
        // TODO Mbean objectname cannot be obtained at the moment as clusterId is generated. need to find a way to get generated id or use another method to monitor
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