local a = compose.animation.fadeIn()
local b = compose.animation.fadeOut()
local c = a + b
print("Result of a + b is: " .. type(c) .. ", value: " .. tostring(c))
