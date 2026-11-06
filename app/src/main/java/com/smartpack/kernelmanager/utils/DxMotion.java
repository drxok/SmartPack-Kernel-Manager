package com.smartpack.kernelmanager.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.smartpack.kernelmanager.R;

/**
 * Kernel Dr.xok: movimento no estilo Material 3 Expressive (física de mola).
 */
public class DxMotion {

    /** mola "expressiva": um pouco de quique, rápida */
    public static final float BOUNCY = 0.6f;
    public static final float SNAPPY = 0.85f;

    /** respeita "Escala de animação: desligada" nas opções do desenvolvedor */
    public static boolean enabled() {
        return android.os.Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled();
    }

    public static SpringAnimation spring(View v, DynamicAnimation.ViewProperty prop, float to,
                                         float damping, float stiffness) {
        int key = keyFor(prop);
        SpringAnimation anim = (SpringAnimation) v.getTag(key);
        if (anim == null) {
            anim = new SpringAnimation(v, prop);
            anim.setSpring(new SpringForce());
            v.setTag(key, anim);
        }
        anim.getSpring().setDampingRatio(damping).setStiffness(stiffness);
        if (!enabled()) {
            anim.cancel();
            prop.setValue(v, to);
            return anim;
        }
        anim.animateToFinalPosition(to);
        return anim;
    }

    private static int keyFor(DynamicAnimation.ViewProperty prop) {
        if (prop == DynamicAnimation.SCALE_X) return R.id.dx_spring_scale_x;
        if (prop == DynamicAnimation.SCALE_Y) return R.id.dx_spring_scale_y;
        if (prop == DynamicAnimation.TRANSLATION_X) return R.id.dx_spring_tx;
        if (prop == DynamicAnimation.TRANSLATION_Y) return R.id.dx_spring_ty;
        return R.id.dx_spring_alpha;
    }

    public static void scale(View v, float to, float damping, float stiffness) {
        spring(v, DynamicAnimation.SCALE_X, to, damping, stiffness);
        spring(v, DynamicAnimation.SCALE_Y, to, damping, stiffness);
    }

    /** ao tocar, o elemento "afunda" e volta com mola; não rouba o clique */
    @SuppressLint("ClickableViewAccessibility")
    public static void press(View v) {
        // sem clique o View não recebe o ACTION_UP e ficaria "afundado"
        if (!v.isClickable()) v.setClickable(true);
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    scale(view, 0.95f, SNAPPY, SpringForce.STIFFNESS_MEDIUM);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scale(view, 1f, BOUNCY, SpringForce.STIFFNESS_LOW);
                    break;
            }
            return false;
        });
    }

    /** número que "rola" até o valor novo */
    public static void count(TextView tv, int to, String suffix) {
        Object last = tv.getTag(R.id.dx_count_value);
        int from = last instanceof Integer ? (Integer) last : to;
        tv.setTag(R.id.dx_count_value, to);
        if (from == to || !enabled() || !tv.isShown()) {
            SpringAnimation running = (SpringAnimation) tv.getTag(R.id.dx_count_anim);
            if (running != null) running.cancel();
            tv.setText(to + suffix);
            return;
        }
        SpringAnimation old = (SpringAnimation) tv.getTag(R.id.dx_count_anim);
        if (old != null) old.cancel();
        FloatValueHolder holder = new FloatValueHolder(from);
        SpringAnimation anim = new SpringAnimation(holder);
        anim.setSpring(new SpringForce(to).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_LOW));
        anim.addUpdateListener((a, value, velocity) -> tv.setText(Math.round(value) + suffix));
        anim.addEndListener((a, canceled, value, velocity) -> {
            if (!canceled) tv.setText(to + suffix);
        });
        tv.setTag(R.id.dx_count_anim, anim);
        anim.start();
    }

    /** número inteiro que rola; valores <= 0 mostram "—" */
    public static void countOrDash(TextView tv, int value, String suffix) {
        if (value > 0) {
            count(tv, value, suffix);
        } else {
            SpringAnimation old = (SpringAnimation) tv.getTag(R.id.dx_count_anim);
            if (old != null) old.cancel();
            tv.setTag(R.id.dx_count_value, null);
            tv.setText("—");
        }
    }

    /** filhos aparecem um depois do outro, subindo com mola (entrada de tela) */
    public static void stagger(ViewGroup group) {
        if (group == null || !enabled()) return;
        float dy = 28 * group.getResources().getDisplayMetrics().density;
        for (int i = 0; i < group.getChildCount(); i++) {
            View c = group.getChildAt(i);
            c.setAlpha(0f);
            c.setTranslationY(dy);
            final int delay = Math.min(i, 8) * 45;
            c.postDelayed(() -> {
                c.animate().alpha(1f).setDuration(220).start();
                spring(c, DynamicAnimation.TRANSLATION_Y, 0f, BOUNCY, SpringForce.STIFFNESS_LOW);
            }, delay);
        }
    }

    /** barra que cresce de baixo para cima com mola (0..1) */
    public static void bar(View bar, float fraction) {
        bar.setPivotY(bar.getHeight() > 0 ? bar.getHeight() : 1);
        spring(bar, DynamicAnimation.SCALE_Y, Math.max(0.12f, Math.min(1f, fraction)),
                BOUNCY, SpringForce.STIFFNESS_LOW);
    }

    /** altura (px) que cresce e encolhe com mola */
    public static void height(View v, int targetPx) {
        android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null) return;
        SpringAnimation anim = (SpringAnimation) v.getTag(R.id.dx_spring_height);
        if (!enabled()) {
            if (anim != null) anim.cancel();
            lp.height = targetPx;
            v.setLayoutParams(lp);
            return;
        }
        if (anim == null) {
            if (lp.height <= 0 || !v.isAttachedToWindow()) {
                lp.height = targetPx;
                v.setLayoutParams(lp);
            }
            FloatValueHolder holder = new FloatValueHolder(lp.height);
            anim = new SpringAnimation(holder);
            anim.setSpring(new SpringForce(targetPx).setDampingRatio(BOUNCY)
                    .setStiffness(SpringForce.STIFFNESS_LOW));
            anim.addUpdateListener((a, value, velocity) -> {
                android.view.ViewGroup.LayoutParams p = v.getLayoutParams();
                int h = Math.max(1, Math.round(value));
                if (p.height != h) {
                    p.height = h;
                    v.setLayoutParams(p);
                }
            });
            v.setTag(R.id.dx_spring_height, anim);
        }
        anim.animateToFinalPosition(targetPx);
    }
}
