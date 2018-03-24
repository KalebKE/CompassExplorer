package com.kircherelectronics.compassexplorer.view;

/*
 * AccelerationExplorer
 * Copyright 2017 Kircher Electronics, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by kaleb on 7/7/17.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.kircherelectronics.compassexplorer.R;

public class VectorDrawableButton extends AppCompatTextView {
    public VectorDrawableButton(Context context) {
        super(context);
    }
    public VectorDrawableButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }
    public VectorDrawableButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray attributeArray = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.VectorDrawableButton);

            Drawable drawableLeft = null;
            Drawable drawableRight = null;
            Drawable drawableBottom = null;
            Drawable drawableTop = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawableLeft = attributeArray.getDrawable(R.styleable.VectorDrawableButton_drawableLeftCompat);
                drawableRight = attributeArray.getDrawable(R.styleable.VectorDrawableButton_drawableRightCompat);
                drawableBottom = attributeArray.getDrawable(R.styleable.VectorDrawableButton_drawableBottomCompat);
                drawableTop = attributeArray.getDrawable(R.styleable.VectorDrawableButton_drawableTopCompat);
            } else {
                final int drawableLeftId = attributeArray.getResourceId(R.styleable.VectorDrawableButton_drawableLeftCompat, -1);
                final int drawableRightId = attributeArray.getResourceId(R.styleable.VectorDrawableButton_drawableRightCompat, -1);
                final int drawableBottomId = attributeArray.getResourceId(R.styleable.VectorDrawableButton_drawableBottomCompat, -1);
                final int drawableTopId = attributeArray.getResourceId(R.styleable.VectorDrawableButton_drawableTopCompat, -1);

                if (drawableLeftId != -1)
                    drawableLeft = AppCompatResources.getDrawable(context, drawableLeftId);
                if (drawableRightId != -1)
                    drawableRight = AppCompatResources.getDrawable(context, drawableRightId);
                if (drawableBottomId != -1)
                    drawableBottom = AppCompatResources.getDrawable(context, drawableBottomId);
                if (drawableTopId != -1)
                    drawableTop = AppCompatResources.getDrawable(context, drawableTopId);
            }
            setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
            attributeArray.recycle();
        }
    }
}