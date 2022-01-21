/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

/**
 *
 * @author virtu
 */
public abstract class Task extends Thread {
    protected final Thread thread;
    private final String threadName;

    protected abstract void initialization();
    protected abstract void mainTask();
    protected abstract void finished();
    
    @Override
    public final void run() {
        initialization();
        mainTask();
        //System.err.println("Thread " +  threadName + " exiting...");
        finished();
        //System.err.println("Thread " +  threadName + " closed.");
    }
   
    @Override
    public final void start () {
        //System.err.println("Thread " +  threadName + " starting.");
        thread.start ();
    }

    public Task(String threadName) {
        this.threadName = threadName;
        thread = new Thread (this, threadName);
    }
            
}
