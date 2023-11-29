package htmlunit;

import java.io.ByteArrayInputStream;
import java.io.File;
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

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

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
	Logger logger = Logger.getLogger(Smartvista.class);

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
			logger.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("resource")
	public void RunOper() {

		String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j.xml";
		DOMConfigurator.configure(log4jConfigFile);

		logger.info("Run SVFE: " + Thread.currentThread().getName());

		try (InputStream input = new FileInputStream("config.properties")) {
			prop.load(input);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		try {
			dbConnect(prop.getProperty("password"), prop.getProperty("login"), prop.getProperty("url"));
		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
		}

		String login = prop.getProperty("login_svfe");
		String password = prop.getProperty("password_svfe");

		String cardNumber_t = "9990080816422815";
		String dateFrom_t = "01/11/2000 00:00";
		String dateTo_t = "25/11/2023 23:59";

		int timeForWhait = 5000 * 2;

		try {
			WebClient webClient = new WebClient(BrowserVersion.CHROME);
			// Получить URL
			String webPageURl = prop.getProperty("svfe_url");
			// Страница
			HtmlPage signUpPage = webClient.getPage(webPageURl);
			// Находим форму
			HtmlForm form = signUpPage.getHtmlElementById(prop.getProperty("site_login_form"));
			// Поле login
			HtmlTextInput userField = form.getInputByName(prop.getProperty("site_login_form_login"));
			userField.setValueAttribute(login);
			// Пароль
			HtmlInput pwdField = form.getInputByName(prop.getProperty("site_login_form_pwd"));
			pwdField.setValueAttribute(password);
			// Кнопка поиска
			HtmlSubmitInput submitButton = form.getInputByName(prop.getProperty("site_loginform_submin"));
			// После входа
			HtmlPage pageAfterLogon = submitButton.click();
			// Находим форму
			HtmlForm userForm = pageAfterLogon.getHtmlElementById(prop.getProperty("site_userform"));
			/// Поле со счетом
			HtmlTextInput accNumber = userForm.getInputByName(prop.getProperty("site_card"));
			accNumber.setValueAttribute(cardNumber_t);
			// Дата с
			HtmlInput dateFrom = pageAfterLogon.getHtmlElementById(prop.getProperty("site_dt1"));
			dateFrom.setValueAttribute(dateFrom_t);
			// Дата по
			HtmlInput dateTo = pageAfterLogon.getHtmlElementById(prop.getProperty("site_dt2"));
			dateTo.setValueAttribute(dateTo_t);
			// Кнопка поиска
			HtmlInput searchbutton = pageAfterLogon.getHtmlElementById(prop.getProperty("site_searchbtn"));
			// После нажатия поиска
			HtmlPage pageAfterClick = (HtmlPage) searchbutton.click();
			webClient.waitForBackgroundJavaScript(timeForWhait);
			// Нажать на кнопку скачать xlsx
			HtmlAnchor loadXLSFile = pageAfterLogon.getHtmlElementById(prop.getProperty("site_loadXLSFile"));
			// Окно скачивания
			WebWindow window = pageAfterClick.getEnclosingWindow();
			loadXLSFile.click();
			UnexpectedPage downloadPage = (UnexpectedPage) window.getEnclosedPage();

			// Обработать файл в вида InputStream
			try (InputStream xlsx = downloadPage.getInputStream()) {
				// Call Stored Function
				CallableStatement callStmt = conn.prepareCall("{ ? = call sbra_svfe_xlsx(?)}");
				callStmt.registerOutParameter(1, Types.VARCHAR);
				byte[] buf = ByteStreams.toByteArray(xlsx);
				callStmt.setBlob(2, new ByteArrayInputStream(buf), buf.length);
				try {
					callStmt.execute();
				} catch (SQLException e) {
					logger.error(e.getMessage(), e);
					conn.rollback();
					callStmt.close();
				}
				// Возврат ошибки
				String ret = callStmt.getString(1);
				if (!ret.equals("OK")) {
					conn.rollback();
					logger.error("SQLException = " + ret);
				} else {
					conn.commit();
				}
			}
			dbDisconnect();

		} catch (Exception e) {
			dbDisconnect();
			logger.error(e.getMessage(), e);
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
			logger.error(e.getMessage(), e);
		}
	}

	public static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
}
