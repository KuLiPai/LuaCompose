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
                        addStatement("val functionCache = mutableMapOf<String, kotlin.reflect.KFunction<*>>()")
                        addStatement("val map = mutableMapOf<String, @androidx.compose.runtime.Composable (Map<String, Any?>, com.kulipai.luacompose.compose.runtime.ComposeScope?) -> Unit>()")
                        for (func in composableFunctions) {
                            val funcName = func.simpleName.asString()
                            val fileName = func.containingFile?.fileName ?: ""
                            val jvmClassName = if (fileName.endsWith(".kt")) fileName.replace(".kt", "Kt") else "${funcName}Kt"
                            val fullClassName = "$packageName.$jvmClassName"
                            
                            val codeBlock = """
                            |map["$funcName"] = { props, childScope ->
                            |    val function = functionCache.getOrPut("$funcName") {
                            |        Class.forName("$fullClassName").kotlin.members.first { it.name == "$funcName" } as kotlin.reflect.KFunction<*>
                            |    }
                            |    val argsMap = mutableMapOf<kotlin.reflect.KParameter, Any?>()
                            |    for (param in function.parameters) {
                            |        val propName = param.name
                            |        if (propName != null && props.containsKey(propName)) {
                            |            argsMap[param] = com.kulipai.luacompose.compose.TypeResolver.resolve(props[propName], param.type)
                            |        } else if (propName == "content" && childScope != null) {
                            |            argsMap[param] = @androidx.compose.runtime.Composable { com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent(childScope, null) }
                            |        }
                            |    }
                            |    function.callBy(argsMap)
                            |}
                            """.trimMargin()
                            addCode("%L\n", codeBlock)
                        }
                        addStatement("return map")
                    }
                    .build()
                )

            val fileSpec = FileSpec.builder("com.kulipai.luacompose.generated", className)
                .addType(typeSpec.build())
                .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true))
    }
}
