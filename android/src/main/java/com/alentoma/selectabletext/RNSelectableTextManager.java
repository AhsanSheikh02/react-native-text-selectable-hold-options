package com.rob117.selectabletext;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.MotionEvent;
import android.view.View;
import android.text.Selection;
import android.text.Spannable;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;

import java.util.Map;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.text.ReactTextView;
import com.facebook.react.views.text.ReactTextViewManager;

import java.util.List;
import java.util.ArrayList;

public class RNSelectableTextManager extends ReactTextViewManager {
    public static final String REACT_CLASS = "RNSelectableText";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public ReactTextView createViewInstance(ThemedReactContext context) {
        ReactTextView textView = new ReactTextView(context);

        // Gesture detector to detect single taps
        final GestureDetector gestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSinglePressEvent(textView);
                return true;
            }
        });

        // Set a touch listener to detect single taps and long presses
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false; // Allow other touch events to be handled as well
            }
        });

        // Set a long click listener to handle long press events
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int selectionStart = Selection.getSelectionStart(textView.getText());
                int selectionEnd = Selection.getSelectionEnd(textView.getText());
                if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
                    String selectedText = textView.getText().toString().substring(selectionStart, selectionEnd);
                    onSelectNativeEvent(textView, "longPress", selectedText, selectionStart, selectionEnd);
                    return true;
                }
                return false;
            }
        });

        return textView;
    }

    @ReactProp(name = "menuItems")
    public void setMenuItems(ReactTextView textView, ReadableArray items) {
        List<String> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            result.add(items.getString(i));
        }

        registerSelectionListener(result.toArray(new String[0]), textView);
    }

    public void registerSelectionListener(final String[] menuItems, final ReactTextView view) {
        view.setCustomSelectionActionModeCallback(new Callback() {
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                for (int i = 0; i < menuItems.length; i++) {
                    menu.add(0, i, 0, menuItems[i]);
                }
                return true;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int selectionStart = view.getSelectionStart();
                int selectionEnd = view.getSelectionEnd();
                String selectedText = view.getText().toString().substring(selectionStart, selectionEnd);

                // Dispatch event
                onSelectNativeEvent(view, menuItems[item.getItemId()], selectedText, selectionStart, selectionEnd);

                mode.finish();

                return true;
            }
        });
    }

    public void onSelectNativeEvent(ReactTextView view, String eventType, String content, int selectionStart, int selectionEnd) {
        WritableMap event = Arguments.createMap();
        event.putString("eventType", eventType);
        event.putString("content", content);
        event.putInt("selectionStart", selectionStart);
        event.putInt("selectionEnd", selectionEnd);

        // Dispatch
        ReactContext reactContext = (ReactContext) view.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                view.getId(),
                "topSelection",
                event
        );
    }

    public void onSinglePressEvent(ReactTextView view) {
        WritableMap event = Arguments.createMap();

        // Dispatch
        ReactContext reactContext = (ReactContext) view.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                view.getId(),
                "topSinglePress",
                event
        );
    }

    @Override
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.builder()
                .put("topSelection", MapBuilder.of("registrationName", "onSelection"))
                .put("topSinglePress", MapBuilder.of("registrationName", "onSinglePress"))
                .build();
    }
}
