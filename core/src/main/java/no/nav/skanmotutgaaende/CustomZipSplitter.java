package no.nav.skanmotutgaaende;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.dataformat.zipfile.ZipIterator;
import org.apache.camel.dataformat.zipfile.ZipSplitter;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.Key;
import java.util.Base64;

public class CustomZipSplitter extends ZipSplitter {
	public CustomZipSplitter() {
	}

	@Override
	public Object evaluate(Exchange exchange) {
		try{
			Message inputMessage = exchange.getIn();
			String zipPassword = "password";

			ZipInputStream zip = new ZipInputStream(exchange.getIn().getBody(InputStream.class), zipPassword.toCharArray());

			return new ZipIterator(exchange,zip);
		}catch (Exception e){
			//TODO: FIX
			return null;
		}
	}

	/*@Override
	public Object evaluate(Exchange exchange) {
		try{
			Message inputMessage = exchange.getIn();
			byte[] decodedKey = Base64.getDecoder().decode("123");
			SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
			//3505bb564abaa37555ed24bf4d8f70f0
			SecretKeySpec secKey = new SecretKeySpec("3505bb564abaa37555ed24bf4d8f70f0".getBytes(), "AES");
			final Cipher c2 = Cipher.getInstance("AES");
			c2.init(Cipher.DECRYPT_MODE, secKey);
			return new ZipIterator(exchange, new CipherInputStream(inputMessage.getBody(InputStream.class), c2));
		}catch (Exception e){
			return null;
		}
	}*/
	@Override
	public <T> T evaluate(Exchange exchange, Class<T> type) {
		Object result = this.evaluate(exchange);
		return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
	}
}
