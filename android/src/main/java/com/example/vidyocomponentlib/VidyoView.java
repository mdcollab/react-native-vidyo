package com.example.vidyocomponentlib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.content.res.Configuration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.uimanager.ThemedReactContext;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Connector.VidyoConnector;
import com.vidyo.VidyoClient.Endpoint.VidyoLogRecord;
import com.vidyo.VidyoClient.VidyoNetworkInterface;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;


public class VidyoView extends ConstraintLayout implements
        VidyoConnector.IConnect,
        VidyoConnector.IRegisterLogEventListener,
        VidyoConnector.IRegisterNetworkInterfaceEventListener {

    private static final String TAG = "VidyoView";

    enum VIDYO_CONNECTOR_STATE {
        VC_CONNECTED,
        VC_DISCONNECTED,
        VC_DISCONNECTED_UNEXPECTED,
        VC_CONNECTION_FAILURE
    }

    private VIDYO_CONNECTOR_STATE mVidyoConnectorState = VIDYO_CONNECTOR_STATE.VC_DISCONNECTED;
    private boolean mVidyoConnectorConstructed = false;
    private boolean mVidyoClientInitialized = false;
    private Logger mLogger = Logger.getInstance();
    private VidyoConnector mVidyoConnector = null;
    private FrameLayout mVideoFrame;
    private WeakReference<ThemedReactContext> reactContext;
    private String host;
    private String token;
    private String userName;
    private String resourceId;
    private Boolean cameraOn = true;
    private Boolean microphoneOn = true;
    private Boolean mEnableDebug = true;
    private int width;
    private int height;

    private final Runnable mLayoutRunnable = new Runnable() {
        @Override
        public void run() {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };


    public VidyoView(@NonNull ThemedReactContext context) {
        super(context);
        setUp();
    }

    public VidyoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    public VidyoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUp();
    }

    private void setUp() {
        mLogger.Log("Vidyo: SET UP");
        inflate(getContext(), R.layout.component_vidyo_view, this);

        setWillNotDraw(false);

        final ThemedReactContext context = (ThemedReactContext) getContext();

        context.addLifecycleEventListener(new LifecycleEventListener() {
            @Override
            public void onHostResume() {
                mLogger.Log("Vidyo: RESUME");
                start();
            }

            @Override
            public void onHostPause() {
                mLogger.Log("Vidyo: PAUSE");
                if (mVidyoConnectorConstructed && mVidyoConnector != null) {
                    mVidyoConnector.SetMode(VidyoConnector.VidyoConnectorMode.VIDYO_CONNECTORMODE_Background);
                }
            }

            @Override
            public void onHostDestroy() {
                mLogger.Log("Vidyo: DESTROY");
                cleanUp();
            }
        });

        setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mVideoFrame = (FrameLayout) findViewById(R.id.video_container);
        // suppress keyboard
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Connector.SetApplicationUIContext(context.getCurrentActivity());

        mVidyoClientInitialized = Connector.Initialize();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        post(mLayoutRunnable);
    }

    public void start() {
        mLogger.Log("start");
        ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mVideoFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mLogger.Log("vidyo width:" + mVideoFrame.getMeasuredHeight() + " " + mVideoFrame.getHeight());
                    checkVidyoConnection();
                }
            });
            viewTreeObserver.dispatchOnGlobalLayout();
        }
    }

    public void stop(){
        mLogger.Log("Stop");
        if (mVidyoConnectorConstructed) {
            mVidyoConnector.SetMode(VidyoConnector.VidyoConnectorMode.VIDYO_CONNECTORMODE_Background);
        }
    }

    public void cleanUp() {
        mLogger.Log("Detach");
        if (mVidyoConnector != null) {
            if (mVidyoConnectorState == VIDYO_CONNECTOR_STATE.VC_CONNECTED) {
                mVidyoConnector.Disconnect();
                emitVidyoConnectionEnd((ThemedReactContext) getContext(), getId());
            }
            mVidyoConnector.Disable();
            mVidyoConnector = null;
        }

        Connector.Uninitialize();
    }


    public void toggleCameraOn(){
        mLogger.Log("Disable camera");
        if(mVidyoConnector != null) {
            cameraOn = !cameraOn;
            mVidyoConnector.SetCameraPrivacy(!cameraOn);
        }
    }


    public void toggleMicrophoneOn() {
        mLogger.Log("microPhoneOn before: " + microphoneOn);
        if (mVidyoConnector != null) {
            microphoneOn = !microphoneOn;
            mVidyoConnector.SetMicrophonePrivacy(!microphoneOn);
        }
        mLogger.Log("microPhoneOn after: " + microphoneOn);
    }


    // The device interface orientation has changed
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mLogger.Log("onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

        // Refresh the video size after it is painted
        ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mVideoFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    // Width/height values of views not updated at this point so need to wait
                    // before refreshing UI

                    refreshView();
                }
            });
        }
    }

    /*
     * Private Utility Functions
     */

    private void checkVidyoConnection() {
        if (!mVidyoConnectorConstructed) {
            if (mVidyoClientInitialized) {
                createVidyoConnector();
                refreshView();
            } else {
                mLogger.Log( "ERROR: VidyoClientInitialize failed - not constructing VidyoConnector ...");
            }

            mLogger.Log( "onResume: vidyoConnectorConstructed => " + (mVidyoConnectorConstructed ? "success" : "failed"));
        }
    }

    public void createVidyoConnector() {
        mVidyoConnector = new VidyoConnector(mVideoFrame,
                VidyoConnector.VidyoConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                15,
                "info@VidyoClient info@VidyoConnector warning",
                "",
                0);

        mVidyoConnectorConstructed = true;

        if (!mVidyoConnector.RegisterNetworkInterfaceEventListener(VidyoView.this)) {
            mLogger.Log( "ERROR: VidyoConnector RegisterNetworkInterfaceEventListener failed");
        }

        if (!mVidyoConnector.RegisterLogEventListener(VidyoView.this, "info@VidyoClient info@VidyoConnector warning")) {
            mLogger.Log("ERROR: VidyoConnector RegisterLogEventListener failed");
        }
    }

    public void refreshView() {
        mLogger.Log("refreshView");
        mVidyoConnector.ShowViewAt(mVideoFrame, 0, 0, this.width, this.height);
        mLogger.Log("VidyoConnectorShowViewAt: x = 0, y = 0, w = " + this.width + ", h = " + this.height);

    }

    private void ConnectorStateUpdated(VIDYO_CONNECTOR_STATE state, final String statusText) {
        mLogger.Log("ConnectorStateUpdated, state = " + state.toString());
        mLogger.Log("onConnectorStateUpdeted, state text = " + statusText);

        mVidyoConnectorState = state;

        ThemedReactContext context = (ThemedReactContext) getContext();
        if (context != null && context.getCurrentActivity() != null) {
            context.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkConnectionState(mVidyoConnectorState);
                }
            });
        }
    }

    private void checkConnectionState(VIDYO_CONNECTOR_STATE state){
        if (state == VIDYO_CONNECTOR_STATE.VC_CONNECTED) {
            onStateConnected();
        } else if (state == VIDYO_CONNECTOR_STATE.VC_CONNECTION_FAILURE) {
            onStateConnectionFailure();
        } else if (state == VIDYO_CONNECTOR_STATE.VC_DISCONNECTED) {
            onStateDisconnected();
        } else if (state == VIDYO_CONNECTOR_STATE.VC_DISCONNECTED_UNEXPECTED) {
            onStateDisconnected();
        }
    }

    private void onStateConnected(){
        mLogger.Log("State connected");
        refreshView();
        emitVidyoConnected((ThemedReactContext) getContext(), getId());
    }

    private void onStateConnectionFailure(){
        emitVidyoConnectionFailure((ThemedReactContext) getContext(), getId(), "cannot connect");
    }

    private void onStateDisconnected() {
        emitVidyoConnectionEnd((ThemedReactContext) getContext(), getId());
    }

    public void connect() {
        if (mVidyoConnectorState != VIDYO_CONNECTOR_STATE.VC_CONNECTED &&
              mVidyoConnector != null &&
              !resourceId.contains(" ") &&
              !resourceId.contains("@")) {
            mLogger.Log("host: " + host);
            mLogger.Log("token: " + token);
            mLogger.Log("userName: " + userName);
            mLogger.Log("resourceeId: " + resourceId);

            if (!mVidyoConnector.Connect(host, token, userName, resourceId, this)) {
                ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_CONNECTION_FAILURE, "Connection failed");
                mLogger.Log("Status: " + false);
            } else
                mLogger.Log("Status: " + true);
        }
    }

    public void switchCamera() {
        mVidyoConnector.CycleCamera();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        mLogger.Log("OnDraw");
        super.onDraw(canvas);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLogger.Log("onFinishInflate");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mLogger.Log("onMeasure");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mLogger.Log("onLayout");
    }

//never used?
    public void restartConnection() {
        mLogger.Log("Restart");
        if (mVidyoConnectorConstructed) {
            mVidyoConnector.SetMode(VidyoConnector.VidyoConnectorMode.VIDYO_CONNECTORMODE_Foreground);
        }
    }

    public void setHeight(int h) {
        this.height = h;
    }

    public void setWidth(int w) {
        this.width = w;
    }

    /*
     *  Connector Events
     */
    @Override
    public void OnSuccess() {
        mLogger.Log("OnSuccess: successfully connected.");
        ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_CONNECTED, "Connected");
    }
    @Override
    public void OnFailure(VidyoConnector.VidyoConnectorFailReason reason) {
        mLogger.Log("OnFailure: connection attempt failed, reason = " + reason.toString());
        ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_CONNECTION_FAILURE, "Connection failed");
    }

    @Override
    public void OnDisconnected(VidyoConnector.VidyoConnectorDisconnectReason reason) {
        VIDYO_CONNECTOR_STATE state;
        String statusText;
        if (reason == VidyoConnector.VidyoConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
            mLogger.Log("OnDisconnected: successfully disconnected, reason = " + reason.toString());
            state = VIDYO_CONNECTOR_STATE.VC_DISCONNECTED;
            statusText = "Disconnected";
        } else {
            mLogger.Log("OnDisconnected: unexpected disconnection, reason = " + reason.toString());
            state = VIDYO_CONNECTOR_STATE.VC_DISCONNECTED_UNEXPECTED;
            statusText = "Unexpected disconnection";
        }
        ConnectorStateUpdated(state, statusText);
    }

    public void OnLog(VidyoLogRecord logRecord) {
        mLogger.LogClientLib(logRecord.message);
    }

    public void OnNetworkInterfaceAdded(VidyoNetworkInterface vidyoNetworkInterface) {
        mLogger.Log("OnNetworkInterfaceAdded: name=" + vidyoNetworkInterface.GetName() + " address=" + vidyoNetworkInterface.GetAddress() + " type=" + vidyoNetworkInterface.GetType() + " family=" + vidyoNetworkInterface.GetFamily());
    }

    public void OnNetworkInterfaceRemoved(VidyoNetworkInterface vidyoNetworkInterface) {
        mLogger.Log("OnNetworkInterfaceRemoved: name=" + vidyoNetworkInterface.GetName() + " address=" + vidyoNetworkInterface.GetAddress() + " type=" + vidyoNetworkInterface.GetType() + " family=" + vidyoNetworkInterface.GetFamily());

    }

    public void OnNetworkInterfaceSelected(VidyoNetworkInterface vidyoNetworkInterface, VidyoNetworkInterface.VidyoNetworkInterfaceTransportType vidyoNetworkInterfaceTransportType) {
        mLogger.Log("OnNetworkInterfaceSelected: name=" + vidyoNetworkInterface.GetName() + " address=" + vidyoNetworkInterface.GetAddress() + " type=" + vidyoNetworkInterface.GetType() + " family=" + vidyoNetworkInterface.GetFamily());

    }

    public void OnNetworkInterfaceStateUpdated(VidyoNetworkInterface vidyoNetworkInterface, VidyoNetworkInterface.VidyoNetworkInterfaceState vidyoNetworkInterfaceState) {
        mLogger.Log("OnNetworkInterfaceStateUpdated: name=" + vidyoNetworkInterface.GetName() + " address=" + vidyoNetworkInterface.GetAddress() + " type=" + vidyoNetworkInterface.GetType() + " family=" + vidyoNetworkInterface.GetFamily() + " state=" + vidyoNetworkInterfaceState);
    }


    private final String CONNECTION_STOP = "onDisconnect";
    private final String CONNECTION_START = "onConnect";
    private final String CONNECTION_FAILURE = "onFailure";

    private void emitVidyoConnectionEnd(ThemedReactContext context, int viewId) {
        emit(context, viewId, CONNECTION_STOP, "");
    }

    private void emitVidyoConnected(ThemedReactContext context, int viewId) {
        emit(context, viewId, CONNECTION_START, "");
    }

    private void emitVidyoConnectionFailure(ThemedReactContext context, int viewId, String errorText) {
        emit(context, viewId, CONNECTION_FAILURE, errorText);
    }

    private void emit(ThemedReactContext context, int eventId, String event, String message) {
        mLogger.Log("Vidyo emitting: eventId = " + eventId);
        mLogger.Log("Vidyo emitting: event = " + event);
        mLogger.Log("Vidyo emitting: message = " + message);
        if (context != null) {
            WritableMap data = Arguments.createMap();
            data.putString("event", event);
            data.putString("message", message);
            context.getJSModule(RCTEventEmitter.class).receiveEvent(eventId, "topChange", data);
        }
    }
}
