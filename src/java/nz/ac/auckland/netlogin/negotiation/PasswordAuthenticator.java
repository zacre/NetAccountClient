package nz.ac.auckland.netlogin.negotiation;

import nz.ac.auckland.cs.des.C_Block;
import nz.ac.auckland.cs.des.Key_schedule;
import nz.ac.auckland.cs.des.desDataInputStream;
import nz.ac.auckland.cs.des.desDataOutputStream;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

public class PasswordAuthenticator implements Authenticator {

	private Random random = new SecureRandom();
	private Key_schedule schedule;
	private int clientNonce;

	public String getName() {
		return "Password";
	}

	public AuthenticationRequest startAuthentication(CredentialsCallback callback) throws LoginException, IOException {
		if (!callback.requestCredentials()) throw new CredentialNotFoundException();
		String username = callback.getUsername();
		String password = callback.retrievePassword();

		clientNonce = random.nextInt();

		schedule = new Key_schedule(password);
		desDataOutputStream outBuffer = new desDataOutputStream(128);
		outBuffer.writeInt(clientNonce);
		byte[] nonceEnc = outBuffer.des_encrypt(schedule);
		outBuffer.close();
		return new AuthenticationRequest(username, nonceEnc);
	}

	public LoginComplete validateResponse(byte[] serverResponse) throws LoginException, IOException {
		desDataInputStream in = new desDataInputStream(serverResponse, 0, serverResponse.length, schedule);
		int clientNonceReturned = in.readInt();
		int serverNonce = in.readInt();
		C_Block sessionKey = in.readC_Block();
		in.close();
		
		if (clientNonce + 1 != clientNonceReturned) {
			// Other end doesn't agree on the current retrievePassword
			throw new LoginException("Incorrect password");
		}

		return new LoginComplete(clientNonce, serverNonce, sessionKey);
	}
	
}
