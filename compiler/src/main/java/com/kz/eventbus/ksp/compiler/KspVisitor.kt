package com.kz.eventbus.ksp.compiler

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import org.greenrobot.eventbus.Subscribe

class KspVisitor(private val logger: KSPLogger) : KSDefaultVisitor<Unit, VisitorResult>() {

    override fun defaultHandler(node: KSNode, data: Unit): VisitorResult = VisitorResult.Nothing

    override fun visitFunctionDeclaration(
        function: KSFunctionDeclaration,
        data: Unit
    ): VisitorResult = VisitorResult.Data(
        function.subscriberName(),
        function.funcName(),
        function.paramType(),
        function.annotationArgs(),
        function.containingFile()
    )

    private fun KSFunctionDeclaration.containingFile() =
        parentDeclaration!!.containingFile!!

    private fun KSFunctionDeclaration.subscriberName(): String =
        "${parentDeclaration!!.qualifiedName!!.asString()}::class.java"

    private fun KSFunctionDeclaration.paramType() =
        "${parameters.first().type.asTypeName()}::class.java"

    private fun KSFunctionDeclaration.funcName() =
        simpleName.getShortName()

    private fun KSFunctionDeclaration.annotationArgs(): AnnotationArg =
        annotations.filter { it.shortName.getShortName() == Subscribe::class.simpleName }
            .first()
            .arguments
            .let { list ->
                var threadMode = "org.greenrobot.eventbus.ThreadMode.POSTING"
                var sticky = "false"
                var priority = "0"
                list.forEach { arg ->
                    when (arg.name!!.asString()) {
                        "threadMode" -> threadMode = arg.value.toString()
                        "sticky" -> sticky = arg.value.toString()
                        "priority" -> priority = arg.value.toString()
                    }
                }
                AnnotationArg(
                    threadMode,
                    sticky,
                    priority,
                )
            }

    private fun KSTypeReference.asTypeName(): String =
        (resolve().declaration as KSClassDeclaration).asClassName()

    private fun KSClassDeclaration.asClassName(): String =
        qualifiedName!!.asString()
}
