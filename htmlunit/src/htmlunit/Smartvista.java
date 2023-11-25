package htmlunit;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

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
import com.google.common.io.ByteStreams;

public class Smartvista {

	static Properties prop = new Properties();

	public static void main(String[] args) {
		Smartvista sfve = new Smartvista();
		sfve.RunOper();
	}

	/**
	 * Сессия
	 */
	private Connection conn;

	/**
	 * Открыть сессию
	 * 
	 * @throws UnknownHostException
	 */
	private void dbConnect(String userPassword_, String userID_, String connectionURL_) throws UnknownHostException {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
			Properties props = new Properties();
			props.setProperty("password", userPassword_);
			props.setProperty("user", userID_);
			props.put("v$session.osuser", System.getProperty("user.name").toString());
			props.put("v$session.machine", InetAddress.getLocalHost().getHostAddress());
			props.put("v$session.program", getClass().getName());
			conn = DriverManager.getConnection("jdbc:oracle:thin:@" + connectionURL_, props);
			conn.setAutoCommit(false);
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	public void RunOper() {
		try (InputStream input = new FileInputStream("config.properties")) {
			prop.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		InputStream xlsx = null;
		String login = "Astan";
		String password = "1234567";
		String cardNumber_t = "9990080816422815";
		String dateFrom_t = "01/11/2000 00:00";
		String dateTo_t = "25/11/2023 23:59";
		int timeForWhait = 5000 * 2;

		try {
			WebClient webClient = new WebClient(BrowserVersion.CHROME);
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

			xlsx = downloadPage.getInputStream();

			{
				dbConnect(prop.getProperty("password"), prop.getProperty("login"), prop.getProperty("url"));

				CallableStatement callStmt = conn.prepareCall("{ call Z_SB_CALC_CONTACT.LOAD(?,?,?,?)}");
				callStmt.registerOutParameter(1, Types.VARCHAR);
				callStmt.registerOutParameter(4, Types.VARCHAR);

				byte[] buf = ByteStreams.toByteArray(xlsx);
				// add parameters
				callStmt.setBlob(2, new ByteArrayInputStream(buf), buf.length);
				// catch
				try {
					callStmt.execute();
				} catch (Exception e) {
					conn.rollback();
					callStmt.close();
				}
			}
			dbDisconnect();

		} catch (Exception e) {
			dbDisconnect();
			try {
				xlsx.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println(getStackTrace(e));
		}
	}

	/**
	 * Отключить сессию
	 */
	public void dbDisconnect() {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.rollback();
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
}
