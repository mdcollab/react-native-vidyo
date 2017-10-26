#import <UIKit/UIKit.h>
#import <React/RCTComponent.h>

@protocol RNTVideoViewDelegate <NSObject>
- (void)viewDidMoveToSuperview;
@end

@interface RNTVideoView : UIView

@property (nonatomic, weak) id<RNTVideoViewDelegate> delegate;
@property (nonatomic, weak, readonly) UIView *videoContainerView;

@end
