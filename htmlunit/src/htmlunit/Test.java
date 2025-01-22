package htmlunit;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Date dBefore = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
		Date dNow = new Date(System.currentTimeMillis());
		SimpleDateFormat ft = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		System.out.println(ft.format(dBefore));
		System.out.println(ft.format(dNow));
	}

}
