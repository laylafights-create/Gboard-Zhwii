package dev.jason.gboardpatches.patches.gboard.features.keypresssound

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import dev.jason.gboardpatches.patches.gboard.shared.findMutableMethodOrThrow
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.gboard.shared.isMethodReference
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardKeypressSoundBytecodePatch = bytecodePatch(
    description = "在鍵盤根 View 的 dispatchTouchEvent 入口，於手指按下時播放按鍵音效。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        findMutableMethodOrThrow(
            classType = INPUT_VIEW_CLASS,
            name = DISPATCH_TOUCH_EVENT_NAME,
            returnType = DISPATCH_TOUCH_EVENT_RETURN,
            parameterTypes = listOf(MOTION_EVENT_TYPE),
        ).applyKeypressSoundTouchObserver()
    }
}

/** Calls the runtime with (this, event) before the view's own touch handling; idempotent. */
internal fun MutableMethod.applyKeypressSoundTouchObserver() {
    val abi = RuntimeAbiCatalog.abi(RuntimeCallId.KEYPRESS_SOUND_RUNTIME_ON_TOUCH_EVENT)
    val instructions = implementation?.instructions
        ?: error("No instructions in $definingClass->$name")
    val existing = instructions.count { it.isMethodReference(abi.reference) }
    if (existing > 0) {
        check(existing == 1) { "Malformed keypress sound observer in $definingClass->$name" }
        return
    }
    addInstructions(
        0,
        RuntimeCallEmitter.invoke(RuntimeCallId.KEYPRESS_SOUND_RUNTIME_ON_TOUCH_EVENT, "p0 .. p1"),
    )
}

private const val INPUT_VIEW_CLASS =
    "Lcom/google/android/libraries/inputmethod/inputview/InputView;"
private const val DISPATCH_TOUCH_EVENT_NAME = "dispatchTouchEvent"
private const val MOTION_EVENT_TYPE = "Landroid/view/MotionEvent;"
private const val DISPATCH_TOUCH_EVENT_RETURN = "Z"
