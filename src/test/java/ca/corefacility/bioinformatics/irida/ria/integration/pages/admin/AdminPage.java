package ca.corefacility.bioinformatics.irida.ria.integration.pages.admin;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import ca.corefacility.bioinformatics.irida.ria.integration.pages.AbstractPage;

public class AdminPage extends AbstractPage {
	public static final String RELATIVE_URL = "/";
	public static final String ADMIN_RELATIVE_URL = "admin/";

	@FindBy(className="t-admin-panel-title")
	private List<WebElement> adminPanelTitle;

	@FindBy(className="t-admin-panel-btn")
	private List<WebElement> adminPanelBtn;

	public AdminPage(WebDriver driver) { super(driver); }

	/**
	 * Initialize the page so that the default {@Link WebElement} has been found.
	 *
	 * @param driver	{@Link WebDriver}
	 * @return The initialized {@Link AdminPage}
	 */
	public static AdminPage initPage(WebDriver driver) {
		get(driver, RELATIVE_URL);
		return PageFactory.initElements(driver, AdminPage.class);
	}

	/**
	 * Navigate to the admin panel page on path 'admin/'.
	 *
	 * @param driver	{@Link WebDriver}
	 */
	public void goToAdminPage(WebDriver driver) {
		get(driver, ADMIN_RELATIVE_URL);
	}

	/**
	 *  Determines if admin panel title is
	 *  visible on the admin panel page.
	 *
	 * @return {@link Boolean}
	 */
	public boolean adminPanelTitleVisible() {
		return adminPanelTitle.size() == 1;
	}

	/**
	 *  Determines if admin panel button is
	 *  visible on the navbar.
	 *
	 * @return {@link Boolean}
	 */
	public boolean adminPanelButtonVisible() {
		return adminPanelBtn.size() == 1;
	}

	/**
	 *  Clicks on the admin panel button to navigate
	 *  to the admin panel page.
	 */
	public void clickAdminButton() {
		adminPanelBtn.get(0).click();
		WebDriverWait wait = new WebDriverWait(driver, 2);
		wait.until(ExpectedConditions.urlContains("/admin"));
	}
}