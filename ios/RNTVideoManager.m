#import "RNTVideoManager.h"
#import "RNTVideoView.h"
#import "Lmi/VidyoClient/VidyoConnector_Objc.h"

@interface RNTVideoManager () <IConnect, RNTVideoViewDelegate>

@property (nonatomic, assign, getter=isCameraOn) BOOL cameraOn;
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
    const char *host = [self.videoView.host UTF8String];
    const char *token = [self.videoView.token UTF8String];
    const char *displayName = [self.videoView.displayName UTF8String];
    const char *resourceId = [self.videoView.resourceId UTF8String];

    [self.connector Connect:host Token:token DisplayName:displayName ResourceId:resourceId Connect:self];
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

#pragma mark IConnect methods

- (void)OnSuccess {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.videoView.onConnect) {
            self.videoView.onConnect(@{});
        }
    });
}

- (void)OnFailure:(ConnectorFailReason)reason {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.videoView.onFailure) {
            self.videoView.onFailure(@{@"reason": @(reason)});
        }
    });
    
}

- (void)OnDisconnected:(ConnectorDisconnectReason)reason {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.videoView.onDisconnect) {
            self.videoView.onDisconnect(@{@"reason": @(reason)});
        }
    });
}

- (void)viewDidMoveToSuperview {
    [VidyoClientConnector Initialize];
    [self.videoView setNeedsLayout];
    [self.videoView layoutIfNeeded];
    UIView *videoContainerView = self.videoView.videoContainerView;
    self.connector = [[Connector alloc] init:&videoContainerView ViewStyle:CONNECTORVIEWSTYLE_Default RemoteParticipants:16 LogFileFilter:"" LogFileName:"" UserData:0];
    [self.connector ShowViewAt:&videoContainerView X:0 Y:0 Width:videoContainerView.bounds.size.width Height:videoContainerView.bounds.size.height];
}

@end
