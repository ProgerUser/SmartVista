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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.google.common.io.ByteStreams;

public class Smartvista {

	static Properties prop = new Properties();
	Logger logger = Logger.getLogger(Smartvista.class);

	public static void main(String[] args) {
		String dateFrom = "";
		String dateTo = "";
		System.out.println(args[0]);
		System.out.println(args[1]);
		if (args.length != 0) {
			dateFrom = args[0];
			dateTo = args[1];
			Smartvista sfve = new Smartvista();
			sfve.RunOper(dateFrom, dateTo);
		} else {
			Runtime.getRuntime().exit(0);
		}
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
			logger.error(getStackTrace(e), e);
		}
	}

	private static final long BG_JS_WAIT_MS = 5000;

	/**
	 * @param domElement
	 * @return
	 * @throws IOException
	 */
	public HtmlPage clickOn(DomElement domElement) throws IOException {
		HtmlPage htmlPage = domElement.click();

		htmlPage.getWebClient().waitForBackgroundJavaScript(BG_JS_WAIT_MS);

		return htmlPage;
	}

	@SuppressWarnings("resource")
	public void RunUpdateAccOst() {
		try {

			String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j.xml";
			DOMConfigurator.configure(log4jConfigFile);

			logger.info("Run SVFE: " + Thread.currentThread().getName());

			try (InputStream input = new FileInputStream("config.properties")) {
				prop.load(input);
			} catch (IOException e) {
				logger.error(getStackTrace(e), e);
			}

			try {
				dbConnect(prop.getProperty("password"), prop.getProperty("login"), prop.getProperty("url"));
			} catch (UnknownHostException e) {
				logger.error(getStackTrace(e), e);
			}

			String login = prop.getProperty("login_svfe");
			String password = prop.getProperty("password_svfe");

			int timeForWhait = 5000;

			PreparedStatement prp = conn.prepareStatement("SELECT to_char(DT, 'dd/MM/yyyy') || ' 00:00' DATE_FROM,\r\n"
					+ "       to_char(DT, 'dd/MM/yyyy') || ' 23:59' DATE_TO,\r\n"
					+ "       to_char(to_date(DT || ' ' || TM, 'dd/MM/yyyy hh24:mi:ss') -\r\n"
					+ "               5 / 24 / 60,\r\n" + "               'dd/MM/yyyy hh24:mi:ss') date_before,\r\n"
					+ "       to_char(to_date(DT || ' ' || TM, 'dd/MM/yyyy hh24:mi:ss') +\r\n"
					+ "               5 / 24 / 60,\r\n" + "               'dd/MM/yyyy hh24:mi:ss') date_after,\r\n"
					+ "       to_char(TR_NUM_FE) TR_NUM_FE\r\n" + "  FROM sbra_smart_tranz t\r\n"
					+ " WHERE EXISTS (SELECT NULL FROM plc WHERE plc.cplcnum = CARD_NUMBER)\r\n"
					+ "   AND ost IS NULL");
			ResultSet rs = prp.executeQuery();

			while (rs.next()) {

				String v_dateFrom = rs.getString("DATE_FROM");
				String v_dateTo = rs.getString("DATE_TO");
				String v_utrnno = rs.getString("TR_NUM_FE");

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
				HtmlTextInput accNumber = userForm.getInputByName(prop.getProperty("site_utrnno"));
				accNumber.setValueAttribute(v_utrnno);
				// Дата с
				HtmlInput dateFrom = pageAfterLogon.getHtmlElementById(prop.getProperty("site_dt1"));
				dateFrom.setValueAttribute(v_dateFrom);
				// Дата по
				HtmlInput dateTo = pageAfterLogon.getHtmlElementById(prop.getProperty("site_dt2"));
				dateTo.setValueAttribute(v_dateTo);
				// Кнопка поиска
				HtmlInput searchbutton = pageAfterLogon.getHtmlElementById(prop.getProperty("site_searchbtn"));
				// После нажатия поиска
				HtmlPage pageAfterClick = (HtmlPage) searchbutton.click();
				webClient.waitForBackgroundJavaScript(timeForWhait);

				// {02.05.2024}
				// Выбрать строку
				HtmlTableRow contentRow = pageAfterClick.getHtmlElementById("UserForm:tiD:n:0");
				// Получить страницу
				HtmlPage pageaAfterClickRow = clickOn(contentRow);

				System.out.println(pageaAfterClickRow.getHtmlPageOrNull());

				// Получить таблицу с атрибутами
				HtmlTable tableWithAttr = pageaAfterClickRow.getHtmlElementById("UserForm:j_id589_shifted");
				// Получить страницу
				HtmlPage pageaAfterClickAccOst = clickOn(tableWithAttr);
				// Получить ячейку с остатком
				HtmlTableCell accspan = pageaAfterClickAccOst.getHtmlElementById("UserForm:j_id589");

				System.out.println(accspan.asNormalizedText());

				CallableStatement callStmt = conn.prepareCall("{ ? = call sbra_svfe_xlsx2(?,?)}");
				callStmt.registerOutParameter(1, Types.VARCHAR);
				callStmt.setString(2, accspan.asNormalizedText());
				callStmt.setString(3, v_utrnno);
				try {
					callStmt.execute();
				} catch (SQLException e) {
					logger.error(getStackTrace(e), e);
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
			// Отключить сессию
			dbDisconnect();
		} catch (Exception e) {
			dbDisconnect();
			logger.error(getStackTrace(e), e);
		}
	}

	@SuppressWarnings("resource")
	public void RunOper(String v_dateFrom, String v_dateTo) {

		String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j.xml";
		DOMConfigurator.configure(log4jConfigFile);

		logger.info("Run SVFE: " + Thread.currentThread().getName());

		try (InputStream input = new FileInputStream("config.properties")) {
			prop.load(input);
		} catch (IOException e) {
			logger.error(getStackTrace(e), e);
		}

		try {
			dbConnect(prop.getProperty("password"), prop.getProperty("login"), prop.getProperty("url"));
		} catch (UnknownHostException e) {
			logger.error(getStackTrace(e), e);
		}

		logger.info("Start!");

		String login = prop.getProperty("login_svfe");
		String password = prop.getProperty("password_svfe");

		// Date dBefore = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
		// Date dNow = new Date(System.currentTimeMillis());
		// SimpleDateFormat ft = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

		// String cardNumber_t = "";
		String dateFrom_t = v_dateFrom;// ft.format(dBefore);//"01/01/2000 00:00";
		String dateTo_t = v_dateTo;// ft.format(dNow);//"30/11/2024 23:59";

		// System.out.println(dateFrom_t);
		// System.out.println(dateTo_t);

		int timeForWhait = Integer.valueOf(prop.getProperty("timeForWhait"));

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
			///// HtmlForm userForm =
			// pageAfterLogon.getHtmlElementById(prop.getProperty("site_userform"));
			/// Поле со карте
			///// HtmlTextInput accNumber =
			// userForm.getInputByName(prop.getProperty("site_card"));
			///// accNumber.setValueAttribute(cardNumber_t);
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

			// Нажать на кнопку скачать excel
			HtmlAnchor loadXLSFile = pageAfterLogon.getHtmlElementById(prop.getProperty("site_loadXLSFile"));
			// Окно скачивания
			WebWindow window = pageAfterClick.getEnclosingWindow();
			loadXLSFile.click();
			UnexpectedPage downloadPage = (UnexpectedPage) window.getEnclosedPage();

			// Обработать файл в вида InputStream
			try (InputStream xlsx = downloadPage.getInputStream()) {

				// Call Stored Function
				CallableStatement callStmt = conn.prepareCall(prop.getProperty("pl_sql_call"));
				callStmt.registerOutParameter(1, Types.VARCHAR);
				byte[] buf = ByteStreams.toByteArray(xlsx);
				callStmt.setBlob(2, new ByteArrayInputStream(buf), buf.length);
				try {
					callStmt.execute();
				} catch (SQLException e) {
					logger.error(getStackTrace(e), e);
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

			// Отключить сессию
			dbDisconnect();
			logger.info("Done!");
		} catch (Exception e) {
			dbDisconnect();
			logger.error(getStackTrace(e), e);
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
			logger.error(getStackTrace(e), e);
		}
	}

	public static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
}
