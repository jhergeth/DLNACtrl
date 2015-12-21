package mctrl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

	final public static String version = "0.2.1";
	static DLNACtrl dlnaCtrl = null;
	static Future<?> fMain;
	public static Logger jlog =  Logger.getLogger("name.hergeth.upnp");


	volatile int curBrowse = 0;
	int curMedia = 0;

	private static ExecutorService executor = null;

	public static ExecutorService getExecutor(){
		return executor;
	}
	
/**
 * Runs a simple UPnP discovery procedure.
 */
    public static void main(final String[] args) throws Exception {
    	executor = Executors.newFixedThreadPool(1);
    	Future<?> f = executor.submit(() -> {
        	dlnaCtrl = new DLNACtrl();
        	try {
        		dlnaCtrl.runIt(args);

                // Let's wait 10 seconds for them to respond
                jlog.log(Level.INFO, "Waiting 10 seconds before shutting down...");
        		try {
        			Thread.sleep(10000);
        		} catch (InterruptedException e) {
        		}

                dlnaCtrl.searchUDAServiceType("ContentDirectory");
                dlnaCtrl.searchUDAServiceType("AVTransport");

        		dlnaCtrl.waitForRenderer();

        		for( int i = 0; i < 100; i++ )
        			dlnaCtrl.renderPlaylist();
                
        		dlnaCtrl.shutDown();
        		shutDown();
        		
    		}catch( org.fourthline.cling.transport.RouterException re){
        	    jlog.log(Level.SEVERE, "RouterException...");
    		}
        	catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	});
    	f.get();
    	
    	// Shutdown executors
    	try {
    	    jlog.log(Level.INFO, "attempt to shutdown executor");
    	    executor.shutdown();
    	    executor.awaitTermination(11, TimeUnit.SECONDS);
    	}
    	catch (InterruptedException e) {
    	    jlog.log(Level.SEVERE, "tasks interrupted");
    	}
    	finally {
    	    if (!executor.isTerminated()) {
    	        jlog.log(Level.SEVERE, "cancel non-finished tasks");
    	    }
    	    executor.shutdownNow();
    	    jlog.log(Level.INFO, "shutdown finished");
    	}
    }
    
    public static DLNACtrl startIt(){
    	String[] args = null;
    	
	    jlog.log(Level.SEVERE, "Staring DLNACtrl version: " + version);
    	// Shutdown executors
    	try {
        	executor = Executors.newFixedThreadPool(1);
        	fMain = executor.submit(() -> {
            	dlnaCtrl = new DLNACtrl();
            	try {
        			dlnaCtrl.runIt(args);
        		}catch( org.fourthline.cling.transport.RouterException re){
            	    jlog.log(Level.SEVERE, "RouterException...");
        		}
            	catch (Exception e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
        	});
    	}
    	catch (Exception e) {
    	    jlog.log(Level.SEVERE, "tasks interrupted");
    	    jlog.log(Level.SEVERE, e.getMessage());
    	}
    	try {
			fMain.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return dlnaCtrl;
    }
    
	public static void shutDown() {

        try {
			fMain.get();

			jlog.log(Level.INFO, "attempt to shutdown executor");
			executor.shutdown();
			executor.awaitTermination(11, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	finally {
    	    if (!executor.isTerminated()) {
    	        jlog.log(Level.SEVERE, "cancel non-finished tasks");
    	    }
    	    executor.shutdownNow();
    	    jlog.log(Level.INFO, "shutdown finished");
    	}
	}


}
