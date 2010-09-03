package com.ferox.input;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventQueue {
    private final ExecutorService executor;
    
    public EventQueue() {
        executor = Executors.newFixedThreadPool(1);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
    
    public void postEvent(Event e) {
        EventDispatcher dispatcher = e.getSource().getDispatcher();
        executor.submit(new EventTask(dispatcher, e));
    }
    
    private static class EventTask implements Runnable {
        private final Event e;
        private final EventDispatcher dispatcher;
        
        public EventTask(EventDispatcher dispatcher, Event e) {
            this.dispatcher = dispatcher;
            this.e = e;
        }
        
        @Override
        public void run() {
            dispatcher.dispatchEvent(e);
        }
    }
}
