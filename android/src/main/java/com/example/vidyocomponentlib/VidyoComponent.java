package com.example.vidyocomponentlib;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.SimpleViewManager;

import java.util.Map;
import javax.annotation.Nullable;


public class VidyoComponent extends SimpleViewManager<VidyoView> {

    private static final String COMPONENT_NAME = "RNTVideo";

    public static final int COMMAND_CONNECT = 1;
    public static final int COMMAND_CLEAN_UP = 2;
    public static final int COMMAND_TOGGLE_CAMERA_ON = 3;
    public static final int COMMAND_TOGGLE_MICROPHONE_ON = 4;
    public static final int COMMAND_SWITCH_CAMERA = 5;
    public static final int COMMAND_REFRESH_UI = 6;

    private VidyoView currentView;

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }

    @Override
    protected VidyoView createViewInstance(ThemedReactContext reactContext) {
        currentView = new VidyoView(reactContext);
        return currentView;
    }

    @ReactProp(name = "height")
    public void setHeight(VidyoView view, int height) {
        view.setHeight(height);
    }

    @ReactProp(name = "width")
    public void setWidth(VidyoView view, int width) {
        view.setWidth(width);
    }

    @ReactProp(name = "token")
    public void setToken(VidyoView view, String token) {
        view.setToken(token);
    }

    @ReactProp(name = "host")
    public void setHost(VidyoView view, String host) {
        view.setHost(host);
    }

    @ReactProp(name = "displayName")
    public void setUserName(VidyoView view, String userName) {
        view.setUserName(userName);
    }

    @ReactProp(name = "resourceId")
    public void setRoomId(VidyoView view, String roomName) {
        view.setResourceId(roomName);
    }

    @Override
    public Map<String,Integer> getCommandsMap() {
        return MapBuilder.of(
            "connect", COMMAND_CONNECT,
            "cleanUp", COMMAND_CLEAN_UP,
            "toggleCameraOn", COMMAND_TOGGLE_CAMERA_ON,
            "toggleMicrophoneOn", COMMAND_TOGGLE_MICROPHONE_ON,
            "switchCamera", COMMAND_SWITCH_CAMERA,
            "refreshUI", COMMAND_REFRESH_UI
        );
    }

	@Override
	public void receiveCommand(VidyoView view, int commandType, @Nullable ReadableArray args) {
		Assertions.assertNotNull(view);
		Assertions.assertNotNull(args);
		switch (commandType) {
            case COMMAND_CONNECT: {
                view.connect();
                return;
            }
            case COMMAND_TOGGLE_CAMERA_ON: {
                view.toggleCameraOn();
                return;
            }
            case COMMAND_TOGGLE_MICROPHONE_ON: {
                view.toggleMicrophoneOn();
                return;
            }
            case COMMAND_SWITCH_CAMERA: {
                view.switchCamera();
                return;
            }
            case COMMAND_REFRESH_UI: {
                //necessary on android?
                view.refreshView();
                return;
            }
            case COMMAND_CLEAN_UP: {
                view.cleanUp();
                return;
            }
			default:
				throw new IllegalArgumentException(String.format(
                    "Unsupported command %d received by %s.",
                    commandType,
                    getClass().getSimpleName()));
		}
	}
}
