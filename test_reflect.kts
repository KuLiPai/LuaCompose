import kotlin.reflect.jvm.kotlinFunction

val clazz = Class.forName("androidx.compose.material3.TextKt")
println(clazz.methods.filter { it.name == "Text" }.map { it.kotlinFunction })
