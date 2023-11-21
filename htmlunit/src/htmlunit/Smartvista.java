package htmlunit;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class Smartvista {

	@SuppressWarnings("resource")
	public static void main(String[] args) {

		String login = "Astan";
		String password = "1234567";
		String cardNumber_t = "9990080816422815";
		String dateFrom_t = "01/11/2022 00:00";
		String dateTo_t = "19/11/2023 23:59";

		int timeForWhait = 5000 * 2;

		WebClient webClient = new WebClient(BrowserVersion.CHROME);

		try {
			String webPageURl = "http://185.30.105.112:7001/SVFE2/login.jsf";

			HtmlPage signUpPage = webClient.getPage(webPageURl);

			HtmlForm form = signUpPage.getHtmlElementById("LoginForm");
			HtmlTextInput userField = form.getInputByName("LoginForm:Login");
			userField.setValueAttribute(login);
			HtmlInput pwField = form.getInputByName("LoginForm:Password");
			pwField.setValueAttribute(password);
			HtmlSubmitInput submitButton = form.getInputByName("LoginForm:submit");

			HtmlPage pageAfterLogon = submitButton.click();
			HtmlForm userForm = pageAfterLogon.getHtmlElementById("UserForm");
			HtmlTextInput cardNumber = userForm.getInputByName("UserForm:hpan");
			cardNumber.setValueAttribute(cardNumber_t);
			HtmlInput dateFrom = pageAfterLogon.getHtmlElementById("UserForm:j_id96InputDate");
			dateFrom.setValueAttribute(dateFrom_t);
			HtmlInput dateTo = pageAfterLogon.getHtmlElementById("UserForm:j_id99InputDate");
			dateTo.setValueAttribute(dateTo_t);
			HtmlInput searchbutton = pageAfterLogon.getHtmlElementById("UserForm:searchButton");

			HtmlPage pageAfterClick = (HtmlPage) searchbutton.click();
			webClient.waitForBackgroundJavaScript(timeForWhait);
			HtmlAnchor loadXLSFile = pageAfterLogon.getHtmlElementById("UserForm:loadXLSFile");

			WebWindow window = pageAfterClick.getEnclosingWindow();
			loadXLSFile.click();
			UnexpectedPage downloadPage = (UnexpectedPage) window.getEnclosedPage();
			InputStream xlsx = downloadPage.getInputStream();

			File targetFile = new File("D:\\Users\\saidp\\Documents\\smarta.xlsx");

			FileUtils.copyInputStreamToFile(xlsx, targetFile);
			// byte[] buf = ByteStreams.toByteArray(xlsx);

		} catch (Exception e) {
			System.out.println(getStackTrace(e));
		}
	}

	public static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
}
