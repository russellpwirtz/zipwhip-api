package com.zipwhip.important;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.important.schedulers.HashedWheelScheduler;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 11:53 AM
 */
public class ImportantTaskExecutor extends CascadingDestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(ImportantTaskExecutor.class);

    private final Map<String, Worker> executorMap = Collections.synchronizedMap(new HashMap<String, Worker>());
    private final Map<String, ScheduledRequest> queuedRequests = Collections.synchronizedMap(new HashMap<String, ScheduledRequest>());
    private final Set<String> executingRequests = Collections.synchronizedSet(new HashSet<String>());

    private Scheduler scheduler;

    public ImportantTaskExecutor() {
        this(null);
    }

    public ImportantTaskExecutor(Scheduler scheduler) {
        if (scheduler != null) {
            this.setScheduler(scheduler);
        } else {
            scheduler = new HashedWheelScheduler();
            this.setScheduler(scheduler);
            this.link((HashedWheelScheduler) scheduler);
        }
    }

    public <T extends Serializable, TResponse> ObservableFuture<TResponse> enqueue(ImportantTask<T> request) {
        Worker<T, TResponse> executor = executorMap.get(request.getRequestType());
        ObservableFuture<TResponse> requestFuture = null;

        // TODO: consider a default expiration
        scheduler.schedule(request.getRequestId(), request.getExpirationDate());

        ObservableFuture<TResponse> parentFuture = null;
        ScheduledRequest scheduledRequest = null;
        if (request.getExpirationDate() != null) {
            parentFuture = createObservableFuture();
            scheduledRequest = new ScheduledRequest(parentFuture, request);

            // in case it times out
            queuedRequests.put(request.getRequestId(), scheduledRequest);
        }

        try {
            executingRequests.add(request.getRequestId());
            requestFuture = executor.execute(request.getParameters());
            if (scheduledRequest != null){
                scheduledRequest.setRequestFuture(requestFuture);
            }
        } catch (java.lang.Exception e) {
            requestFuture = new DefaultObservableFuture<TResponse>(this);
            requestFuture.setFailure(e);
            return requestFuture;
        } finally {
            executingRequests.remove(request.getRequestId());
            // check for abortion
            if (parentFuture != null && parentFuture.isDone()) {
                // it finished late.
                LOGGER.error("The parent future is already done? Did it finish late?");
            }
        }

        if (parentFuture != null && requestFuture != null){
            final ObservableFuture<TResponse> finalParentFuture = parentFuture;
            requestFuture.addObserver(new Observer<ObservableFuture<TResponse>>() {
                @Override
                public void notify(Object sender, ObservableFuture<TResponse> item) {
                    if (item.isCancelled()) {
                        finalParentFuture.cancel();
                    } else if (!item.isSuccess()) {
                        finalParentFuture.setFailure(item.getCause());
                    } else if (item.isSuccess()) {
                        finalParentFuture.setSuccess(item.getResult());
                    }
                }
            });
        }

        return parentFuture != null ? parentFuture : requestFuture;
    }

    public void register(String requestType, Worker executor) {
        if (executorMap.containsKey(requestType)) {
            throw new IllegalStateException("Already have an executor for type " + requestType);
        }
        executorMap.put(requestType, executor);
    }

    public void unregister(String requestType) {
        executorMap.remove(requestType);
    }

    private final Observer<String> onTimerScheduleComplete = new Observer<String>() {

        @Override
        public void notify(Object sender, String requestId) {
            synchronized (ImportantTaskExecutor.this) {
                if (isDestroyed()) {
                    return;
                }
            }

            ScheduledRequest scheduledRequest = queuedRequests.get(requestId);
            if (scheduledRequest == null) {
                // must be a reboot of the service.
                return;
            }

            ImportantTask request = scheduledRequest.getRequest();
            Date expirationDate = request.getExpirationDate();
            if (expirationDate != null && nowIsAfterThisDate(expirationDate)) {
                queuedRequests.remove(scheduledRequest.getRequest().getRequestId());

                // was the initial future even finished yet?
                ObservableFuture f = scheduledRequest.getRequestFuture();
                if (f == null){
                    // it took too long to even make the request. Wow.
                    scheduledRequest.getParentFuture().setFailure(new TimeoutException("Too late? " + expirationDate));
                } else {
                    // it just simply timed out normally
                    scheduledRequest.getRequestFuture().setFailure(new TimeoutException("Too late? " + expirationDate));
                }
            }
        }
    };

    private boolean nowIsAfterThisDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("The date can't be null!");
        }

        return System.currentTimeMillis() - date.getTime() < 1000;
    }

    private <TResponse> DefaultObservableFuture<TResponse> createObservableFuture() {
        return new DefaultObservableFuture<TResponse>(this);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        if (this.scheduler != null) {
            this.scheduler.removeOnScheduleComplete(onTimerScheduleComplete);
        }

        this.scheduler = scheduler;

        if (this.scheduler != null) {
            this.scheduler.onScheduleComplete(onTimerScheduleComplete);
        }
    }

    @Override
    protected void onDestroy() {
        executorMap.clear();
        queuedRequests.clear();
        executingRequests.clear();
    }

    /**
     * Created by IntelliJ IDEA.
     * User: Russ
     * Date: 8/29/12
     * Time: 12:09 PM
     */
    public static class ScheduledRequest<TRequest extends Serializable, TResponse> {

        private final ObservableFuture<TResponse> parentFuture;
        private final ImportantTask<TRequest> request;
        private ObservableFuture<TResponse> requestFuture;

        public ScheduledRequest(ObservableFuture<TResponse> parentFuture, ImportantTask<TRequest> request) {
            this.parentFuture = parentFuture;
            this.request = request;
        }

        public ObservableFuture<TResponse> getParentFuture() {
            return parentFuture;
        }

        public ImportantTask<TRequest> getRequest() {
            return request;
        }

        public void setRequestFuture(ObservableFuture<TResponse> requestFuture) {
            this.requestFuture = requestFuture;
        }

        public ObservableFuture<TResponse> getRequestFuture() {
            return requestFuture;
        }
    }
}
