#import <UIKit/UIKit.h>
#import <React/RCTComponent.h>

@protocol RNTVideoViewDelegate <NSObject>
- (void)viewDidMoveToSuperview;
@end

@interface RNTVideoView : UIView

@property (nonatomic, weak) id<RNTVideoViewDelegate> delegate;
@property (nonatomic, weak, readonly) UIView *videoContainerView;

@property (nonatomic, copy) NSString *host;
@property (nonatomic, copy) NSString *token;
@property (nonatomic, copy) NSString *displayName;
@property (nonatomic, copy) NSString *resourceId;
@property (nonatomic, copy) NSNumber *width;
@property (nonatomic, copy) NSNumber *height;

@property (nonatomic, copy) RCTBubblingEventBlock onConnect;
@property (nonatomic, copy) RCTBubblingEventBlock onDisconnect;
@property (nonatomic, copy) RCTBubblingEventBlock onFailure;

@end
