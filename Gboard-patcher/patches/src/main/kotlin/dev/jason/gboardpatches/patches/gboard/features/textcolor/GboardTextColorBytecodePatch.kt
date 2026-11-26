package dev.jason.gboardpatches.patches.gboard.features.textcolor

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import dev.jason.gboardpatches.patches.gboard.shared.findMutableMethodOrThrow
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.gboard.shared.isMethodReference
import dev.jason.gboardpatches.patches.gboard.shared.isOpcode
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardTextColorBytecodePatch = bytecodePatch(
    description = "在 Gboard 按鍵文字 View 的 onDraw 取色後交給 Custom Text Color runtime，只改文字色，不碰背景。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        findMutableMethodOrThrow(
            classType = TEXT_VIEW_CLASS,
            name = ON_DRAW_NAME,
            returnType = "V",
            parameterTypes = listOf(CANVAS_TYPE),
        ).applyTextColorDelegate()
    }
}

/**
 * Rewrites the value `getCurrentTextColor()` produced, right before the paint receives it, so the
 * delegate can only ever change the text color. Register count is unchanged: the delegate reuses
 * the register that already holds the color.
 */
internal fun MutableMethod.applyTextColorDelegate() {
    val abi = RuntimeAbiCatalog.abi(RuntimeCallId.TEXT_COLOR_RUNTIME_RESOLVE_FOR_VIEW)
    val implementation = implementation ?: error("No instructions in $definingClass->$name")
    val instructions = implementation.instructions.toList()
    val existing = instructions.count { it.isMethodReference(abi.reference) }
    if (existing > 0) {
        check(existing == 1) { "Duplicate text color delegates in $definingClass->$name" }
        return
    }
    check(accessFlags and AccessFlags.STATIC.value == 0) { "$name must be an instance method" }
    check(implementation.registerCount <= MAX_NON_RANGE_REGISTERS) {
        "Unexpected register count in $definingClass->$name: ${implementation.registerCount}"
    }

    val colorCalls = instructions.indices.filter { index ->
        val reference = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
        reference != null && reference.name == GET_CURRENT_TEXT_COLOR &&
            reference.returnType == "I" && reference.parameterTypes.isEmpty()
    }
    check(colorCalls.size == 1) {
        "Expected exactly one getCurrentTextColor in $definingClass->$name, found ${colorCalls.size}"
    }
    val moveResultIndex = colorCalls.single() + 1
    val moveResult = instructions.getOrNull(moveResultIndex)
    check(moveResult != null && moveResult.isOpcode("MOVE_RESULT") && moveResult is OneRegisterInstruction) {
        "getCurrentTextColor result is not consumed by move-result in $definingClass->$name"
    }
    val colorRegister = (moveResult as OneRegisterInstruction).registerA
    check(colorRegister < MAX_NON_RANGE_REGISTERS) { "Color register v$colorRegister is out of range" }

    // The color must flow into TextPaint.setColor unchanged, otherwise this is the wrong spot.
    val setColorIndex = (moveResultIndex + 1 until instructions.size).firstOrNull { index ->
        val instruction = instructions[index]
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        // R8 may rebind the call from TextPaint to the Paint class that declares setColor.
        reference != null && reference.definingClass in PAINT_OWNER_TYPES &&
            reference.name == SET_COLOR && reference.parameterTypes == listOf("I") &&
            instruction is FiveRegisterInstruction &&
            instruction.registerCount == 2 && instruction.registerD == colorRegister
    }
    check(setColorIndex != null) { "No TextPaint.setColor(color) follows getCurrentTextColor in $name" }
    check((moveResultIndex + 1 until setColorIndex).none { index ->
        val instruction = instructions[index]
        instruction is OneRegisterInstruction && instruction.opcode.setsRegister() &&
            instruction.registerA == colorRegister
    }) { "Color register is overwritten before TextPaint.setColor in $name" }

    addInstructions(
        moveResultIndex + 1,
        """
            ${RuntimeCallEmitter.invoke(RuntimeCallId.TEXT_COLOR_RUNTIME_RESOLVE_FOR_VIEW, "p0, v$colorRegister")}
            move-result v$colorRegister
        """.trimIndent(),
    )
}

private const val TEXT_VIEW_CLASS =
    "Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;"
private const val ON_DRAW_NAME = "onDraw"
private const val CANVAS_TYPE = "Landroid/graphics/Canvas;"
private const val GET_CURRENT_TEXT_COLOR = "getCurrentTextColor"
private val PAINT_OWNER_TYPES = setOf("Landroid/text/TextPaint;", "Landroid/graphics/Paint;")
private const val SET_COLOR = "setColor"
private const val MAX_NON_RANGE_REGISTERS = 16
