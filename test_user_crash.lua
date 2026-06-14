local compose = compose
local material3 = compose.material3
local setContent = compose.setContent
local Column = compose.foundation.Column
local Modifier = Modifier
local Arrangement = compose.ui.Arrangement

function AnimationExample1()
    Column {
        modifier = Modifier().fillMaxSize().padding(16),
        verticalArrangement = Arrangement.spacedBy(16),
        content = function()

        end
    }
end

setContent(AnimationExample1)
