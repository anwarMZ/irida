package ca.corefacility.bioinformatics.irida.ria.integration.admin;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ca.corefacility.bioinformatics.irida.ria.integration.AbstractIridaUIITChromeDriver;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.LoginPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.admin.AdminPage;

import com.github.springtestdbunit.annotation.DatabaseSetup;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

@ActiveProfiles("it")
@DatabaseSetup("/ca/corefacility/bioinformatics/irida/ria/web/admin/AdminPageView.xml")
public class AdminPageIT extends AbstractIridaUIITChromeDriver {

	@Test
	public void accessPageAsAdmin() {
		LoginPage.loginAsAdmin(driver());
		AdminPage page = AdminPage.initPage(driver());
		assertTrue("Admin button should be displayed", page.adminPanelButtonVisible());
		page.clickAdminButton();
		assertTrue("Admin can navigate to admin panel, admin page title should be present", page.adminPanelTitleVisible());
	}

	@Test
	public void accessPageFailure() {
		LoginPage.loginAsUser(driver());
		AdminPage page = AdminPage.initPage(driver());
		assertFalse("Admin button should not be displayed", page.adminPanelButtonVisible());
		// No admin button, so attempt to go to admin page by modifying the URL
		page.goToAdminPage(driver());
		assertFalse("User cannot navigate to admin panel, admin page title should not be present", page.adminPanelTitleVisible());
	}

	@Test
	public void testPageSetUp() {
		LoginPage.loginAsManager(driver());
		AdminPage page = AdminPage.initPage(driver());
		page.clickAdminButton();
		// default page is statistics pagee
	}
}