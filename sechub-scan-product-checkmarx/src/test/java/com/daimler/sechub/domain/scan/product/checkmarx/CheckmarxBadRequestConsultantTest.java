package com.daimler.sechub.domain.scan.product.checkmarx;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.daimler.sechub.sharedkernel.resilience.ResilienceContext;
import com.daimler.sechub.sharedkernel.resilience.ResilienceProposal;
import com.daimler.sechub.sharedkernel.resilience.RetryResilienceProposal;

public class CheckmarxBadRequestConsultantTest {

	private CheckmarxBadRequestConsultant consultantToTest;
	private ResilienceContext context;

	@Before
	public void before() {
		consultantToTest = new CheckmarxBadRequestConsultant();
		context= mock(ResilienceContext.class);
	}

	@Test
	public void no_exception_returns_null() {
		/* prepare*/

		/* execute*/
		ResilienceProposal proposal = consultantToTest.consultFor(context);

		/* test*/
		assertNull(proposal);
	}

	@Test
	public void illegal_argument_exception_returns_null() {
		/* prepare*/
		when(context.getCurrentError()).thenReturn(new IllegalArgumentException());

		/* execute*/
		ResilienceProposal proposal = consultantToTest.consultFor(context);

		/* test*/
		assertNull(proposal);
	}

	@Test
	public void http_bad_request_400_exception_returns_retry_proposal_with_3_retries_and_2000_millis_to_wait() {
		/* prepare*/
		when(context.getCurrentError()).thenReturn(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

		/* execute*/
		ResilienceProposal proposal = consultantToTest.consultFor(context);

		/* test*/
		assertNotNull(proposal);
		assertTrue(proposal instanceof RetryResilienceProposal);
		RetryResilienceProposal rrp = (RetryResilienceProposal) proposal;
		assertEquals(3, rrp.getMaximumAmountOfRetries());
		assertEquals(2000, rrp.getMillisecondsToWaitBeforeRetry());
	}

}
