package com.cor3.bluetoothpanel;

import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;

/**
 * A method for limiting the rate at which messages are sent over the bluetooth serial connection
 * when generated in response to UI actions (such as dragging a SeekBar.) Each message added to the
 * queue is tagged, and removes any pending messages with the same tag already in the queue. The
 * handle() abstract method is called with each message in the order they were enqueued, with a
 * minimum delay of waitTime seconds. If it has been more than waitTime seconds since the last
 * enqueued message was passed to handle(), then enqueuing a message will call handle() immediately.
 *
 * Internally, handle() is called by way of an inner Handler subclass (actually a static inner class
 * with a weak reference to the outer class instance to prevent leaking context references), so it
 * is safe to update the UI from within handle() (so long as the instance was constructed by the
 * UI thread (for instance in onCreate() of a view or activity, etc.) regardless of which thread is
 * writing to the queue by calling add().
 *
 * The type parameters T and M are the tag value type and the message data type, respectively. The
 * tags are compared using Objects.equals() so it is safe to use Strings (or any other class X with
 * a boolean equals(X) method) as tags.
 *
 * The constructor takes one argument, a float which specifies the minimum delay between messages,
 * expressed in seconds. Internally, this value is converted to milliseconds and cast to an integer
 * so delays of less than 0.001 will be treated as 0 seconds.
 *
 * Created by Victor Condino on 6/28/2016.
 */
public abstract class RateLimitedTaggedQueue<T, M> {
    protected final ArrayList<Pair<T, M>> queue;
    protected final RateLimitedTaggedQueue.RateLimitedTaggedQueueHandler<T, M> handler;
    protected boolean waiting = false;
    protected final float waitTime;

    public RateLimitedTaggedQueue(final float waitTime) {
        super();
        this.waitTime = waitTime;
        this.handler = new RateLimitedTaggedQueue.RateLimitedTaggedQueueHandler<>(this);
        this.queue = new ArrayList<>();
    }

    protected abstract void handle(M msg);

    public void add(T tag, M msg) {
        for(Pair<T, M> i : queue) {
            if(Objects.equals(i.first, tag)) {
                queue.remove(i);
            }
        }
        queue.add(new Pair<T, M>(tag, msg));
        if(!waiting)
        {
            waiting = true;
            handler.sendEmptyMessage(0);
        }
    }

    private static class RateLimitedTaggedQueueHandler<T, M> extends Handler {
        private final WeakReference<RateLimitedTaggedQueue<T, M>> rateLimitedTaggedQueueWeakReference;

        public RateLimitedTaggedQueueHandler(RateLimitedTaggedQueue<T, M> rateLimitedTaggedQueue) {
            super();
            this.rateLimitedTaggedQueueWeakReference = new WeakReference<>(rateLimitedTaggedQueue);
        }

        @Override
        public final void handleMessage(Message msg) {
            RateLimitedTaggedQueue<T, M> rateLimitedTaggedQueue = this.rateLimitedTaggedQueueWeakReference.get();
            if(rateLimitedTaggedQueue != null) {
                if (!rateLimitedTaggedQueue.queue.isEmpty()) {
                    Pair<T, M> next = rateLimitedTaggedQueue.queue.get(0);
                    rateLimitedTaggedQueue.queue.remove(0);
                    rateLimitedTaggedQueue.handle(next.second);
                    rateLimitedTaggedQueue.handler.sendEmptyMessageDelayed(0, (long) (rateLimitedTaggedQueue.waitTime * 1000f));
                } else {
                    rateLimitedTaggedQueue.waiting = false;
                }
            }
            super.handleMessage(msg);
        }
    }
}
