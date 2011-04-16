package org.webseer.visitor;

import java.util.LinkedList;
import java.util.Queue;

import name.levering.ryan.util.Pair;

public class ThreadedSuperVisitor extends AbstractStoppableVisitor implements SuperVisitor {

    private final Queue<Pair<ToCall, Object>> callQueue = new LinkedList<Pair<ToCall, Object>>();

    private final SuperVisitor visitor;

    public ThreadedSuperVisitor(SuperVisitor visitor) {
        this.visitor = visitor;
    }

    public void postTraverse(Object model) {
        synchronized (this.callQueue) {
            this.callQueue.add(new Pair<ToCall, Object>(ToCall.POST_TRAVERSE, model));
            this.callQueue.notify();
        }
    }

    public void postVisit(Object model) {
        synchronized (this.callQueue) {
            this.callQueue.add(new Pair<ToCall, Object>(ToCall.POST_VISIT, model));
            this.callQueue.notify();
        }
    }

    public void preTraverse(Object model) {
        synchronized (this.callQueue) {
            this.callQueue.add(new Pair<ToCall, Object>(ToCall.PRE_TRAVERSE, model));
            this.callQueue.notify();
        }
    }

    public void preVisit(Object model) {
        synchronized (this.callQueue) {
            this.callQueue.add(new Pair<ToCall, Object>(ToCall.PRE_VISIT, model));
            this.callQueue.notify();
        }
    }

    /**
     * This thread will sequentially go through the call queue and execute the visits.
     */
    public void run() {
        while (true) {
            Pair<ToCall, Object> methodCall;
            synchronized (this.callQueue) {
                methodCall = this.callQueue.poll();
                if (methodCall == null) {
                    try {
                        this.callQueue.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            if (methodCall != null) {
                switch (methodCall.getFirst()) {
                case POST_TRAVERSE:
                    this.visitor.postTraverse(methodCall.getSecond());
                    break;
                case POST_VISIT:
                    this.visitor.postVisit(methodCall.getSecond());
                    break;
                case PRE_TRAVERSE:
                    this.visitor.preTraverse(methodCall.getSecond());
                    break;
                case PRE_VISIT:
                    this.visitor.preVisit(methodCall.getSecond());
                    break;
                case VISIT:
                    this.visitor.visit(methodCall.getSecond());
                    break;
                }
            }
        }
    }

    public void visit(Object model) {
        synchronized (this.callQueue) {
            this.callQueue.add(new Pair<ToCall, Object>(ToCall.VISIT, model));
            this.callQueue.notify();
        }
    }

    private enum ToCall {
        POST_TRAVERSE, PRE_TRAVERSE, POST_VISIT, PRE_VISIT, VISIT;
    }

}
