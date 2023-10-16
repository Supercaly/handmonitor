package com.handmonitor.recorder.presentation

import com.handmonitor.recorder.R
import com.handmonitor.recorder.data.Action


fun getTitleFromAction(action: Action.Type) =
    when (action) {
        Action.Type.HandWash -> R.string.action_hand_wash
        Action.Type.HandRub -> R.string.action_hand_rub
        Action.Type.Eating -> R.string.action_eat
        Action.Type.TeethBrush -> R.string.action_teeth_brush
        Action.Type.FaceWash -> R.string.action_face_wash
        Action.Type.Writing -> R.string.action_write
        Action.Type.Typing -> R.string.action_type
        Action.Type.Housework -> R.string.action_housework
        else -> R.string.action_other
    }

fun getIconFromAction(action: Action.Type) =
    when (action) {
        Action.Type.HandWash -> R.drawable.ic_wash
        Action.Type.HandRub -> R.drawable.ic_rub
        Action.Type.Eating -> R.drawable.ic_eat
        Action.Type.TeethBrush -> R.drawable.ic_teethbrush
        Action.Type.FaceWash -> R.drawable.ic_facewash
        Action.Type.Writing -> R.drawable.ic_write
        Action.Type.Typing -> R.drawable.ic_keyboard
        Action.Type.Housework -> R.drawable.ic_housework
        else -> R.drawable.ic_cancel
    }
