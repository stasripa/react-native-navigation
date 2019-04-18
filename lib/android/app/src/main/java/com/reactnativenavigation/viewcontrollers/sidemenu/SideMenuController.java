package com.reactnativenavigation.viewcontrollers.sidemenu;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import com.facebook.react.ReactInstanceManager;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativenavigation.parse.Options;
import com.reactnativenavigation.parse.SideMenuOptions;
import com.reactnativenavigation.parse.SideMenuRootOptions;
import com.reactnativenavigation.parse.params.Bool;
import com.reactnativenavigation.presentation.Presenter;
import com.reactnativenavigation.presentation.SideMenuPresenter;
import com.reactnativenavigation.utils.CommandListener;
import com.reactnativenavigation.viewcontrollers.ChildControllersRegistry;
import com.reactnativenavigation.viewcontrollers.ParentController;
import com.reactnativenavigation.viewcontrollers.ViewController;
import com.reactnativenavigation.views.Component;
import com.reactnativenavigation.views.SideMenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class SideMenuController extends ParentController<DrawerLayout> implements DrawerLayout.DrawerListener {

    public static final String KEY_DRAWER_VISIBLE = "drawer_visible";
    private final ReactInstanceManager reactInstanceManager;
    private ViewController center;
    private ViewController left;
    private ViewController right;
    private SideMenuPresenter presenter;
    private int leftMenuWidth = MATCH_PARENT;
    private int rightMenuWidth = MATCH_PARENT;

    public SideMenuController(Activity activity,
                              ChildControllersRegistry childRegistry,
                              String id,
                              Options initialOptions,
                              SideMenuPresenter sideMenuOptionsPresenter,
                              Presenter presenter,
                              ReactInstanceManager reactInstanceManager) {
        super(activity, childRegistry, id, presenter, initialOptions);
        this.presenter = sideMenuOptionsPresenter;
        this.reactInstanceManager = reactInstanceManager;
    }

    @Override
    protected ViewController getCurrentChild() {
	    if (getView().isDrawerOpen(Gravity.LEFT)) {
            return left;
        } else if (getView().isDrawerOpen(Gravity.RIGHT)) {
            return right;
        }
        return center;
    }

    @NonNull
	@Override
	protected DrawerLayout createView() {
        DrawerLayout sideMenu = new SideMenu(getActivity());
        presenter.bindView(sideMenu);
        sideMenu.addDrawerListener(this);
        return sideMenu;
	}

    @Override
    public void sendOnNavigationButtonPressed(String buttonId) {
        center.sendOnNavigationButtonPressed(buttonId);
    }

    @NonNull
	@Override
	public Collection<ViewController> getChildControllers() {
		ArrayList<ViewController> children = new ArrayList<>();
		if (center != null) children.add(center);
		if (left != null) children.add(left);
		if (right != null) children.add(right);
		return children;
	}

    @Override
    public void applyChildOptions(Options options, Component child) {
        super.applyChildOptions(options, child);
        presenter.applyChildOptions(resolveCurrentOptions());
        performOnParentController(parentController ->
                ((ParentController) parentController).applyChildOptions(this.options, child)
        );
        this.updateControllers(options.sideMenuRootOptions);
    }

    @Override
    public void mergeChildOptions(Options options, ViewController childController, Component child) {
        super.mergeChildOptions(options, childController, child);
        presenter.mergeChildOptions(options.sideMenuRootOptions);
        this.updateControllers(options.sideMenuRootOptions);
        performOnParentController(parentController ->
                ((ParentController) parentController).mergeChildOptions(options.copy().clearSideMenuOptions(), childController, child)
        );
    }

    @Override
    public void mergeOptions(Options options) {
        super.mergeOptions(options);
        presenter.mergeOptions(options.sideMenuRootOptions);
        this.updateControllers(options.sideMenuRootOptions);
    }

    @Override
    public Options resolveCurrentOptions() {
        Options options = super.resolveCurrentOptions();
        if (getView().isDrawerOpen(Gravity.LEFT) || getView().isDrawerOpen(Gravity.RIGHT)) {
            options = options.mergeWith(center.resolveCurrentOptions());
        }
        return options;
    }

    //For onDrawerOpened and onDrawerClosed :
    //Merge the options to the current state, if this happened due to a gesture we need to
    //update the option state

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        DeviceEventManagerModule.RCTDeviceEventEmitter emitter = reactInstanceManager.getCurrentReactContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        emitter.emit(KEY_DRAWER_VISIBLE, true);
//        ViewController view = this.getMatchingView(drawerView);
//        view.mergeOptions(this.getOptionsWithVisability(this.viewIsLeft(drawerView), true));
//        view.onViewAppeared();
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        DeviceEventManagerModule.RCTDeviceEventEmitter emitter = reactInstanceManager.getCurrentReactContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        emitter.emit(KEY_DRAWER_VISIBLE, false);
//        ViewController view = this.getMatchingView(drawerView);
//        view.mergeOptions(this.getOptionsWithVisability(this.viewIsLeft(drawerView), false));
//        view.onViewDisappear();
    }

    @Override
    public boolean handleBack(CommandListener listener) {
        return presenter.handleBack() || center.handleBack(listener) || super.handleBack(listener);
    }

    public void setCenterController(ViewController centerController) {
		this.center = centerController;
		View childView = centerController.getView();
		getView().addView(childView);
	}

    public void setLeftController(ViewController controller) {
        this.left = controller;
        this.updateLeftController(controller);
    }

    public void setRightController(ViewController controller) {
        this.right = controller;
        this.updateRightController(controller);
    }

    private void updateControllers (SideMenuRootOptions options) {
        if (options.left.width.hasValue()) {
            leftMenuWidth = options.left.width.get();
        }
        if (options.right.width.hasValue()) {
            rightMenuWidth = options.right.width.get();
        }
        this.updateRightController(right);
        this.updateLeftController(left);
    }

    private void updateLeftController(ViewController controller) {
        if (controller == null) return;
        this.updateView(controller.getView(), options.sideMenuRootOptions.left, Gravity.LEFT);
    }

    private void updateRightController(ViewController controller) {
        if (controller == null) return;
        this.updateView(controller.getView(), options.sideMenuRootOptions.right, Gravity.RIGHT);
    }

    private void updateView(View view, SideMenuOptions options, int gravity) {
        int width = this.getWidth(gravity);
        int height = this.getHeight(options);
        if (getView().indexOfChild(view) != -1) {
            view.setLayoutParams(new LayoutParams(width, height, gravity));
        } else {
            getView().addView(view, new LayoutParams(width, height, gravity));
        }
    }

    private int getWidth(int gravity) {
        int width = MATCH_PARENT;
        int layoutWidth = gravity == Gravity.LEFT ? leftMenuWidth : rightMenuWidth;
        if (layoutWidth != width) {
            width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, layoutWidth, Resources.getSystem().getDisplayMetrics());
        }
        return width;
    }

    private int getHeight(SideMenuOptions sideMenuOptions) {
        int height = MATCH_PARENT;
        if (sideMenuOptions.height.hasValue()) {
            height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sideMenuOptions.height.get(), Resources.getSystem().getDisplayMetrics());
        }
        return height;
    }

    private ViewController getMatchingView (View drawerView) {
        return this.viewIsLeft(drawerView) ? left : right;
    }

    private boolean viewIsLeft (View drawerView) {
        return (left != null && drawerView.equals(left.getView()));
    }

    private Options getOptionsWithVisability ( boolean isLeft, boolean visible ) {
        Options options = new Options();
        if (isLeft) {
            options.sideMenuRootOptions.left.visible = new Bool(visible);
        } else {
            options.sideMenuRootOptions.right.visible = new Bool(visible);
        }
        return options;
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
