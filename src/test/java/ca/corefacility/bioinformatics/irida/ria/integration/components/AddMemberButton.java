package ca.corefacility.bioinformatics.irida.ria.integration.components;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AddMemberButton {
	@FindBy(className = "t-add-member-btn")
	private WebElement addMemberBtn;

	@FindBy(className = "t-add-member-modal")
	private WebElement addMemberModal;

	@FindBy(className = "t-new-member")
	private List<WebElement> newMemberList;

	@FindBy(css = ".t-add-member-modal .ant-select-selection-search-input")
	private WebElement addMemberInput;

	private static WebDriverWait wait;

	public static AddMemberButton getAddMemberButton(WebDriver driver) {
		wait = new WebDriverWait(driver, 3);
		return PageFactory.initElements(driver, AddMemberButton.class);
	}

	public void addMember(WebDriver driver, String name) {
		wait.until(ExpectedConditions.elementToBeClickable(addMemberBtn));
		addMemberBtn.click();
		wait.until(ExpectedConditions.visibilityOf(addMemberModal));
		WebElement input = driver.switchTo().activeElement();
		input.sendKeys(name);
		wait.until(ExpectedConditions.visibilityOf(newMemberList.get(0)));
		newMemberList.get(0).click();
		WebElement modalOkBtn = addMemberModal.findElement(By.cssSelector(".ant-btn.ant-btn-primary"));
		wait.until(ExpectedConditions.elementToBeClickable(modalOkBtn));
		modalOkBtn.click();
		wait.until(ExpectedConditions.invisibilityOf(addMemberModal));
	}
}
