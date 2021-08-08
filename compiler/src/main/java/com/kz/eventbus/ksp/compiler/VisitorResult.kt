package com.kz.eventbus.ksp.compiler

import com.google.devtools.ksp.symbol.KSFile

sealed interface VisitorResult {

    object Nothing : VisitorResult

    data class Data(
        val subscriberClass: String,
        val funcName: String,
        val eventType: String,
        val annotationArg: AnnotationArg,
        val file: KSFile
    ) : VisitorResult
}

data class AnnotationArg(
    val threadMode: String,
    val sticky: String,
    val priority: String,
)
