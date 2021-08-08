package com.kz.eventbus.ksp.compiler

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.meta.SimpleSubscriberInfo
import org.greenrobot.eventbus.meta.SubscriberInfoIndex
import org.greenrobot.eventbus.meta.SubscriberMethodInfo

@AutoService(SymbolProcessorProvider::class)
class EventBusProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = EventBusProcessor(
        environment.options["eventBusIndexPackage"]!!,
        environment.options["eventBusIndexName"]!!,
        environment.codeGenerator,
        environment.logger
    )
}

class EventBusProcessor(
    private val packageName: String,
    private val fileName: String,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        return emptyList<KSAnnotated>().apply {
            resolver.getSymbolsWithAnnotation(Subscribe::class.qualifiedName.toString())
                .map { it.accept(KspVisitor(logger), Unit) }
                .filterIsInstance<VisitorResult.Data>()
                .groupBy { it.subscriberClass to it.file }
                .apply {
                    if (size > 0) {

                        val superinterface = SubscriberInfoIndex::class.java

                        val method = superinterface.methods.first()

                        val name = method.name

                        val parameter = method.parameters.first()

                        val properName = "classInfoMap"

                        val funSpec = FunSpec.builder(name)
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(parameter.name, ClassName("", parameter.type.simpleName).parameterizedBy(TypeVariableName("*")))
                            .addCode("return $properName[${parameter.name}]")
                            .returns(ClassName(method.returnType.packageName, method.returnType.simpleName).copy(nullable = true))
                            .build()

                        val propertySpec = PropertySpec.builder(
                            properName, ClassName("kotlin.collections", "Map").parameterizedBy(
                                ClassName(
                                    "java.lang",
                                    "Class"
                                ).parameterizedBy(TypeVariableName("*")),
                                ClassName("org.greenrobot.eventbus.meta", "SubscriberInfo")
                            )
                        ).build()

                        val typeSpec = TypeSpec.classBuilder(fileName)
                            .addProperty(propertySpec)
                            .addInitializerBlock(buildCodeBlock {
                                addStatement("%L = mapOf(", properName)
                                indent()
                                entries.forEach {
                                    addStatement(
                                        "%L to %T(",
                                        it.key.first,
                                        SimpleSubscriberInfo::class.java
                                    )
                                    indent()
                                    addStatement("%L,", it.key.first)
                                    addStatement("%L,", true)
                                    addStatement("arrayOf(")
                                    indent()
                                    it.value.forEach { data ->
                                        addStatement("%T(", SubscriberMethodInfo::class.java)
                                        indent()
                                        addStatement("%S,",data.funcName)
                                        addStatement("%L,", data.eventType)
                                        addStatement("%L,",data.annotationArg.threadMode)
                                        addStatement("%L,",data.annotationArg.priority)
                                        addStatement("%L,",data.annotationArg.sticky)
                                        unindent()
                                        addStatement("),")
                                    }
                                    unindent()
                                    addStatement("),")
                                    unindent()
                                    addStatement("),")
                                }
                                unindent()
                                addStatement(")")
                            })
                            .addSuperinterface(superinterface)
                            .addFunction(funSpec)
                            .build()

                        val fileSpec = FileSpec.builder(packageName, fileName)
                            .addType(typeSpec)
                            .build()

                        codeGenerator.createNewFile(
                            Dependencies(true, *(entries.map { it.key.second }.toTypedArray())),
                            packageName,
                            fileName
                        ).use { stream ->
                            stream.writer().use { writer ->
                                fileSpec.writeTo(writer)
                            }
                            stream.flush()
                        }
                    }
                }
        }
    }
}
