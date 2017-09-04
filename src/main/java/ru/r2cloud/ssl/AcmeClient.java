package ru.r2cloud.ssl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Registration;
import org.shredzone.acme4j.RegistrationBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeRetryAfterException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;

import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.uitl.NamingThreadFactory;
import ru.r2cloud.uitl.Util;

public class AcmeClient {

	private final static Logger LOG = Logger.getLogger(AcmeClient.class.getName());
	private final static long INITIAL_RETRY = 3000L;

	private ScheduledExecutorService executor;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private final Configuration config;
	private final File basepath;
	private final File webrootPath;
	private final List<String> messages = new ArrayList<String>();

	public AcmeClient(Configuration config) {
		this.config = config;
		this.basepath = Util.initDirectory(config.getProperty("acme.basepath"));
		this.webrootPath = Util.initDirectory(config.getProperty("acme.webroot"));
	}

	public void start() {
		executor = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("acme-client"));
		// FIXME check new File(basepath, "domain-chain.crt") exists and
		// schedule re-new
	}

	public void setup() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		messages.clear();
		executor.submit(new Runnable() {

			@Override
			public void run() {
				messages.add("starting up...");

				KeyPair userKeyPair;
				try {
					userKeyPair = loadOrCreateKeyPair(new File(basepath, "user.key"));
				} catch (IOException e) {
					String message = "unable to load user keypair";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				messages.add("creating session");
				Session session = new Session(config.getProperty("acme.ca.url"), userKeyPair);

				Registration reg;
				try {
					reg = findOrRegisterAccount(session);
				} catch (AcmeException e) {
					String message = "unable to setup account";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				String domain = config.getProperty("ddns.noip.domain");
				try {
					authorize(reg, domain);
				} catch (Exception e) {
					String message = "unable to authorize domain";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				KeyPair domainKeyPair;
				try {
					domainKeyPair = loadOrCreateKeyPair(new File(basepath, "domain.key"));
				} catch (IOException e) {
					String message = "unable to load domain keypair";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				messages.add("creating csr");
				CSRBuilder csrb = new CSRBuilder();
				csrb.addDomain(domain);
				try {
					csrb.sign(domainKeyPair);
				} catch (IOException e) {
					String message = "unable to sign csr";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				messages.add("saving csr for future use");
				try (Writer out = new FileWriter(new File(basepath, "domain.csr"))) {
					csrb.write(out);
				} catch (IOException e) {
					String message = "unable to save csr";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				messages.add("requesting certificate");
				Certificate certificate;
				try {
					certificate = reg.requestCertificate(csrb.getEncoded());
				} catch (Exception e) {
					String message = "unable to request certificate";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				messages.add("downloading certificate");
				X509Certificate cert;
				try {
					cert = certificate.download();
				} catch (AcmeException e) {
					String message = "unable to download certificate";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}
				messages.add("downloading certificate chain");
				X509Certificate[] chain;
				try {
					chain = certificate.downloadChain();
				} catch (AcmeException e) {
					String message = "unable to download certificate chain";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}
				messages.add("saving certificate");
				try (FileWriter fw = new FileWriter(new File(basepath, "domain-chain.crt"))) {
					CertificateUtils.writeX509CertificateChain(fw, cert, chain);
				} catch (IOException e) {
					String message = "unable to save certificate";
					messages.add(message);
					LOG.log(Level.SEVERE, message, e);
					return;
				}

				// FIXME clean up authorization

				running.set(false);
			}
		});
	}
	
	public List<String> getMessages() {
		//FIXME sync messages
		return new ArrayList<String>(messages);
	}

	private KeyPair loadOrCreateKeyPair(File file) throws IOException {
		if (file.exists()) {
			messages.add("loading keypair");
			try (FileReader fr = new FileReader(file)) {
				return KeyPairUtils.readKeyPair(fr);
			}
		} else {
			messages.add("creating keypair");
			KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
			try (FileWriter fw = new FileWriter(file)) {
				KeyPairUtils.writeKeyPair(keyPair, fw);
			}
			return keyPair;
		}
	}

	private Registration findOrRegisterAccount(Session session) throws AcmeException {
		Registration reg;
		try {
			messages.add("registering new user");
			reg = new RegistrationBuilder().create(session);
			URI agreement = reg.getAgreement();
			messages.add("accepting terms of service");
			// FIXME put ToS on Web UI
			reg.modify().setAgreement(agreement).commit();
			// FIXME add contact data
		} catch (AcmeConflictException ex) {
			messages.add("account already exists. use it");
			reg = Registration.bind(session, ex.getLocation());
		}
		return reg;
	}

	private void authorize(Registration reg, String domain) throws AcmeException, FileNotFoundException, IOException {
		messages.add("authorizing domain: " + domain);
		Authorization auth = reg.authorizeDomain(domain);

		Challenge challenge = httpChallenge(auth);
		if (challenge.getStatus() == Status.VALID) {
			messages.add("challenge already successeded");
			return;
		}
		messages.add("trigger challenge");
		challenge.trigger();

		// Poll for the challenge to complete.
		try {
			long retryTimeout = INITIAL_RETRY;
			while (challenge.getStatus() != Status.VALID) {
				// Did the authorization fail?
				if (challenge.getStatus() == Status.INVALID) {
					throw new AcmeException("Challenge failed... Giving up.");
				}

				Thread.sleep(retryTimeout);

				try {
					messages.add("update challenge");
					challenge.update();
				} catch (AcmeRetryAfterException e) {
					retryTimeout = e.getRetryAfter().toEpochMilli() - System.currentTimeMillis();
					messages.add("not ready. retry after: " + retryTimeout + " millis");
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		// All reattempts are used up and there is still no valid authorization?
		if (challenge.getStatus() != Status.VALID) {
			throw new AcmeException("Failed to pass the challenge for domain " + domain + ", ... Giving up.");
		}
	}

	public Challenge httpChallenge(Authorization auth) throws AcmeException, FileNotFoundException, IOException {
		messages.add("find http challenge");
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
		if (challenge == null) {
			throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
		}
		messages.add("saving challenge request");
		try (FileOutputStream fos = new FileOutputStream(new File(webrootPath, "/.well-known/acme-challenge/" + challenge.getToken()))) {
			fos.write(challenge.getAuthorization().getBytes(StandardCharsets.UTF_8));
		}
		return challenge;
	}

	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	public boolean isSSLEnabled() {
		return new File(basepath, "domain-chain.crt").exists();
	}

}
