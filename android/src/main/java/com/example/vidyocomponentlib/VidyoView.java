package com.example.vidyocomponentlib;

import android.content.Context;
import android.graphics.Canvas;
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
    private Boolean cameraOn;
    private Boolean microphoneOn;
    private Boolean mEnableDebug = true;

    public VidyoView(@NonNull Context context) {
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
        mLogger.Log("SET UP");
        inflate(getContext(), R.layout.component_vidyo_view, this);

        setWillNotDraw(false);

        final ThemedReactContext context = (ThemedReactContext) getContext();

        context.addLifecycleEventListener(new LifecycleEventListener() {
            @Override
            public void onHostResume() {
                mLogger.Log("RESUME");
                start();
            }

            @Override
            public void onHostPause() {
                mLogger.Log("PAUSE");
                if (mVidyoConnectorConstructed) {
                    mVidyoConnector.SetMode(VidyoConnector.VidyoConnectorMode.VIDYO_CONNECTORMODE_Background);
                }
            }

            @Override
            public void onHostDestroy() {
                mLogger.Log("DESTROY");
                mVidyoConnector.Disable();
                Connector.Uninitialize();
            }
        });

        reactContext = new WeakReference<>((ThemedReactContext) getContext());

        setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mVideoFrame = (FrameLayout) findViewById(R.id.video_container);

        // suppress keyboard
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (reactContext.get() != null) {
            Connector.SetApplicationUIContext(reactContext.get().getCurrentActivity());
        }

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
    }

    public void start() {
        mLogger.Log("start");
        ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mVideoFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // If the vidyo connector was not previously successfully constructed then construct it
                    if (!mVidyoConnectorConstructed) {
                        if (mVidyoClientInitialized) {
                            mVidyoConnector = new VidyoConnector(mVideoFrame,
                                    VidyoConnector.VidyoConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                                    15,
                                    "info@VidyoClient info@VidyoConnector warning",
                                    "",
                                    0);

                            if (mVidyoConnector != null) {
                                mVidyoConnectorConstructed = true;

                                // If enableDebug is configured then enable debugging
                                if (mEnableDebug) {
                                    mVidyoConnector.EnableDebug(7776, "warning info@VidyoClient info@VidyoConnector");
                                }

                                RefreshUI();

                                if (!mVidyoConnector.RegisterNetworkInterfaceEventListener(VidyoView.this)) {
                                    mLogger.Log("VidyoConnector RegisterNetworkInterfaceEventListener failed");
                                }

                                if (!mVidyoConnector.RegisterLogEventListener(VidyoView.this, "info@VidyoClient info@VidyoConnector warning")) {
                                    mLogger.Log("VidyoConnector RegisterLogEventListener failed");
                                }
                            } else {
                                mLogger.Log("VidyoConnector Construction failed - cannot connect...");
                            }
                        } else {
                            mLogger.Log("ERROR: VidyoClientInitialize failed - not constructing VidyoConnector ...");
                        }

                        Logger.getInstance().Log("onResume: mVidyoConnectorConstructed => " + (mVidyoConnectorConstructed ? "success" : "failed"));
                    }
                }
            });
            //viewTreeObserver.dispatchOnGlobalLayout();
        }
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

                    RefreshUI();
                }
            });
        }
    }

    /*
     * Private Utility Functions
     */

    private void RefreshUI() {
        mVidyoConnector.ShowViewAt(mVideoFrame, 0, 0, mVideoFrame.getWidth(), mVideoFrame.getHeight());
        mLogger.Log("VidyoConnectorShowViewAt: x = 0, y = 0, w = " + mVideoFrame.getWidth() + ", h = " + mVideoFrame.getHeight());
    }

    private void ConnectorStateUpdated(VIDYO_CONNECTOR_STATE state, final String statusText) {
        mLogger.Log("ConnectorStateUpdated, state = " + state.toString());
        mVidyoConnectorState = state;
        if (mVidyoConnectorState == VIDYO_CONNECTOR_STATE.VC_CONNECTION_FAILURE) {
            emitVidyoConnectionFailure(reactContext.get(), getId(), "cannot connect");
        }
    }

    public void connect() {
        if (mVidyoConnectorState != VIDYO_CONNECTOR_STATE.VC_CONNECTED &&
              mVidyoConnector != null &&
              !resourceId.contains(" ") &&
              !resourceId.contains("@")) {
            mLogger.Log("host: " + host);
            mLogger.Log("token: " + token);
            mLogger.Log("userName: " + userName);
            mLogger.Log("resoureId: " + resourceId);

            if (!mVidyoConnector.Connect(host, token, userName, resourceId, this)) {
                ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_CONNECTION_FAILURE, "Connection failed");
                mLogger.Log("Status: " + false);
            } else
                mLogger.Log("Status: " + true);
        }
    }

    public void disconnect() {
        mLogger.Log("Detach");
        if (mVidyoConnector != null) {
            mVidyoConnector.Disconnect();
        }
    }

    public void toggleCameraOn() {
        if (mVidyoConnector.SetCameraPrivacy(!cameraOn)) {
            cameraOn = !cameraOn;
        }
    }

    public void toggleMicrophoneOn() {
        if (mVidyoConnector.SetMicrophonePrivacy(!microphoneOn)) {
            microphoneOn = !microphoneOn;
        }
    }

    public void switchCamera() {
        mVidyoConnector.CycleCamera();
    }

    public void refreshView() {
        mLogger.Log("refreshView");
        mVidyoConnector.ShowViewAt(mVideoFrame, 0, 0, mVideoFrame.getWidth(), mVideoFrame.getHeight());
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

    public void setHeight(int height) {
        getLayoutParams().height = height;
    }

    public void setWidth(int width) {
        getLayoutParams().width = width;
    }

    /*
     *  Connector Events
     */

    public void OnSuccess() {
        mLogger.Log("OnSuccess: successfully connected.");
        ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_CONNECTED, "Connected");
        emitVidyoConnected(reactContext.get(), getId());
    }

    public void OnFailure(VidyoConnector.VidyoConnectorFailReason reason) {
        mLogger.Log("OnFailure: connection attempt failed, reason = " + reason.toString());
        ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_CONNECTION_FAILURE, "Connection failed");
        emitVidyoConnectionFailure(reactContext.get(), getId(), reason.toString());
    }

    public void OnDisconnected(VidyoConnector.VidyoConnectorDisconnectReason reason) {
        if (reason == VidyoConnector.VidyoConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
            mLogger.Log("OnDisconnected: successfully disconnected, reason = " + reason.toString());
            ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_DISCONNECTED, "Disconnected");
        } else {
            mLogger.Log("OnDisconnected: unexpected disconnection, reason = " + reason.toString());
            ConnectorStateUpdated(VIDYO_CONNECTOR_STATE.VC_DISCONNECTED_UNEXPECTED, "Unexpected disconnection");
        }
        emitVidyoConnectionEnd(reactContext.get(), getId());
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


    private static final String CONNECTION_STOP = "onDisconnect";
    private static final String CONNECTION_START = "onConnect";
    private static final String CONNECTION_FAILURE = "onFailure";

    static void emitVidyoConnectionEnd(ThemedReactContext context, int viewId) {
        emit(context, viewId, CONNECTION_STOP, "");
    }

    static void emitVidyoConnected(ThemedReactContext context, int viewId) {
        emit(context, viewId, CONNECTION_START, "");
    }

    static void emitVidyoConnectionFailure(ThemedReactContext context, int viewId, String errorText) {
        emit(context, viewId, CONNECTION_FAILURE, errorText);
    }

    private static void emit(ThemedReactContext context, int eventId, String event, String message) {
        if (context != null) {
            WritableMap data = Arguments.createMap();
            data.putString("event", event);
            data.putString("message", message);
            context.getJSModule(RCTEventEmitter.class).receiveEvent(eventId, "topChange", data);
        }
    }
}
