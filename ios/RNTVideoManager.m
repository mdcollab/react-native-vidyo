#import "RNTVideoManager.h"
#import "RNTVideoView.h"
#import "Lmi/VidyoClient/VidyoConnector_Objc.h"
#import <Foundation/Foundation.h>
#import "Logger.h"


@interface RNTVideoManager () <IConnect, RNTVideoViewDelegate> {
  @private
    BOOL    enableDebug;
    Logger  *logger;
}

@property (nonatomic, assign, getter=isCameraOn) BOOL cameraOn;
@property (nonatomic, assign, getter=isMicrophoneOn) BOOL microphoneOn;
@property (nonatomic, strong) Connector *connector;
@property (nonatomic, weak) RNTVideoView *videoView;

@end

@implementation RNTVideoManager

RCT_EXPORT_MODULE()

RCT_EXPORT_VIEW_PROPERTY(host, NSString)
RCT_EXPORT_VIEW_PROPERTY(token, NSString)
RCT_EXPORT_VIEW_PROPERTY(displayName, NSString)
RCT_EXPORT_VIEW_PROPERTY(resourceId, NSString)
RCT_EXPORT_VIEW_PROPERTY(onConnect, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onDisconnect, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onFailure, RCTBubblingEventBlock)

- (UIView *)view {
    RNTVideoView *view = [RNTVideoView new];
    view.delegate = self;
    self.videoView = view;
    return view;
}

#pragma mark EXPORT methods

RCT_EXPORT_METHOD(connect:(nonnull NSNumber *)reactTag) {
    [self _connect];
}

RCT_EXPORT_METHOD(disconnect:(nonnull NSNumber *)reactTag) {
    [self.connector Disconnect];
}

RCT_EXPORT_METHOD(switchCamera:(nonnull NSNumber *)reactTag) {
    [self.connector CycleCamera];
}

RCT_EXPORT_METHOD(toggleCameraOn:(nonnull NSNumber *)reactTag) {
    BOOL result = [self.connector SetCameraPrivacy:!self.isCameraOn];
    if (result) {
        self.cameraOn = !self.isCameraOn;
    }
}

RCT_EXPORT_METHOD(toggleMicrophoneOn:(nonnull NSNumber *)reactTag) {
  BOOL result = [self.connector SetMicrophonePrivacy:!self.isMicrophoneOn];
  if (result) {
    self.microphoneOn = !self.isMicrophoneOn;
  }
}

RCT_EXPORT_METHOD(refreshUI:(nonnull NSNumber *)reactTag) {
  [self RefreshUI];
}

#pragma mark -
#pragma mark VidyoConnector Event Handlers

- (void)_connect {
  BOOL status = [self.connector Connect:[self.videoView.host UTF8String]
                                  Token:[self.videoView.token UTF8String]
                            DisplayName:[self.videoView.displayName UTF8String]
                             ResourceId:[self.videoView.resourceId UTF8String]
                                Connect:self];

  [logger Log:[NSString stringWithFormat:@"VidyoConnectorConnect status = %d", status]];
}

- (void)OnSuccess {
    [logger Log:@"Successfully connected."];

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.videoView.onConnect) {
            self.videoView.onConnect(@{});
        }
    });
}

- (void)OnFailure:(ConnectorFailReason)reason {
    [logger Log:@"Connection attempt failed."];

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.videoView.onFailure) {
            self.videoView.onFailure(@{@"reason": @(reason)});
        }
    });
}

- (void)OnDisconnected:(ConnectorDisconnectReason)reason {
    if (reason == CONNECTORDISCONNECTREASON_Disconnected) {
      [logger Log:@"Succesfully disconnected."];
    } else {
      [logger Log:@"Unexpected disconnection."];
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.videoView.onDisconnect) {
            self.videoView.onDisconnect(@{@"reason": @(reason)});
        }
    });
}

-(void) OnLog:(LogRecord*)logRecord {
  [logger LogClientLib:logRecord->message];
}

#pragma mark -
#pragma mark View Lifecycle

- (void)viewDidMoveToSuperview {
    logger = [[Logger alloc] init];
    [logger Log:@"RNTVideoManager::viewDidMoveToSuperview called."];

    enableDebug = NO;

    [VidyoClientConnector Initialize];

    [self.videoView setNeedsLayout];
    [self.videoView layoutIfNeeded];

    UIView *videoContainerView = self.videoView.videoContainerView;
    self.connector = [[Connector alloc] init:(void*)&videoContainerView
                                   ViewStyle:CONNECTORVIEWSTYLE_Default
                          RemoteParticipants:15
                               LogFileFilter:"info@VidyoClient info@VidyoConnector warning"
                                 LogFileName:""
                                    UserData:0];

    if (self.connector) {
        if (enableDebug) {
            [self.connector EnableDebug:7776 LogFilter:"warning info@VidyoClient info@VidyoConnector"];
        }
        if (![self.connector RegisterLogEventListener:self Filter:"info@VidyoClient info@VidyoConnector warning"]) {
            [logger Log:@"RegisterLogEventListener failed"];
        }
        [self RefreshUI];
    } else {
        [logger Log:@"ERROR: VidyoConnector construction failed ..."];
    }

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(appDidEnterBackground:)
                                                 name:UIApplicationDidEnterBackgroundNotification
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(appWillEnterForeground:)
                                                 name:UIApplicationWillEnterForegroundNotification
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(appWillTerminate:)
                                                 name:UIApplicationWillTerminateNotification
                                               object:nil];
}

#pragma mark -
#pragma mark Application Lifecycle

- (void)appDidEnterBackground:(NSNotification*)notification {
    [self.connector SetMode:CONNECTORMODE_Background];
}

- (void)appWillEnterForeground:(NSNotification*)notification {
    [self.connector SetMode:CONNECTORMODE_Foreground];
}

- (void)appWillTerminate:(NSNotification*)notification {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [VidyoClientConnector Uninitialize];
    [logger Close];
}

#pragma mark -
#pragma mark App UI Updates

- (void)RefreshUI {
    UIView *videoContainerView = self.videoView.videoContainerView;

    [logger Log:[
      NSString stringWithFormat:@"VidyoConnectorShowViewAt: x = %f, y = %f, w = %f, h = %f",
      videoContainerView.frame.origin.x,
      videoContainerView.frame.origin.y,
      videoContainerView.frame.size.width,
      videoContainerView.frame.size.height
    ]];

    [self.connector ShowViewAt:&videoContainerView
                             X:0
                             Y:0
                         Width:videoContainerView.bounds.size.width
                        Height:videoContainerView.bounds.size.height];
}

@end
