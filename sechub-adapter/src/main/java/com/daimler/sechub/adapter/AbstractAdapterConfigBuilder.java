// SPDX-License-Identifier: MIT
package com.daimler.sechub.adapter;

import java.net.InetAddress;
import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.crypto.SealedObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daimler.sechub.adapter.support.URIShrinkSupport;

public abstract class AbstractAdapterConfigBuilder<B extends AbstractAdapterConfigBuilder<B, C>, C extends AdapterConfig> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractAdapterConfigBuilder.class);

	private static final int MAX_5_DAYS_IN_MINUTES = 5 * 24 * 60;
	private static final int MAX_1_HOUR_IN_MINUTES = 60;

	private static final int DEFAULT_SCAN_RESULT_TIMEOUT_IN_MINUTES=MAX_5_DAYS_IN_MINUTES;
	private static final int DEFAULT_SCAN_RESULT_CHECK_IN_MINUTES=1;

	public static final String DOCUMENT_INFO_TIMEOUT="Time in minutes when adapter result check will be canceled/time out. When -1 timeout is "+AbstractAdapterConfigBuilder.DEFAULT_SCAN_RESULT_TIMEOUT_IN_MINUTES+" minutes";
	public static final String DOCUMENT_INFO_CHECK="Time in minutes when adapter check operation is called next. When -1 value is "+AbstractAdapterConfigBuilder.DEFAULT_SCAN_RESULT_CHECK_IN_MINUTES+" minutes";
	public static final String DOCUMENT_INFO_TRUSTALL="Turns off certification checks for this product only. Should only be used in test or development environments!";


	private String traceID;
	private String user;
	private SealedObject apiToken;
	private String productBaseURL;

	private int timeToWaitForNextCheckOperationInMinutes = DEFAULT_SCAN_RESULT_CHECK_IN_MINUTES;// one minute check default
	private int scanResultTimeOutInMinutes = DEFAULT_SCAN_RESULT_TIMEOUT_IN_MINUTES; // 5 days default

	private String proxyHostname;

	private int proxyPort;

	private SealedObject password;

	private String policyID;

	private LinkedHashSet<URI> targetURIs = new LinkedHashSet<>();

	private LinkedHashSet<InetAddress> targetIPs = new LinkedHashSet<>();

	private boolean trustAllCertificatesEnabled;

	private URIShrinkSupport uriShrinker;

	private String projectId;


	protected AbstractAdapterConfigBuilder() {
		uriShrinker = createURIShrinker();
	}

	/**
	 * Hook for tests
	 * @return new shrinker, never <code>null</code>
	 */
	protected URIShrinkSupport createURIShrinker() {
		return new URIShrinkSupport();
	}

	/**
	 * Configure this builder by given strategy
	 * @param strategy
	 * @return builder (configured by strategy)
	 */
	@SuppressWarnings("unchecked")
	public final B configure(AdapterConfigurationStrategy strategy) {
		strategy.configure((B) this);
		return (B) this;
	}

	/**
	 * Set result check interval in milliseconds.
	 *
	 * @param minutes
	 *            when <0 the setting will be ignored and default value used!"
	 *            see {@link #DOCUMENT_INFO_CHECK}
	 * @return builder
	 */
	@SuppressWarnings("unchecked")
	public final B setTimeToWaitForNextCheckOperationInMinutes(int minutes) {
		if (minutes < 0) {
			return (B) this;
		}
		this.timeToWaitForNextCheckOperationInMinutes = minutes;
		return (B) this;
	}

	/**
	 * Set result check timeout in minutes.
	 *
	 * @param scanResultTimeOutInMinutes
	 *            when <0 the setting will be ignored and default value used!"
	 *             see {@link #DOCUMENT_INFO_TIMEOUT}
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final B setScanResultTimeOutInMinutes(int scanResultTimeOutInMinutes) {
		if (scanResultTimeOutInMinutes < 0) {
			return (B) this;
		}
		this.scanResultTimeOutInMinutes = scanResultTimeOutInMinutes;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public final B setUser(String userID) {
		this.user = userID;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public final B setPassword(String password) {
		this.password=encrypt(password);
		return (B) this;
	}

	protected SealedObject encrypt(String password) {
		return CryptoAccess.CRYPTO_STRING.seal(password);
	}

	@SuppressWarnings("unchecked")
	public final B setApiToken(String apiToken) {
		this.apiToken = encrypt(apiToken);
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public final B setTraceID(String traceID) {
		this.traceID = traceID;
		return (B) this;
	}

	/**
	 * Set the base url for the product - e.g. "https://x.y.z:8123"
	 * @param baseURL
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final B setProductBaseUrl(String baseURL) {
		this.productBaseURL = baseURL;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public final B setProxyHostname(String hostname) {
		this.proxyHostname = hostname;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public final B setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public final B setPolicyID(String policyID) {
		this.policyID = policyID;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B setTargetURIs(Set<URI> targetURIs) {
		if (targetURIs==null) {
			this.targetURIs= new LinkedHashSet<>();
		}else {
			this.targetURIs= new LinkedHashSet<>();
			this.targetURIs.addAll(targetURIs);
		}
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B setTargetURI(URI targetURI) {
		if (targetURI==null) {
			return (B) this;
		}
		return setTargetURIs(Collections.singleton(targetURI));
	}

	@SuppressWarnings("unchecked")
	public B setTargetIPs(Set<InetAddress> targetIPs) {
		if (targetIPs==null) {
			this.targetIPs= new LinkedHashSet<>();
		}else {
			this.targetIPs= new LinkedHashSet<>();
			this.targetIPs.addAll(targetIPs);
		}
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B setProjectId(String projectId) {
		this.projectId=projectId;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B setTargetIP(InetAddress ipAdress) {
		if (ipAdress==null) {
			return (B) this;
		}
		return setTargetIPs(Collections.singleton(ipAdress));
	}

	/**
	 * Be aware when using this - this highly insecure and
	 * should only be used for development. Enabling this,
	 * the used adapter will accept all kind of certificates!
	 * @param trustAllCertificates
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public B setTrustAllCertificates(boolean trustAllCertificates) {
		this.trustAllCertificatesEnabled=trustAllCertificates;
		return (B) this;
	}

	public final C build() {
		validate();
		ensureTimeSetup();
		ensureTraceID();

		C config = buildInitialConfig();
		if (!( config instanceof AbstractAdapterConfig)) {
			throw new IllegalStateException(getClass().getName()+" does not return a child of AbstractAdapterConfig!");
		}
		AbstractAdapterConfig abstractAdapterConfig = (AbstractAdapterConfig) config;
		abstractAdapterConfig.productBaseURL = productBaseURL;
		abstractAdapterConfig.apiToken=apiToken;

		abstractAdapterConfig.timeToWaitForNextCheckOperationInMilliseconds = timeToWaitForNextCheckOperationInMinutes * 60 * 1000;
		abstractAdapterConfig.timeOutInMilliseconds = scanResultTimeOutInMinutes * 60 * 1000;

		abstractAdapterConfig.proxyHostname = proxyHostname;
		abstractAdapterConfig.proxyPort = proxyPort;
		abstractAdapterConfig.user = user;
		abstractAdapterConfig.trustAllCertificatesEnabled=trustAllCertificatesEnabled;
		abstractAdapterConfig.password = password;
		abstractAdapterConfig.policyId = policyID;
		abstractAdapterConfig.targetURIs = targetURIs;
		abstractAdapterConfig.rootTargetUris.addAll(uriShrinker.shrinkToRootURIs(targetURIs));
		abstractAdapterConfig.targetIPs = targetIPs;

		abstractAdapterConfig.traceID = traceID;
		abstractAdapterConfig.projectId=projectId;

		packageInternalCustomBuild(config);
		customBuild(config);

		return config;
	}

	/**
	 * Customization method - is package private, so can only be changed inside
	 * main adapter package
	 * @param config
	 */
	void packageInternalCustomBuild(C config) {
		/* per default do nothing*/
	}

	protected abstract void customBuild(C config);

	protected abstract C buildInitialConfig();

	private void ensureTraceID() {
		if (traceID == null) {
			traceID = "FALLBACK_TRACE_ID#" + System.nanoTime();
		}
	}

	private void ensureTimeSetup() {
		if (timeToWaitForNextCheckOperationInMinutes > MAX_1_HOUR_IN_MINUTES) {
			LOG.warn(
					"Check interval {} bigger than 1 hour. Automatic reset to one hour done. Please check your configuration!", timeToWaitForNextCheckOperationInMinutes);
			timeToWaitForNextCheckOperationInMinutes = MAX_1_HOUR_IN_MINUTES;
		}
		if (scanResultTimeOutInMinutes > MAX_5_DAYS_IN_MINUTES) {
			LOG.warn(
					"Scan check timeout {} bigger than 5 days. Automatic reset to 5 days done. Please check your configuration!", scanResultTimeOutInMinutes);
			scanResultTimeOutInMinutes = MAX_5_DAYS_IN_MINUTES;
		}

	}

	private void validate() {
		assertProxyPortSetWhenProxyHostnameDefined();
		customValidate();
	}

	/**
	 * Custom validation. Use assert methods defined in
	 * {@link AbstractAdapterConfigBuilder} or use your own for new fields. <br>
	 * <br>
	 * Already initial checked before are:
	 * <ul>
	 * <li>when a proxy hostname is set the proxy port must be set too</li>
	 * <li>product base url set</li>
	 * </ul>
	 */
	protected abstract void customValidate();

	protected void assertUserSet() {
		if (user == null) {
			throw new IllegalStateException("no user given");
		}
	}

	public void assertProjectIdSet() {
		if (projectId == null) {
			throw new IllegalStateException("no projectId given");
		}
	}

	protected void assertPasswordSet() {
		if (password == null) {
			throw new IllegalStateException("no password given");
		}
	}

	protected void assertPolicyIdSet() {
		if (policyID == null) {
			throw new IllegalStateException("no policyID given");
		}
	}

	protected void assertProductBaseURLSet() {
		if (productBaseURL == null || productBaseURL.isEmpty()) {
			throw new IllegalStateException("no product base url given");
		}
	}

	protected void assertProxyPortSetWhenProxyHostnameDefined() {
		if (isProxyDefinedButPortMissing()) {
			throw new IllegalStateException("Proxy set, but no port!");
		}
	}

	protected void assertAPITokenSet() {
		if (apiToken == null) {
			throw new IllegalStateException("no apiToken given");
		}
	}

	private boolean isProxyDefinedButPortMissing() {
		return proxyHostname != null && !proxyHostname.isEmpty() && proxyPort == 0;
	}

}