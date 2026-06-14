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
            
            val annotation = symbol.annotations.find { 
                it.shortName.asString() == "LuaBridgePackage" 
            } ?: continue
            
            val packageName = annotation.arguments.find { it.name?.asString() == "packageName" }?.value as? String ?: continue
            val category = annotation.arguments.find { it.name?.asString() == "category" }?.value as? String ?: "ui"
            
            logger.warn("Processing LuaBridgePackage for: $packageName, category: $category")
            
            generateBridgeForPackage(resolver, packageName, category)
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

        val className = "${category.replaceFirstChar { it.uppercase() }}GeneratedPlugin"
        
        val fileBuilder = FileSpec.builder("com.kulipai.luacompose.generated", className)
            .addImport("androidx.compose.runtime", "Composable")
        
        val pluginClass = TypeSpec.classBuilder(className)
            // Ideally implements ComposeScriptPlugin, but for simplicity in demo we'll just create a map generator function
            .addFunction(
                FunSpec.builder("getComponents")
                    .returns(MAP.parameterizedBy(
                        STRING,
                        ClassName("kotlin", "Any") // Simplified return type for demo
                    ))
                    .apply {
                        addStatement("val map = mutableMapOf<String, Any>()")
                        for (func in composableFunctions) {
                            val funcName = func.simpleName.asString()
                            addStatement("map[%S] = %S // TODO: Implement callBy logic", funcName, "$packageName.$funcName")
                        }
                        addStatement("return map")
                    }
                    .build()
            )

        fileBuilder.addType(pluginClass.build())
        
        fileBuilder.build().writeTo(codeGenerator, Dependencies(true))
    }
}
