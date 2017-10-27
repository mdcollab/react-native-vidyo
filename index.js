import PropTypes from "prop-types";
import React from "react";
import {findNodeHandle, requireNativeComponent, UIManager} from "react-native";


class Video extends React.Component {
  connect() {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.RNTVideo.Commands.connect,
      [],
    );
  }

  disconnect() {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.RNTVideo.Commands.disconnect,
      [],
    );
  }

  switchCamera() {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.RNTVideo.Commands.switchCamera,
      [],
    );
  }

  toggleCameraOn() {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.RNTVideo.Commands.toggleCameraOn,
      [],
    );
  }

  toggleMicrophoneOn() {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.RNTVideo.Commands.toggleMicrophoneOn,
      [],
    );
  }

  refreshUI() {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.RNTVideo.Commands.refreshUI,
      [],
    );
  }

  render() {
    return <RNTVideo {...this.props} />;
  }
}

Video.propTypes = {
  host: PropTypes.string,
  token: PropTypes.string,
  displayName: PropTypes.string,
  resourceId: PropTypes.string,
  onConnect: PropTypes.func,
  onDisconnect: PropTypes.func,
  onFailure: PropTypes.func,
};

var RNTVideo = requireNativeComponent('RNTVideo', Video);

module.exports = Video;
