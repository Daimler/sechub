// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.product;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import com.daimler.sechub.sharedkernel.UUIDTraceLogID;
import com.daimler.sechub.sharedkernel.configuration.SecHubConfiguration;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionContext;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionException;

public class AbstractProductExecutionServiceTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

	private static final ProductIdentifier USED_PRODUCT_IDENTIFIER = ProductIdentifier.FARRADAY;
	private AbstractProductExecutionService serviceToTest;
	private UUIDTraceLogID traceLogID;
	private SecHubExecutionContext context;
	private ProductExecutor executor;
	private List<ProductExecutor> executors;
	private ProductResultRepository productResultRepository;
	private Logger logger;
	private UUID sechubJobUUID;

    private ProductExecutorContextFactory productExecutorContextFactory;
    private ProductExecutorContext productExecutorContext;

	@Before
	public void before() throws Exception {
		SecHubConfiguration configuration = new SecHubConfiguration();
		configuration.setProjectId("projectid1");

		sechubJobUUID = UUID.randomUUID();
		logger=mock(Logger.class);
		traceLogID=mock(UUIDTraceLogID.class);

		serviceToTest = new TestImplAbstractProductExecutionService();
		executors = new ArrayList<>();
		executor = mock(ProductExecutor.class);
		when(executor.getIdentifier()).thenReturn(USED_PRODUCT_IDENTIFIER);

		executors.add(executor);
		context = mock(SecHubExecutionContext.class);
		when(context.getSechubJobUUID()).thenReturn(sechubJobUUID);
		when(context.getConfiguration()).thenReturn(configuration);

		productResultRepository=mock(ProductResultRepository.class);
		serviceToTest.productResultRepository=productResultRepository;
		
		productExecutorContextFactory=mock(ProductExecutorContextFactory.class);
		serviceToTest.productExecutorContextFactory=productExecutorContextFactory;
		
		productExecutorContext= mock(ProductExecutorContext.class);
		when(productExecutorContextFactory.create(any(), any(), any())).thenReturn(productExecutorContext);
	}

	@Test
	public void executeAndPersistResults_a_null_result_throws_no_error_but_does_error_logging() throws Exception{
		/* prepare */
		when(executor.execute(eq(context),any())).thenReturn(null);

		/* execute */
		serviceToTest.executeAndPersistResults(executors, context, traceLogID);

		/* test */
		verify(productResultRepository, never()).save(any());
		verify(logger).error(any(), eq(USED_PRODUCT_IDENTIFIER), eq(traceLogID));
	}

	@Test
	public void executeAndPersistResults_a_non_null_result_saves_the_result_no_error_logging() throws Exception{
		ProductResult result = mock(ProductResult.class);
		ArgumentCaptor<ProductExecutorContext> executorContext = ArgumentCaptor.forClass(ProductExecutorContext.class); 
		
		/* prepare */
		when(executor.execute(eq(context),executorContext.capture())).thenReturn(Collections.singletonList(result));

		/* execute */
		serviceToTest.executeAndPersistResults(executors, context, traceLogID);

		/* test */
		verify(productResultRepository).findProductResults(sechubJobUUID,USED_PRODUCT_IDENTIFIER);
		verify(productExecutorContext).persist(result);
		verify(logger,never()).error(any(), eq(USED_PRODUCT_IDENTIFIER), eq(traceLogID));

	}

	@Test
	public void sechub_execution_error_on_execution_shall_not_break_the_build_but_safe_fallbackresult() throws Exception{
		ArgumentCaptor<ProductResult> productResultCaptor = ArgumentCaptor.forClass(ProductResult.class);
		/* prepare */
		SecHubExecutionException exception = new SecHubExecutionException("an-error occurred on execution, but this should not break at all!");
		when(executor.execute(context,productExecutorContext)).thenThrow(exception);

		/* execute */
		serviceToTest.executeAndPersistResults(executors, context, traceLogID);

		/* test */
		verify(productResultRepository).findProductResults(sechubJobUUID,USED_PRODUCT_IDENTIFIER);
		verify(productExecutorContext).persist(productResultCaptor.capture());

		ProductResult captured = productResultCaptor.getValue();
		assertEquals(USED_PRODUCT_IDENTIFIER, captured.getProductIdentifier());
		assertEquals("", captured.getResult());

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		verify(logger).error(stringCaptor.capture(), eq(USED_PRODUCT_IDENTIFIER), eq(traceLogID), eq(exception));
		assertTrue(stringCaptor.getValue().startsWith("Product executor failed"));

	}

	@Test
	public void runtime__error_on_execution_shall_not_break_the_build() throws Exception{
		ArgumentCaptor<ProductResult> productResultCaptor = ArgumentCaptor.forClass(ProductResult.class);
		/* prepare */
		RuntimeException exception = new RuntimeException("an-error occurred on execution, but this should not break at all!");
		when(executor.execute(context,productExecutorContext)).thenThrow(exception);

		/* execute */
		serviceToTest.executeAndPersistResults(executors, context, traceLogID);

		/* test */
		verify(productExecutorContext).persist(productResultCaptor.capture());

		ProductResult captured = productResultCaptor.getValue();
		assertEquals(USED_PRODUCT_IDENTIFIER, captured.getProductIdentifier());
		assertEquals("", captured.getResult());

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		verify(logger).error(stringCaptor.capture(),eq(USED_PRODUCT_IDENTIFIER),eq(traceLogID), eq(exception));
		assertTrue(stringCaptor.getValue().startsWith("Product executor failed:"));

	}

	@Test
	public void runtime_errors_in_persistence_shall_break_the_build() throws Exception{
		/* test */
		expected.expect(RuntimeException.class);

		ProductResult result = mock(ProductResult.class);
		/* prepare */
		when(executor.execute(context,productExecutorContext)).thenReturn(Collections.singletonList(result));
		doThrow(new RuntimeException("save-failed")).when(productExecutorContext).persist(result);

		/* execute */
		serviceToTest.executeAndPersistResults(executors, context, traceLogID);

	}

	private class TestImplAbstractProductExecutionService extends AbstractProductExecutionService{

		@Override
		protected boolean isExecutionNecessary(SecHubExecutionContext context, UUIDTraceLogID traceLogID,
				SecHubConfiguration configuration) {
			return true;
		}

		@Override
		Logger getMockableLog() {
			return logger;
		}

	}
}
