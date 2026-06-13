local compose = compose
local material3 = compose.material3
local setContent = compose.setContent
local Column = compose.Column
local Row = compose.Row
local Box = compose.Box
local Text = material3.Text
local Button = material3.Button

local animation = compose.animation
local tween = animation.tween
local AnimatedVisibility = animation.AnimatedVisibility
local fadeIn, fadeOut = animation.fadeIn, animation.fadeOut
local scaleIn, scaleOut = animation.scaleIn, animation.scaleOut
local slideInHorizontally = animation.slideInHorizontally
local slideOutHorizontally = animation.slideOutHorizontally

local state = compose.state

function AnimationExample2()
    local visible = state(true)

    AnimatedVisibility {
        visible = visible.value,
        enter = slideInHorizontally {
            animationSpec = tween(600),
            initialOffsetX = function(fullWidth)
                return fullWidth
            end
        } + fadeIn { animationSpec = tween(600) },
        exit = slideOutHorizontally {
            animationSpec = tween(600),
            targetOffsetX = function(fullWidth)
                return fullWidth
            end
        } + fadeOut { animationSpec = tween(600) },
        content = function()
            Text { text = "SLIDE" }
        end
    }
end

setContent(AnimationExample2)
