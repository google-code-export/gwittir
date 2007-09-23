/*
 * SoftScrollPanel.java
 *
 * Created on August 16, 2007, 9:11 AM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.totsp.gwittir.client.fx.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollListener;
import com.google.gwt.user.client.ui.ScrollListenerCollection;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesScrollEvents;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.totsp.gwittir.client.beans.Introspectable;
import com.totsp.gwittir.client.fx.AnimationFinishedCallback;
import com.totsp.gwittir.client.fx.MutationStrategy;
import com.totsp.gwittir.client.fx.PositionWrapper;
import com.totsp.gwittir.client.fx.PropertyAnimator;
import com.totsp.gwittir.client.ui.*;
import com.totsp.gwittir.client.util.UnitsParser;


/**
 *
 * @author cooper
 */
public class SoftScrollArea extends Composite implements HasWidget,
    Introspectable, SourcesScrollEvents {
    private PositionWrapper position;
    PropertyAnimator horizontalAnimator;
    PropertyAnimator scrollAnimator;
    private ScrollListenerCollection listeners = new ScrollListenerCollection();
    private SimplePanel base = new SimplePanel();
    private Widget widget;
    private boolean limit = true;
    private int defaultAnimationDuration = 500;
    private int horizOverride = Integer.MIN_VALUE;
    private int scrollOverride = Integer.MIN_VALUE;

    /** Creates a new instance of SoftScrollPanel */
    public SoftScrollArea() {
        DOM.setStyleAttribute(base.getElement(), "overflow", "hidden");
        super.initWidget(base);
        this.setStyleName("gwittir-SoftScrollArea");
    }

    public SoftScrollArea(boolean limit) {
        this();
        this.limit = limit;
    }

    public void addScrollListener(ScrollListener listener) {
        this.listeners.add(listener);
    }

    public void animateToHorizontalScrollPosition(int newPosition,
        MutationStrategy integerStrategy, int duration) {
        int max = widget.getOffsetWidth() - base.getOffsetWidth();

        if(limit & (newPosition > max)) {
            newPosition = max;
        }

        if(limit & (newPosition < 0)) {
            newPosition = 0;
        }

        if(this.horizontalAnimator != null) {
            this.horizontalAnimator.cancel();
        }

        horizontalAnimator = new PropertyAnimator(this,
                "horizontalScrollPosition", new Integer(newPosition),
                integerStrategy, duration,
                new AnimationFinishedCallback() {
                    public void onFinish(PropertyAnimator animator) {
                        horizOverride = Integer.MIN_VALUE;
                        horizontalAnimator = null;
                    }

                    public void onFailure(PropertyAnimator animator, Exception e) {
                        horizOverride = Integer.MIN_VALUE;
                        horizontalAnimator = null;
                    }
                });
        horizOverride = newPosition;
        this.horizontalAnimator.start();
    }

    public void animateToHorizontalScrollPosition(int newPosition) {
        this.animateToHorizontalScrollPosition(newPosition,
            MutationStrategy.INTEGER_SINOIDAL,
            this.getDefaultAnimationDuration());
    }

    public void animateToScrollPosition(int newPosition,
        MutationStrategy integerStrategy, int duration) {
        int max = widget.getOffsetHeight() - base.getOffsetHeight();

        if(limit & (newPosition > max)) {
            newPosition = max;
        }

        if(limit & (newPosition < 0)) {
            newPosition = 0;
        }

        if(this.scrollAnimator != null) {
            scrollAnimator.cancel();
        }

        this.scrollAnimator = new PropertyAnimator(this, "scrollPosition",
                new Integer(newPosition), integerStrategy, duration,
                new AnimationFinishedCallback() {
                    public void onFinish(PropertyAnimator animator) {
                        scrollOverride = Integer.MIN_VALUE;
                        scrollAnimator = null;
                    }

                    public void onFailure(PropertyAnimator animator, Exception e) {
                        scrollOverride = Integer.MIN_VALUE;
                        scrollAnimator = null;
                    }
                });
        this.scrollOverride = newPosition;
        this.scrollAnimator.start();
    }

    public void animateToScrollPosition(int newPosition) {
        this.animateToScrollPosition(newPosition,
            MutationStrategy.INTEGER_SINOIDAL,
            this.getDefaultAnimationDuration());
    }

    public void ensureVisible(UIObject uiobject) {
        int scroll = this.ensureVisiblePosition(widget.getElement(),
                uiobject.getElement());
        int horiz = this.ensureVisibleHortizontalPosition(widget.getElement(),
                uiobject.getElement());
        GWT.log(scroll + " " + horiz, null);
        this.setScrollPosition(scroll);
        this.setHorizontalScrollPosition(horiz);
    }

    public void ensureVisibleAnimated(UIObject uiobject) {
        int scroll = this.ensureVisiblePosition(widget.getElement(),
                uiobject.getElement());
        int horiz = this.ensureVisibleHortizontalPosition(widget.getElement(),
                uiobject.getElement());
        GWT.log(scroll + " " + horiz, null);
        this.animateToScrollPosition(scroll);
        this.animateToHorizontalScrollPosition(horiz);
    }

    private native int ensureVisibleHortizontalPosition(Element position,
        Element e) /*-{
    if (!e)
    return;

    var item = e;
    var realOffset = 0;
    while (item && (item != position)) {
    realOffset += item.offsetLeft;
    item = item.offsetParent;
    }
    return realOffset; //- position.offsetWidth / 2;
    }-*/;

    private native int ensureVisiblePosition(Element position, Element e) /*-{
    if (!e)
    return;

    var item = e;
    var realOffset = 0;
    while (item && (item != position)) {
    realOffset += item.offsetTop;
    item = item.offsetParent;
    }
    return -1 * (realOffset - position.offsetHeight/2 + e.offsetHeight / 2);
    }-*/;

    public int getDefaultAnimationDuration() {
        return defaultAnimationDuration;
    }

    public int getHorizontalScrollPosition() {
        return (this.horizOverride == Integer.MIN_VALUE)
        ? (-1 * UnitsParser.parse(position.getLeft()).value) : this.horizOverride;
    }

    public int getMaxHorizontalScrollPosition() {
        int max = limit ? (widget.getOffsetWidth() - base.getOffsetWidth())
                        : Integer.MAX_VALUE;

        return (max < 0) ? 0 : max;
    }

    public int getMaxScrollPosition() {
        int max = limit ? (widget.getOffsetHeight() - base.getOffsetHeight())
                        : Integer.MAX_VALUE;

        return (max < 0) ? 0 : max;
    }

    public int getScrollPosition() {
        return (this.scrollOverride == Integer.MIN_VALUE)
        ? (-1 * UnitsParser.parse(position.getTop()).value) : this.scrollOverride;
    }

    public Widget getWidget() {
        return widget;
    }

    protected void onAttach() {
        super.onAttach();
        this.setScrollPosition(this.getScrollPosition());
    }

    public void pageDown() {
        this.setScrollPosition(this.getScrollPosition()
            + this.base.getOffsetHeight());
    }

    public void pageDownAnimated() {
        this.animateToScrollPosition(this.getScrollPosition()
            + this.base.getOffsetHeight());
    }

    public void pageLeft() {
        this.setHorizontalScrollPosition(this.getHorizontalScrollPosition()
            - this.base.getOffsetWidth());
    }

    public void pageLeftAnimated() {
        this.animateToHorizontalScrollPosition(this.getHorizontalScrollPosition()
            - this.base.getOffsetWidth());
    }

    public void pageRight() {
        this.setHorizontalScrollPosition(this.getHorizontalScrollPosition()
            + this.base.getOffsetWidth());
    }

    public void pageRightAnimated() {
        this.animateToHorizontalScrollPosition(this.getHorizontalScrollPosition()
            + this.base.getOffsetWidth());
    }

    public void pageUp() {
        this.setScrollPosition(this.getScrollPosition()
            - this.base.getOffsetHeight());
    }

    public void pageUpAnimated() {
        this.animateToScrollPosition(this.getScrollPosition()
            - this.base.getOffsetHeight());
    }

    public void removeScrollListener(ScrollListener listener) {
        this.listeners.add(listener);
    }

    public void setDefaultAnimationDuration(int defaultAnimationDuration) {
        this.defaultAnimationDuration = defaultAnimationDuration;
    }

    public void setHorizontalScrollPosition(int position) {
        int max = widget.getOffsetWidth() - base.getOffsetWidth();

        if(limit & (position > max)) {
            position = max;
        }

        if(limit & (position < 0)) {
            position = 0;
        }

        this.position.setLeft((-1 * position) + "px");
        this.listeners.fireScroll(this, this.getHorizontalScrollPosition(),
            this.getScrollPosition());
    }

    public void setScrollPosition(int position) {
        int max = widget.getOffsetHeight() - base.getOffsetHeight();

        if(limit & (position > max)) {
            position = max;
        }

        if(limit & (position < 0)) {
            position = 0;
        }

        this.position.setTop((-1 * position) + "px");
        this.listeners.fireScroll(this, this.getHorizontalScrollPosition(),
            this.getScrollPosition());
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
        this.position = new PositionWrapper(widget);
        this.position.setPosition("relative");
        this.position.setTop("0px");
        this.position.setLeft("0px");
        this.base.setWidget(widget);
    }
}