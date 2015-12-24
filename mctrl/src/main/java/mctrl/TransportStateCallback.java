package mctrl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.TransportState;

public class TransportStateCallback extends SubscriptionCallback
{
	static Map<Service,TransportStateCallback> attached = new HashMap<Service,TransportStateCallback>();
	
	
	private TransportState theState = TransportState.TRANSITIONING;

	public TransportState getTheState() {
		return theState;
	}

	private TransportStateCallback(Service s, int t){
		super(s, t);
	}
	
	public static void add( Service s, int timeout){
		if(!attached.containsKey(s)){
			TransportStateCallback tsc = new TransportStateCallback(s, timeout);
			DLNACtrl.execAction(tsc);
			attached.put(s,  tsc);
		}
		
	}

	@Override
	public void established(GENASubscription sub) {
		Main.jlog.log(Level.INFO, "Established: " + sub.getSubscriptionId());
	}

	@Override
	protected void failed(GENASubscription subscription,
			UpnpResponse responseStatus,
			Exception exception,
			String defaultMsg) {
		Main.jlog.log(Level.WARNING, defaultMsg);
	}

	@Override
	public void ended(GENASubscription sub,
			CancelReason reason,
			UpnpResponse response) {
		//            assertNull(reason);
	}

	@Override
	public void eventReceived(GENASubscription sub) {
		try {
//			Main.jlog.log(Level.INFO, "Event: " + sub.getCurrentSequence().getValue());
//
			Map<String, StateVariableValue> values = sub.getCurrentValues();
			for( String k : values.keySet()){
				Main.jlog.log(Level.INFO, "Event: " + k + "=" + values.get(k));
			}

//			StateVariableValue<Service> lc = values.get("LastChange");
//			if( lc != null ){
//				LastChange lastChange = new LastChange(
//						new AVTransportLastChangeParser(),
//						lc.toString() );
//				TransportState ts = lastChange.getEventedValue( 0, AVTransportVariable.TransportState.class).getValue();
//				theState  = ts;
//			}

		} catch(IllegalArgumentException ax){
			// do nothing
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
		Main.jlog.log(Level.INFO, "Missed events: " + numberOfMissedEvents);
	}
	
}
