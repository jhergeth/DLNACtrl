package name.hergeth.dlna.core;

import java.util.HashMap;
import java.util.Map;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.support.model.TransportState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportStateCallback extends SubscriptionCallback {
	private static Map<Service, TransportStateCallback> attached = new HashMap<Service, TransportStateCallback>();
	private static DLNACtrl ctrl = null;

	private TransportState theState = TransportState.TRANSITIONING;
	private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");

	public TransportState getTheState() {
		return theState;
	}

	private TransportStateCallback(Service s, int t) {
		super(s, t);
	}

	public static TransportStateCallback add(ControlPoint ctrl, Service s, int timeout) {
		if (!attached.containsKey(s)) {
			TransportStateCallback tsc = new TransportStateCallback(s, timeout);
			ctrl.execute(tsc);
			attached.put(s, tsc);
			return tsc;
		}
		return null;
	}

	@Override
	public void established(GENASubscription sub) {
		jlog.info("Established: " + sub.getSubscriptionId());
	}

	@Override
	protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
			String defaultMsg) {
		jlog.warn(defaultMsg);
	}

	@Override
	public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
		// assertNull(reason);
	}

	@Override
	public void eventReceived(GENASubscription sub) {
		try {
			// jlog.info("Event: " +
			// sub.getCurrentSequence().getValue());
			//
			Map<String, StateVariableValue> values = sub.getCurrentValues();
			for (String k : values.keySet()) {
				jlog.info("Event: " + k + "=" + values.get(k));
			}

			// StateVariableValue<Service> lc = values.get("LastChange");
			// if( lc != null ){
			// LastChange lastChange = new LastChange(
			// new AVTransportLastChangeParser(),
			// lc.toString() );
			// TransportState ts = lastChange.getEventedValue( 0,
			// AVTransportVariable.TransportState.class).getValue();
			// theState = ts;
			// }

		} catch (IllegalArgumentException ax) {
			// do nothing
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
		jlog.info("Missed events: " + numberOfMissedEvents);
	}

}
