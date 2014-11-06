package com.example.myalljoyn;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.PasswordManager;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity {
	/* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }

    private static final int MESSAGE_PING = 1;
    private static final int MESSAGE_PING_REPLY = 2;
    private static final int MESSAGE_POST_TOAST = 3;
    private static final int MESSAGE_START_PROGRESS_DIALOG = 4;
    private static final int MESSAGE_STOP_PROGRESS_DIALOG = 5;

    private static final String TAG = "SimpleClient";
    
    /* Handler used to make calls to AllJoyn methods. See onCreate(). */
    private BusHandler mBusHandler;
    
    private ProgressDialog mDialog;
    private EditText mResultEditText;
    
    private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MESSAGE_PING:
                    String ping = (String) msg.obj;
                    //mListViewArrayAdapter.add("Ping:  " + ping);
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_PING_REPLY:
                    String ret = (String) msg.obj;
                    //Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    mResultEditText.setText((String) msg.obj);
                    break;
                case MESSAGE_POST_TOAST:
                	Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                	break;
                case MESSAGE_START_PROGRESS_DIALOG:
                    mDialog = ProgressDialog.show(MainActivity.this, 
                                                  "", 
                                                  "Finding Led Service.\nPlease wait...", 
                                                  true,
                                                  true);
                    break;
                case MESSAGE_STOP_PROGRESS_DIALOG:
                    mDialog.dismiss();
                    break;
                default:
                    break;
                }
            }
        };
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//mResultEditText = (EditText) findViewById(R.id.Edit_Result);
		
		 /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
        mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onOnButtonClick(View view)
	{
		
		 Message msg = mBusHandler.obtainMessage(BusHandler.ON, 
                 "on");
		 mBusHandler.sendMessage(msg);
		
	}
	
	
	public void onOffButtonClick(View view)
	{
		
		 Message msg = mBusHandler.obtainMessage(BusHandler.OFF, 
                 "off");
		 mBusHandler.sendMessage(msg);
		
	}
	
	/*public void onFlashButtonClick(View view)
	{
		EditText inputText = (EditText) findViewById(R.id.Edit_Input);
		//EditText resultText = (EditText) findViewById(R.id.Edit_Result);
		String inputStr = inputText.getText().toString();
		//resultText.setText(inputStr);
		
		 Message msg = mBusHandler.obtainMessage(BusHandler.FLASH, 
                 inputStr);
		 mBusHandler.sendMessage(msg);
		
	}*/
	
	 class BusHandler extends Handler {    	
	        /*
	         * Name used as the well-known name and the advertised name of the service this client is
	         * interested in.  This name must be a unique name both to the bus and to the network as a
	         * whole.
	         *
	         * The name uses reverse URL style of naming, and matches the name used by the service.
	         */
	        private static final String SERVICE_NAME = "org.alljoyn.sample.ledservice";
	        private static final String DAEMON_BUS_NAME = "quiet@org.alljoyn.BusNode.Led";
	        private static final String DEFAULT_SECURED_SRP_PASSWORD     = "1234";
	        private static final short CONTACT_PORT=24;

	        private BusAttachment mBus;
	        private ProxyBusObject mProxyObj;
	        private SimpleInterface mSimpleInterface;
	        
	        private int 	mSessionId;
	        private boolean mIsInASession;
	        private boolean mIsConnected;
	        private boolean mIsStoppingDiscovery;
	        
	        /* These are the messages sent to the BusHandler from the UI. */
	        public static final int CONNECT = 1;
	        public static final int JOIN_SESSION = 2;
	        public static final int DISCONNECT = 3;
	        public static final int ON = 4;
	        public static final int OFF = 5;
	        public static final int FLASH = 6;

	        public BusHandler(Looper looper) {
	            super(looper);
	            
	            mIsInASession = false;
	            mIsConnected = false;
	            mIsStoppingDiscovery = false;
	        }

	        @Override
	        public void handleMessage(Message msg) {
	            switch(msg.what) {
	            /* Connect to a remote instance of an object implementing the SimpleInterface. */
	            case CONNECT: {
	            	org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
	                /*
	                 * All communication through AllJoyn begins with a BusAttachment.
	                 *
	                 * A BusAttachment needs a name. The actual name is unimportant except for internal
	                 * security. As a default we use the class name as the name.
	                 *
	                 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
	                 * communication). The second argument must be set to Receive to allow communication
	                 * between devices.
	                 */
	                mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);
	                
	                /*
	                 * Create a bus listener class
	                 */
	                mBus.registerBusListener(new BusListener() {
	                    @Override
	                    public void foundAdvertisedName(String name, short transport, String namePrefix) {
	                    	logInfo(String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
	                    	/*
	                    	 * This client will only join the first service that it sees advertising
	                    	 * the indicated well-known name.  If the program is already a member of 
	                    	 * a session (i.e. connected to a service) we will not attempt to join 
	                    	 * another session.
	                    	 * It is possible to join multiple session however joining multiple 
	                    	 * sessions is not shown in this sample. 
	                    	 */
	                    	if(!mIsConnected) {
	                    	    Message msg = obtainMessage(JOIN_SESSION);
	                    	    msg.arg1 = transport;
	                    	    msg.obj = name;
	                    	    sendMessage(msg);
	                    	}
	                    }
	                });

	             // Set the password for the daemon to allow thin clients to connect.
	    			Log.d(TAG, "Setting daemon password");
	    			Status passwordStatus = PasswordManager.setCredentials("ALLJOYN_PIN_KEYX", DEFAULT_SECURED_SRP_PASSWORD);
	    			
	    			if ( passwordStatus != Status.OK ) {
	    				Log.e(TAG, "Failed to set password for daemon, Error: " + passwordStatus);
	    			}

	    			Log.d(TAG, "Setting the AuthListener");
	    			
	 //   			SrpKeyXListener authListener = new SrpKeyXListener();
	 //   			Status authStatus = mBus.registerAuthListener(authListener.getMechanisms(), authListener, authListener.getKeyStoreFileName());
	    			
	 //   			if ( authStatus != Status.OK ) {
	  //  				Log.e(TAG, "Failed to register AuthListener");
	  //  			}
	                
	                
	                /* To communicate with AllJoyn objects, we must connect the BusAttachment to the bus. */
	                Status status = mBus.connect();
	                logStatus("BusAttachment.connect()", status);
	                if (Status.OK != status) {
	                    finish();
	                    return;
	                }

	                /*
	                 * Now find an instance of the AllJoyn object we want to call.  We start by looking for
	                 * a name, then connecting to the device that is advertising that name.
	                 *
	                 * In this case, we are looking for the well-known SERVICE_NAME.
	                 */
	                SessionOpts sessionOpts = new SessionOpts();
	                sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
	                sessionOpts.isMultipoint = false;
	                sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;

	                /*
	                 * Explicitly add the Wi-Fi Direct transport into our
	                 * advertisements.  This sample is typically used in a "cable-
	                 * replacement" scenario and so it should work well over that
	                 * transport.  It may seem odd that ANY actually excludes Wi-Fi
	                 * Direct, but there are topological and advertisement/
	                 * discovery problems with WFD that make it problematic to
	                 * always enable.
	                 */
	                sessionOpts.transports = SessionOpts.TRANSPORT_ANY + SessionOpts.TRANSPORT_WFD;
	                
	                status = mBus.advertiseName(DAEMON_BUS_NAME, sessionOpts.TRANSPORT_TCP);
	                logStatus(String.format("BusAttachement.advertiseName(%s)", DAEMON_BUS_NAME), status);
                    if (status != Status.OK) {
                    	
                         //If we are unable to advertise the name, release
                         //the well-known name from the local bus.
                         
                        status = mBus.releaseName(DAEMON_BUS_NAME);
                        logStatus(String.format("BusAttachment.releaseName(%s)", DAEMON_BUS_NAME), status);
                    	finish();
                    	return;
                    }
	                
	                status = mBus.findAdvertisedName(SERVICE_NAME);
	                logStatus(String.format("BusAttachement.findAdvertisedName(%s)", SERVICE_NAME), status);
	                if (Status.OK != status) {
	                	finish();
	                	return;
	                }

	                break;
	            }
	            case (JOIN_SESSION): {
	            	/*
	                 * If discovery is currently being stopped don't join to any other sessions.
	                 */
	                if (mIsStoppingDiscovery) {
	                    break;
	                }
	                
	                /*
	                 * In order to join the session, we need to provide the well-known
	                 * contact port.  This is pre-arranged between both sides as part
	                 * of the definition of the chat service.  As a result of joining
	                 * the session, we get a session identifier which we must use to 
	                 * identify the created session communication channel whenever we
	                 * talk to the remote side.
	                 */
	                short contactPort = CONTACT_PORT;
	                SessionOpts sessionOpts = new SessionOpts();
	                sessionOpts.transports = (short)msg.arg1;
	                Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
	                
	                Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts, new SessionListener() {
	                    @Override
	                    public void sessionLost(int sessionId, int reason) {
	                        mIsConnected = false;
	                        logInfo(String.format("MyBusListener.sessionLost(sessionId = %d, reason = %d)", sessionId,reason));
	                        mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
	                    }
	                });
	                logStatus("BusAttachment.joinSession() - sessionId: " + sessionId.value, status);
	                    
	                if (status == Status.OK) {
	                	/*
	                     * To communicate with an AllJoyn object, we create a ProxyBusObject.  
	                     * A ProxyBusObject is composed of a name, path, sessionID and interfaces.
	                     * 
	                     * This ProxyBusObject is located at the well-known SERVICE_NAME, under path
	                     * "/SimpleService", uses sessionID of CONTACT_PORT, and implements the SimpleInterface.
	                     */
	                	mProxyObj =  mBus.getProxyBusObject(SERVICE_NAME, 
	                										"/org/alljoyn/sample/ledcontroller",
	                										sessionId.value,
	                										new Class<?>[] { SimpleInterface.class });

	                	/* We make calls to the methods of the AllJoyn object through one of its interfaces. */
	                	mSimpleInterface =  mProxyObj.getInterface(SimpleInterface.class);
	                	
	                	mSessionId = sessionId.value;
	                	mIsConnected = true;
	                	mHandler.sendEmptyMessage(MESSAGE_STOP_PROGRESS_DIALOG);
	                	sendUiMessage(MESSAGE_PING, "Connect Success");
	                }
	                break;
	            }
	            
	            /* Release all resources acquired in the connect. */
	            case DISCONNECT: {
	            	mIsStoppingDiscovery = true;
	            	if (mIsConnected) {
	                	Status status = mBus.leaveSession(mSessionId);
	                    logStatus("BusAttachment.leaveSession()", status);
	            	}
	                mBus.disconnect();
	                getLooper().quit();
	                break;
	            }
	            
	            /*
	             * Call the service's Ping method through the ProxyBusObject.
	             *
	             * This will also print the String that was sent to the service and the String that was
	             * received from the service to the user interface.
	             */
	            case ON: {
	                try {
	                	if (mSimpleInterface != null) {
	                		
	                		 mSimpleInterface.On();
	                		//sendUiMessage(MESSAGE_PING_REPLY, "test");
	                	}
	                } catch (BusException ex) {
	                    logException("SimpleInterface.On()", ex);
	                }
	                break;
	            }
	            case OFF: {
	                try {
	                	if (mSimpleInterface != null) {
	                		
	                		 mSimpleInterface.Off();
	                		//sendUiMessage(MESSAGE_PING_REPLY, "OFF");
	                	}
	                } catch (BusException ex) {
	                    logException("SimpleInterface.On()", ex);
	                }
	                break;
	            }
	            case FLASH: {
	                try {
	                	if (mSimpleInterface != null) {
	                		
	                		 mSimpleInterface.Flash(2000);
	                		//sendUiMessage(MESSAGE_PING_REPLY, "flash");
	                	}
	                } catch (BusException ex) {
	                    logException("SimpleInterface.On()", ex);
	                }
	                break;
	            }
	            
	            default:
	                break;
	            }
	        }
	        
	        /* Helper function to send a message to the UI thread. */
	        private void sendUiMessage(int what, Object obj) {
	            mHandler.sendMessage(mHandler.obtainMessage(what, obj));
	        }
	    }

	    private void logStatus(String msg, Status status) {
	        String log = String.format("%s: %s", msg, status);
	        if (status == Status.OK) {
	            Log.i(TAG, log);
	        } else {
	        	Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
	            mHandler.sendMessage(toastMsg);
	            Log.e(TAG, log);
	        }
	    }

	    private void logException(String msg, BusException ex) {
	        String log = String.format("%s: %s", msg, ex);
	        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
	        mHandler.sendMessage(toastMsg);
	        Log.e(TAG, log, ex);
	    }
	    
	    /*
	     * print the status or result to the Android log. If the result is the expected
	     * result only print it to the log.  Otherwise print it to the error log and
	     * Sent a Toast to the users screen. 
	     */
	    private void logInfo(String msg) {
	            Log.i(TAG, msg);
	    }
	
}
