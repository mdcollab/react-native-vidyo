#import "RNTVideoView.h"

@interface RNTVideoView ()
@property (nonatomic, weak, readwrite) UIView *videoContainerView;
@end

@implementation RNTVideoView

- (instancetype)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        [self setUp];
    }
    return self;
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    [self.delegate viewDidMoveToSuperview];
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

#pragma mark Private methods

- (void)setUp {
    UIView *videoContainerView = [UIView new];
    videoContainerView.translatesAutoresizingMaskIntoConstraints = NO;
    [self addSubview:videoContainerView];
    self.videoContainerView = videoContainerView;
    NSArray *layoutConstraints = @[[videoContainerView.topAnchor constraintEqualToAnchor:self.topAnchor],
                                   [videoContainerView.bottomAnchor constraintEqualToAnchor:self.bottomAnchor],
                                   [videoContainerView.leadingAnchor constraintEqualToAnchor:self.leadingAnchor],
                                   [videoContainerView.trailingAnchor constraintEqualToAnchor:self.trailingAnchor]];
    [NSLayoutConstraint activateConstraints:layoutConstraints];
}

@end
