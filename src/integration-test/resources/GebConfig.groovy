
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver


if (!System.getProperty("webdriver.chrome.driver")) {
    System.setProperty("webdriver.chrome.driver", "node_modules/chromedriver/bin/chromedriver")
}
driver = { new ChromeDriver() }
baseUrl = 'http://localhost:8080/'
environments {

    reportsDir = 'target/geb-reports'

    // run as grails -Dgeb.env=chrome test-app
    chrome {

        driver = { new ChromeDriver() }
    }

    firefox {
        driver = { new FirefoxDriver() }
    }

    chromeHeadless {

        if (!System.getProperty("webdriver.chrome.driver")) {
            System.setProperty("webdriver.chrome.driver", "node_modules/chromedriver/bin/chromedriver")
        }
        driver = {
            ChromeOptions o = new ChromeOptions()
            o.addArguments('headless')
            o.addArguments("window-size=1920,1080")
            o.addArguments('--disable-dev-shm-usage')
            new ChromeDriver(o)
        }
    }

}
