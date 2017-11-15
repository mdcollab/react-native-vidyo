import PropTypes from "prop-types";
import React from "react";
import {findNodeHandle, requireNativeComponent, UIManager, View} from "react-native";


const CONNECTION_START = "onConnect";
const CONNECTION_STOP = "onDisconnect";
const CONNECTION_FAILURE = "onFailure";

class Video extends React.Component {
  constructor() {
    super();

    this.onChange = this.onChange.bind(this);
  }

  onChange(event) {
    if (event.nativeEvent.event === CONNECTION_START) return this.props.onConnect();
    if (event.nativeEvent.event === CONNECTION_STOP) return this.props.onDisconnect();
    if (event.nativeEvent.event === CONNECTION_FAILURE) return this.props.onFailure(event.nativeEvent.message);
  }

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
    return <RNTVideo
      {...this.props}
      onChange={this.onChange}
    />;
  }
}

Video.propTypes = {
  ...View.propTypes,
  host: PropTypes.string,
  token: PropTypes.string,
  displayName: PropTypes.string,
  resourceId: PropTypes.string,
  onConnect: PropTypes.func,
  onDisconnect: PropTypes.func,
  onFailure: PropTypes.func,
};

var RNTVideo = requireNativeComponent('RNTVideo', Video, {nativeOnly: {onChange: true}});

module.exports = Video;
