package com.kulipai.luacompose.compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.KspExperimental
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo

class LuaComposeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val packageSymbols = resolver.getSymbolsWithAnnotation("com.kulipai.luacompose.annotations.LuaBridgePackage")
        
        for (symbol in packageSymbols) {
            if (symbol !is KSClassDeclaration) continue
            
            val bridgeAnnotations = symbol.annotations.filter { 
                it.shortName.asString() == "LuaBridgePackage" 
            }
            
            for (annotation in bridgeAnnotations) {
                val packageName = annotation.arguments.find { it.name?.asString() == "packageName" }?.value as? String ?: continue
                val category = annotation.arguments.find { it.name?.asString() == "category" }?.value as? String ?: "ui"
                
                logger.warn("Processing LuaBridgePackage for: $packageName, category: $category")
                
                generateBridgeForPackage(resolver, packageName, category)
            }
        }
        
        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun generateBridgeForPackage(resolver: Resolver, packageName: String, category: String) {
        // Fetch all declarations from the given package
        val decls = resolver.getDeclarationsFromPackage(packageName)
        
        // Filter out functions that have @Composable annotation
        val composableFunctions = decls.filterIsInstance<KSFunctionDeclaration>()
            .filter { func ->
                func.annotations.any { it.shortName.asString() == "Composable" } &&
                func.returnType?.resolve()?.declaration?.simpleName?.asString() == "Unit" &&
                func.isPublic()
            }
            .toList()

        if (composableFunctions.isEmpty()) {
            logger.warn("No Composable functions found in package $packageName")
            return
        }

        val className = category.replaceFirstChar { it.uppercase() } + "GeneratedPlugin"
        
        val typeSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName("com.kulipai.luacompose.compose.runtime", "ComposeScriptPlugin"))
            .addProperty(
                PropertySpec.builder("namespace", STRING.copy(nullable = true), KModifier.OVERRIDE)
                    .initializer("%S", category)
                    .build()
            )
            .addFunction(
                FunSpec.builder("injectGlobals")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("scriptTable", ClassName("com.kulipai.luacompose.compose.script", "ScriptTable"))
                    .build()
            )
            .addFunction(
                FunSpec.builder("getComponents")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(MAP.parameterizedBy(
                        STRING,
                        LambdaTypeName.get(
                            parameters = arrayOf(
                                ParameterSpec.unnamed(MAP.parameterizedBy(STRING, ANY.copy(nullable = true))),
                                ParameterSpec.unnamed(ClassName("com.kulipai.luacompose.compose.runtime", "ComposeScope").copy(nullable = true))
                            ),
                            returnType = UNIT
                        ).copy(annotations = listOf(AnnotationSpec.builder(ClassName("androidx.compose.runtime", "Composable")).build()))
                    ))
                    .apply {
                        addStatement("val functionCache = mutableMapOf<String, java.lang.reflect.Method>()")
                        addStatement("val map = mutableMapOf<String, @androidx.compose.runtime.Composable (Map<String, Any?>, com.kulipai.luacompose.compose.runtime.ComposeScope?) -> Unit>()")
                        for (func in composableFunctions) {
                            val funcName = func.simpleName.asString()
                            val fileName = func.containingFile?.fileName ?: ""
                            val jvmClassName = if (fileName.endsWith(".kt")) fileName.replace(".kt", "Kt") else "${funcName}Kt"
                            val fullClassName = "$packageName.$jvmClassName"
                            
                            val numParams = func.parameters.size
                            val firstParamName = func.parameters.firstOrNull()?.type?.resolve()?.declaration?.simpleName?.asString()
                            val firstParamCheck = if (firstParamName != null) {
                                "it.parameterTypes.getOrNull(0)?.simpleName == \"$firstParamName\""
                            } else { "true" }
                            
                            val codeBlock = StringBuilder()
                            codeBlock.append("""
                            |map["$funcName"] = { props, childScope ->
                            |    val method = functionCache.getOrPut("$funcName") {
                            |        Class.forName("$fullClassName").declaredMethods.first { 
                            |            (it.name == "$funcName" || it.name.startsWith("$funcName-") || it.name.startsWith("${funcName}_")) && 
                            |            it.parameterTypes.any { pt -> pt.name == "androidx.compose.runtime.Composer" } &&
                            |            $firstParamCheck
                            |        }
                            |    }
                            |    val composer = androidx.compose.runtime.currentComposer
                            |    val args = arrayOfNulls<Any>(method.parameterTypes.size)
                            |    val composerIndex = method.parameterTypes.indexOfFirst { it.name == "androidx.compose.runtime.Composer" }
                            |    args[composerIndex] = composer
                            |    
                            |    val defaultBitmasks = IntArray(10) // generous size
                            |    val realParams = $numParams
                            |
                            """.trimMargin())

                            for ((i, param) in func.parameters.withIndex()) {
                                val pName = param.name?.asString()
                                val isComposable = param.annotations.any { it.shortName.asString() == "Composable" }
                                codeBlock.append("""
                            |    val val_$pName = props["$pName"]
                            |    if (val_$pName is com.kulipai.luacompose.compose.script.ScriptFunction && method.parameterTypes[$i].name.startsWith("kotlin.jvm.functions.Function")) {
                            |        val scopeToUse = if ($isComposable) com.kulipai.luacompose.compose.runtime.ComposeBridge.getActiveScope()?.getOrCreateChildScope(val_$pName) else null
                            |        args[$i] = com.kulipai.luacompose.compose.runtime.FunctionWrappers.wrap(scopeToUse, val_$pName, method.parameterTypes[$i].name, $isComposable)
                            |    } else if (props.containsKey("$pName")) {
                            |        args[$i] = com.kulipai.luacompose.compose.TypeResolver.resolve(val_$pName, method.parameterTypes[$i])
                            |    } else if ("$pName" == "content" && childScope != null) {
                            |        args[$i] = com.kulipai.luacompose.compose.runtime.FunctionWrappers.wrap(childScope, null, method.parameterTypes[$i].name, true)
                            |    } else {
                            |        val defaultMaskIndex = $i / 31
                            |        defaultBitmasks[defaultMaskIndex] = defaultBitmasks[defaultMaskIndex] or (1 shl ($i % 31))
                            |        args[$i] = when (method.parameterTypes[$i]) {
                            |            Boolean::class.javaPrimitiveType -> false
                            |            Int::class.javaPrimitiveType -> 0
                            |            Float::class.javaPrimitiveType -> 0f
                            |            Long::class.javaPrimitiveType -> 0L
                            |            Double::class.javaPrimitiveType -> 0.0
                            |            Short::class.javaPrimitiveType -> 0.toShort()
                            |            Byte::class.javaPrimitiveType -> 0.toByte()
                            |            Char::class.javaPrimitiveType -> '\u0000'
                            |            else -> null
                            |        }
                            |    }
                            """.trimMargin() + "\n")
                            }

                            codeBlock.append("""
                            |    val defaultParamsCount = Math.ceil(realParams / 31.0).toInt()
                            |    val intIndices = mutableListOf<Int>()
                            |    for (i in realParams until method.parameterTypes.size) {
                            |        if (i != composerIndex && method.parameterTypes[i] == Int::class.javaPrimitiveType) {
                            |            intIndices.add(i)
                            |        }
                            |    }
                            |    val actualDefaultCount = Math.min(defaultParamsCount, intIndices.size)
                            |    val defaultIndices = intIndices.takeLast(actualDefaultCount)
                            |    val changedIndices = intIndices.dropLast(actualDefaultCount)
                            |    
                            |    for (idx in changedIndices) {
                            |        args[idx] = 0
                            |    }
                            |    for ((i, idx) in defaultIndices.withIndex()) {
                            |        args[idx] = defaultBitmasks[i]
                            |    }
                            |    
                            |    method.isAccessible = true
                            |    method.invoke(null, *args)
                            |}
                            """.trimMargin())
                            
                            addCode("%L\n", codeBlock.toString())
                        }
                        addStatement("return map")
                    }
                    .build()
                )

            val fileSpec = FileSpec.builder("com.kulipai.luacompose.generated", className)
                .addImport("kotlin.reflect.jvm", "kotlinFunction")
                .addType(typeSpec.build())
                .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true))
    }
}
