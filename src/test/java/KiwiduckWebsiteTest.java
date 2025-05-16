import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Тестовый класс для проверки функциональности веб-сайта Kiwiduck.
 */
public class KiwiduckWebsiteTest {
    private WebDriver driver;

    /**
     * Настройка драйвера перед всеми тестами в классе.
     * Использует версию браузера из конфигурации, если указана.
     */
    @BeforeClass
    public static void setupAll() {
        String browserVersion = Config.getBrowserVersion();
        if (browserVersion == null || browserVersion.isEmpty()) {
            WebDriverManager.chromedriver().setup();
        } else {
            WebDriverManager.chromedriver().driverVersion(browserVersion).setup();
        }
    }

    /**
     * Настройка перед каждым тестом:
     * 1. Инициализация профиля браузера
     * 2. Создание и настройка экземпляра WebDriver
     */
    @BeforeMethod
    public void setup() {
        initializeProfileDirectory();
        ChromeOptions options = createChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    /**
     * Инициализация директории профиля браузера.
     * Удаляет существующую директорию (если есть) и создает новую.
     */
    private void initializeProfileDirectory() {
        try {
            Path profileDir = Paths.get(System.getProperty("user.dir"), "target", "profile");

            // Удаляем существующую директорию, если она есть
            if (Files.exists(profileDir)) {
                Files.walk(profileDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete file: " + path + " - " + e.getMessage());
                    }
                });
            }

            // Создаем новую директорию
            Files.createDirectories(profileDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create profile directory", e);
        }
    }

    /**
     * Создает и настраивает объект ChromeOptions с параметрами для тестов.
     *
     * @return настроенный объект ChromeOptions
     */
    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--user-data-dir=" + Paths.get(System.getProperty("user.dir"), "target", "profile"));
        return options;
    }

    /**
     * Завершение работы после каждого теста.
     * Закрывает браузер и обрабатывает возможные исключения.
     */
    @AfterMethod
    public void tearDown() {
        try {
            if (driver != null) {
                driver.quit();
            }
        } catch (Exception e) {
            System.err.println("Error during driver quit: " + e.getMessage());
        }
    }

    @Test
    public void testPromptAlertConfirmPageOptimized() {
        WebDriverWait instantWait = new WebDriverWait(driver, Duration.ofMillis(100));
        driver.get(Config.getAlertsPageUrl());

        clickWithJs(driver, By.xpath("//button[text()='Get password']"));

        Alert alert = waitForAlertOptimized(driver);
        String password = extractPassword(alert.getText());
        alert.accept();

        clickWithJs(driver, By.xpath("//button[text()='Enter password']"));

        Alert prompt = waitForAlertOptimized(driver);
        prompt.sendKeys(password);
        prompt.accept();

        assertFalse(driver.findElements(By.xpath("//label[text()='Great!']")).isEmpty());
        assertFalse(driver.findElements(By.xpath("//button[text()='Return to menu']")).isEmpty());

        clickWithJs(driver, By.xpath("//button[text()='Return to menu']"));

        try {
            Alert confirm = instantWait.until(ExpectedConditions.alertIsPresent());
            confirm.accept();
        } catch (TimeoutException e) {
            // Подтверждение не появилось, это нормально
        }
    }

    @Test
    public void testPromptAlertConfirmPageOptimized_Unsuccessful() {
        WebDriverWait instantWait = new WebDriverWait(driver, Duration.ofMillis(100));
        driver.get(Config.getAlertsPageUrl());

        clickWithJs(driver, By.xpath("//button[text()='Get password']"));

        Alert alert = waitForAlertOptimized(driver);
        String password = extractPassword(alert.getText() + 1);
        alert.accept();

        clickWithJs(driver, By.xpath("//button[text()='Enter password']"));

        Alert prompt = waitForAlertOptimized(driver);
        prompt.sendKeys(password);
        prompt.accept();

        assertTrue(driver.findElements(By.xpath("//label[text()='Great!']")).isEmpty());
        assertTrue(driver.findElements(By.xpath("//button[text()='Return to menu']")).isEmpty());

        clickWithJs(driver, By.xpath("//button[text()='Return to menu']"));

        try {
            Alert confirm = instantWait.until(ExpectedConditions.alertIsPresent());
            confirm.accept();
        } catch (TimeoutException e) {
            // Подтверждение не появилось, это нормально
        }
    }

    @Test
    public void testKiwiDuckTable_Success() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        final int RECORDS_TO_ADD = 10;
        driver.get(Config.getTablePageUrl());

        WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customers")));
        List<WebElement> checkboxes = table.findElements(
                By.xpath(".//td[text()='UK' or text()='Germany']/parent::tr//input[@type='checkbox']"));

        for (WebElement checkbox : checkboxes) {
            wait.until(ExpectedConditions.elementToBeClickable(checkbox)).click();
        }

        WebElement deleteButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@type='button' and @value='Delete']")));
        deleteButton.click();

        for (int i = 1; i <= RECORDS_TO_ADD; i++) {
            WebElement companyField = driver.findElement(
                    By.xpath("//label[text()='Company']/following-sibling::input"));
            WebElement contactField = driver.findElement(
                    By.xpath("//label[text()='Contact']/following-sibling::input"));
            WebElement countryField = driver.findElement(
                    By.xpath("//label[text()='Country']/following-sibling::input"));
            WebElement addButton = driver.findElement(
                    By.xpath("//input[@type='button' and @value='Add']"));

            companyField.clear();
            companyField.sendKeys("Компания " + i);
            contactField.clear();
            contactField.sendKeys("Контакт " + i);
            countryField.clear();
            countryField.sendKeys("Страна " + i);

            addButton.click();
        }

        WebElement successLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href and text()='Great! Return to menu']")));
        assertTrue(successLink.isDisplayed(), "Success link should be visible after operations");

        successLink.click();
    }


    private void clickWithJs(WebDriver driver, By locator) {
        WebElement element = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private Alert waitForAlertOptimized(WebDriver driver) {
        Instant endTime = Instant.now().plusSeconds(3);
        while (Instant.now().isBefore(endTime)) {
            try {
                return driver.switchTo().alert();
            } catch (NoAlertPresentException e) {
                sleep();
            }
        }
        throw new TimeoutException("Alert not present after " + 3 + " seconds");
    }


    private String extractPassword(String alertText) {
        Pattern pattern = Pattern.compile("(?:password|пароль)[: ]*(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(alertText);
        return matcher.find() ? matcher.group(1) : alertText;
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }
}