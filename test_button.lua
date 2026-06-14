local compose = compose
local setContent = compose.setContent
local Column = compose.foundation.Column
local Row = compose.foundation.Row
local material3 = compose.material3
local Text = material3.Text
local Button = material3.Button

function demo()
    Row {
        content = function()
            Text {
                text = "abc"
            }
            Button {
                onClick = function()
                    print("LUA_ANIM_PRINT: Button Clicked!")
                end,
                content = function()
                    Text {
                        text = "123"
                    }
                end
            }
        end
    }
end

setContent(demo)
