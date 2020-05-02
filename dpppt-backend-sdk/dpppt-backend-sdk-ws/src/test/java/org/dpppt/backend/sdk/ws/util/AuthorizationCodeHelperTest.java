package org.dpppt.backend.sdk.ws.util;

import org.junit.Test;

import static org.dpppt.backend.sdk.ws.util.AuthorizationCodeHelper.generateAuthorizationCode;
import static org.dpppt.backend.sdk.ws.util.AuthorizationCodeHelper.getAuthorizationCode;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorizationCodeHelperTest {

	@Test
	public void test() {
		byte[] code1 = generateAuthorizationCode();

		String codeBaseX = AuthorizationCodeHelper.getBaseXAuthorizationCode(code1);
		byte[] code2 = getAuthorizationCode(codeBaseX);
		assertArrayEquals(code1, code2);

		String codeBaseXLowerCase = codeBaseX.toLowerCase();
		byte[] code3 = getAuthorizationCode(codeBaseXLowerCase);
		assertArrayEquals(code1, code3);

		String invalidCodeBaseX1 = codeBaseX + "+";
		byte[] code4 = getAuthorizationCode(invalidCodeBaseX1);
		assertArrayEquals(code1, code4);

		String invalidCodeBaseX2 = "+" + codeBaseX;
		byte[] code5 = getAuthorizationCode(invalidCodeBaseX2);
		assertArrayEquals(code1, code5);

		String invalidCodeBaseX3 = codeBaseX.substring(0, codeBaseX.length() / 2) + "+"
				+ codeBaseX.substring(codeBaseX.length() / 2);
		byte[] code6 = getAuthorizationCode(invalidCodeBaseX3);
		assertArrayEquals(code1, code6);
	}
}
