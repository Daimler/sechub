package com.daimler.sechub.sharedkernel.resilience;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is able to execute actions which shall be resilient by help of
 * consultants. The proposals from consultants will be inspected and used for
 * resilient behaviour. <br><br>
 * But be aware: You <b>MUST</b> use one executor always for same kind of action.
 * If you have dedicated actions you need different executors!. An example:
 * When you want to connect to two servers and you want a fallthrough if server1
 * is not available you do not want to have a fallthrough of server2 automatically.
 * So in this case you should use two different executors for these different
 * targets !
 * @author Albert Tregnaghi
 *
 */
public class ResilientActionExecutor<R>{

	private static final Logger LOG = LoggerFactory.getLogger(ResilientActionExecutor.class);

	private List<ResilienceConsultant> consultants = new ArrayList<>();

	private FallthroughSupport fallThroughSupport = new FallthroughSupport();

	public R executeResilient(ActionWhichShallBeResilient<R> action) throws Exception {
		Objects.requireNonNull(action, "action may not be null!");

		fallThroughSupport.handleFallThrough();

		ResilienctActionContext context = new ResilienctActionContext();
		R result = null;
		do {
			/* try to execute */
			try {
				result = action.execute();
			} catch (Exception e) {
				handleException(context, e);
			}

		} while (result == null && context.isRetryNecessary());

		return result;
	}

	private void handleException(ResilienctActionContext context, Exception e) throws Exception, InterruptedException {
		ResilienceProposal proposal = findFirstProposalFromConsultants(context);
		if (proposal == null) {
			throw e;
		}
		context.prepareForNextCheck(e);
		if (proposal instanceof RetryResilienceProposal) {

			RetryResilienceProposal retryProposal = (RetryResilienceProposal) proposal;

			int maxRetries = retryProposal.getMaximumAmountOfRetries();
			LOG.info("Retry {}/{}:{}", context.getAlreadyDoneRetries(), maxRetries, proposal.getInfo());
			if (context.getAlreadyDoneRetries() >= maxRetries) {
				LOG.warn("Maximum retry amount reached, will rethrow exception", context.getAlreadyDoneRetries(), maxRetries);
				throw e;
			} else {
				context.forceRetry();
				/* wait time for next retry */
				Thread.sleep(retryProposal.getMillisecondsToWaitBeforeRetry());
			}
		} else if (proposal instanceof FallthroughResilienceProposal) {
			FallthroughResilienceProposal fallThroughProposal = (FallthroughResilienceProposal) proposal;
			LOG.info("Fall through activated, will rethrow same exception {} milliseconds", fallThroughProposal.getMillisecondsForFallThrough());

			context.forceFallThrough(fallThroughProposal);
			throw e;
		} else {
			LOG.error("Returned propsal is not wellknown and so cannt be handled:{}", proposal.getClass());
			throw e;
		}
	}

	private ResilienceProposal findFirstProposalFromConsultants(ResilienctActionContext context) {
		ResilienceProposal proposal = null;
		for (ResilienceConsultant consultant : consultants) {
			proposal = consultant.consultFor(context);
			if (proposal != null) {
				break;
			}
		}
		return proposal;
	}

	public void add(ResilienceConsultant consultant) {
		Objects.requireNonNull(consultant, "consultant may not be null!");
		consultants.add(consultant);
	}

	class ResilienctActionContext implements ResilienceContext {

		private Exception currentError;
		private int retriesCount;
		private boolean retryNecessary;

		public void prepareForNextCheck(Exception e) {
			this.retryNecessary = false;
			this.currentError = e;
		}

		public void forceFallThrough(FallthroughResilienceProposal proposal) {
			this.retryNecessary = false;
			fallThroughSupport.enable(currentError,proposal.getInfo(),proposal.getMillisecondsForFallThrough());
		}

		public boolean isRetryNecessary() {
			return retryNecessary;
		}

		public void forceRetry() {
			retryNecessary = true;
			retriesCount++;
		}

		public void setCurrentError(Exception currentError) {
			this.currentError = currentError;
		}

		@Override
		public Exception getCurrentError() {
			return currentError;
		}

		@Override
		public int getAlreadyDoneRetries() {
			return retriesCount;
		}

	}

	class FallthroughSupport{
		private static final long NO_FALLTHROUGH = 0;


		private Exception lastError;
		private String infoFallThrough;
		private Object monitorOBject = new Object();
		private long timeFallthroughEnd;

		public void handleFallThrough() throws Exception{
			synchronized (monitorOBject) {
				if (unsafeIsFallThroughActive()) {
					LOG.info("Fall through active for {} milliseconds:{}", timeFallthroughEnd - System.currentTimeMillis(), infoFallThrough);
					throw lastError;
				}
			}
		}

		private boolean unsafeIsFallThroughActive() {
			if (lastError == null) {
				return false;
			}
			if (timeFallthroughEnd == NO_FALLTHROUGH) {
				return false;
			}
			long currentTimeMillis = System.currentTimeMillis();
			long millisUntilFallThroughTimeOut = timeFallthroughEnd - currentTimeMillis;
			if (millisUntilFallThroughTimeOut > 0) {
				return true;
			} else {
				/* reset */
				lastError = null;
				timeFallthroughEnd = NO_FALLTHROUGH;
				return false;
			}
		}

		public void enable(Exception currentError, String info, long millisecondsForFallThrough) {
			synchronized (monitorOBject) {
				this.lastError = currentError;
				this.infoFallThrough = info;
				this.timeFallthroughEnd = System.currentTimeMillis() + millisecondsForFallThrough;
			}

		}

	}

	public boolean containsConsultant(Class<? extends ResilienceConsultant> consultantClass) {
		Objects.requireNonNull(consultantClass);
		if (consultants.isEmpty()) {
			return false;
		}
		for (ResilienceConsultant consultant: consultants) {
			if (consultant.getClass().isAssignableFrom(consultantClass)) {
				return true;
			}
		}
		return false;
	}

}
