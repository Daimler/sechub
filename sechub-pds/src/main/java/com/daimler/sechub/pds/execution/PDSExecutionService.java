package com.daimler.sechub.pds.execution;

import static com.daimler.sechub.pds.util.PDSAssert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.daimler.sechub.pds.job.PDSJob;
import com.daimler.sechub.pds.job.PDSJobRepository;
import com.daimler.sechub.pds.job.PDSJobStatusState;
import com.daimler.sechub.pds.usecase.UseCaseAdminFetchesExecutionStatus;
import com.daimler.sechub.pds.usecase.UseCaseUserCancelsJob;

/**
 * This class is responsible for all execution queuing parts - it will also know
 * what currently is happening, which job is started, executed etc. But will
 * make no changes to database.<br>
 * <br>
 * 
 * <u>Details:</u><br>
 * A defined thread pool is used for execution queuing, an overload of the queue
 * must be checked by callers via {@link #isQueueFull()}. Execution itself is
 * done inside {@link PDSExecutionCallable} - execution state checks and changes
 * to database are done inside {@link PDSExecutionWatcher}
 * 
 * @author Albert Tregnaghi
 *
 */
@Service
public class PDSExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(PDSExecutionService.class);
    private static final int DEFAULT_WORKER_THREAD_COUNT = 5;
    private static final int DEFAULT_QUEUE_MAX = 50;
    ExecutorService workers;

    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final PDSExecutionWatcher watcher = new PDSExecutionWatcher();
    private final Map<UUID, Future<PDSExecutionResult>> jobsInQueue = new LinkedHashMap<>();

    @Value("${sechub.pds.config.execute.worker.thread.count:" + DEFAULT_WORKER_THREAD_COUNT + "}")
    int workerThreadCount = DEFAULT_WORKER_THREAD_COUNT;

    @Value("${sechub.pds.config.execute.queue.max:" + DEFAULT_QUEUE_MAX + "}")
    int queueMax = DEFAULT_QUEUE_MAX;

    /* only for tests to turn off watcher */
    boolean watcherDisabled;

    @Autowired
    PDSExecutionCallableFactory executionCallableFactory;

    @Autowired
    PDSJobRepository repository;

    @PostConstruct
    protected void postConstruct() {
        workers = Executors.newFixedThreadPool(workerThreadCount);

        scheduler.scheduleAtFixedRate(watcher, 300, 1000, TimeUnit.MILLISECONDS);
    }

    @UseCaseUserCancelsJob
    public boolean cancel(UUID jobUUID) {
        notNull(jobUUID, "job uuid may not be null!");

        synchronized (jobsInQueue) {

            Iterator<Entry<UUID, Future<PDSExecutionResult>>> it = jobsInQueue.entrySet().iterator();
            while (it.hasNext()) {
                Entry<UUID, Future<PDSExecutionResult>> entry = it.next();
                if (jobUUID.equals(entry.getKey())) {
                    Future<PDSExecutionResult> future = entry.getValue();
                    if (future.isDone()) {
                        /* already done or canceled */
                        LOG.info("cancelation of job with uuid:{} skipped, because already done",jobUUID);
                        return false;
                    }
                    boolean canceled = future.cancel(true);
                    if (canceled) {
                        LOG.info("canceled job with uuid:{}",jobUUID);
                    }else {
                        LOG.warn("cancelation of not done job with uuid:{} returned false - this should not happen");
                    }
                    return canceled;
                }
            }
            /*
             * job not found - either never existed or already canceled/done and removed by
             * watcher
             */
            return false;
        }
    }

    public boolean isQueueFull() {
        synchronized (jobsInQueue) {
            return jobsInQueue.size() >= queueMax;
        }
    }

    public void addToExecutionQueue(PDSJob pdsJob) {
        UUID jobUUID = pdsJob.getUUID();
        Future<?> former = null;
        synchronized (jobsInQueue) {
            LOG.debug("add job to execution queue:{}", jobUUID);
            int size = jobsInQueue.size();
            if (size >= queueMax) {
                LOG.warn("execution queue overload:{}/{}", size, queueMax);
            }
            pdsJob.setState(PDSJobStatusState.QUEUED);
            repository.save(pdsJob);

            PDSExecutionFutureTask task = new PDSExecutionFutureTask(executionCallableFactory.createCallable(pdsJob));
            workers.execute(task);
            
            former = jobsInQueue.put(jobUUID, task);
        }
        handleFormerJob(jobUUID, former);
    }

    @UseCaseAdminFetchesExecutionStatus
    public PDSExecutionStatus getExecutionStatus() {
        PDSExecutionStatus status = new PDSExecutionStatus();
        synchronized (jobsInQueue) {
            status.queueMax = queueMax;
            status.jobsInQueue = jobsInQueue.size();

            Iterator<Entry<UUID, Future<PDSExecutionResult>>> it = jobsInQueue.entrySet().iterator();
            while (it.hasNext()) {
                Entry<UUID, Future<PDSExecutionResult>> entry = it.next();
                Future<PDSExecutionResult> future = entry.getValue();
                PDSExecutionJobInQueueStatusEntry statusEntry = new PDSExecutionJobInQueueStatusEntry();
                statusEntry.done = future.isDone();
                statusEntry.canceled = future.isCancelled();
                statusEntry.jobUUID=entry.getKey();

                Optional<PDSJob> jobOption = repository.findById(entry.getKey());
                if (jobOption.isPresent()) {
                    statusEntry.job = jobOption.get();
                }
                status.entries.add(statusEntry);
            }
        }
        return status;
    }

    private void handleFormerJob(UUID jobUUID, Future<?> former) {
        if (former == null) {
            return;
        }
        LOG.error("Did found former job in queue with same job UUID:{}", jobUUID);
        former.cancel(true);
        LOG.info("Canceled former job {}", jobUUID);

    }

    private class PDSExecutionWatcher implements Runnable {

        @Override
        public void run() {
            if (watcherDisabled) {
                LOG.warn("Execution watcher disabled");
                return;
            }
            inspectJobsInQueue();
        }

        private void inspectJobsInQueue() {
            synchronized (jobsInQueue) {
                List<UUID> doneAndDatabaseChangesApplied = new ArrayList<>(0);
                Iterator<Entry<UUID, Future<PDSExecutionResult>>> it = jobsInQueue.entrySet().iterator();

                while (it.hasNext()) {

                    Entry<UUID, Future<PDSExecutionResult>> entry = it.next();
                    Future<PDSExecutionResult> future = entry.getValue();

                    if (future.isDone()) {
                        if (isFutureDoneAndChangesToDatabaseCanBeApplied(entry, future)) {
                            doneAndDatabaseChangesApplied.add(entry.getKey());
                        }
                    }
                }

                for (UUID uuid : doneAndDatabaseChangesApplied) {
                    jobsInQueue.remove(uuid);
                }
            }
        }

        /**
         * Handles work being done - all done parts are marked automatically in database
         * 
         * @param entry
         * @param future
         * @return <code>true</code> when work can be removed from jobsInQueue
         */
        private boolean isFutureDoneAndChangesToDatabaseCanBeApplied(Entry<UUID, Future<PDSExecutionResult>> entry, Future<PDSExecutionResult> future) {
            UUID jobUUID = entry.getKey();

            Optional<PDSJob> jobOption = repository.findById(jobUUID);
            if (!jobOption.isPresent()) {
                LOG.error("job with uuid:{} does no longer exist, but result available! So remove from queue", jobUUID);
                return true;
            }

            try {
                PDSJob job = jobOption.get();
                if (future.isCancelled()) {
                    job.setState(PDSJobStatusState.CANCELED);
                } else {
                    PDSExecutionResult callResult;
                    try {
                        callResult = future.get();
                        job.setResult(callResult.result);
                        if (callResult.failed) {
                            job.setState(PDSJobStatusState.FAILED);
                        }else {
                            job.setState(PDSJobStatusState.DONE);
                        }
                    } catch (InterruptedException e) {
                        LOG.error("Job with uuid:{} was interrupted", jobUUID,e);
                        job.setState(PDSJobStatusState.FAILED);
                        job.setResult("Job interrupted");
                    } catch (ExecutionException e) {
                        LOG.error("Job with uuid:{} failed in execution", jobUUID,e);
                        job.setState(PDSJobStatusState.FAILED);
                        job.setResult("Job execution failed");
                    }
                }
                repository.save(job);

                return true;

            } catch (Exception e) {
                LOG.error("Was not able to handle work for job with uuid:{}", jobUUID, e);
                return false;
            }

        }

    }

    void destroy() {
        // TODO Auto-generated method stub
        
    }

}
