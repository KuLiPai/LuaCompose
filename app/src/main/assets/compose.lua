-- compose.lua
local compose = {}

local activeScopeStack = {}
local activeNodeStack = {}

local function currentScope()
  return activeScopeStack[#activeScopeStack]
end

local function currentNode()
  return activeNodeStack[#activeNodeStack]
end

-- 1. 响应式状态封装
local State = {}
State.__index = function(t, k)
  if k == "value" then
    return t:get()
  end
  return rawget(t, k)
end
State.__newindex = function(t, k, v)
  if k == "value" then
    t:set(v)
  else
    rawset(t, k, v)
  end
end
State.__tostring = function(t)
  return tostring(t:get())
end

local function createState(initialValue, scope)
  -- 实例化 Kotlin 侧的 LuaState
  local s = {
    _isState = true,
    javaState = luajava.newInstance("com.kulipai.luacompose.LuaState", initialValue, scope),
  }
  
  function s:get()
    local current = currentScope()
    if current then
      self.javaState.registerDependency(current.javaScope)
    end
    return self.javaState.get()
  end
  
  function s:set(newValue)
    self.javaState.set(newValue)
  end
  
  setmetatable(s, State)
  return s
end

-- 2. 作用域封装
local Scope = {}
Scope.__index = Scope

function Scope.new(contentFunc, javaScope)
  local self = setmetatable({}, Scope)
  self.contentFunc = contentFunc
  self.javaScope = javaScope
  self.states = {}
  self.statesCount = 0
  return self
end

function Scope:reset()
  self.statesCount = 0
end

function Scope:state(initialValue)
  self.statesCount = self.statesCount + 1
  local idx = self.statesCount
  if not self.states[idx] then
    self.states[idx] = createState(initialValue, self.javaScope)
  end
  return self.states[idx]
end

function Scope:run()
  table.insert(activeScopeStack, self)
  self:reset()
  
  local nodes = {}
  table.insert(activeNodeStack, nodes)
  
  -- 执行布局逻辑
  local success, err = pcall(self.contentFunc, self)
  if not success then
    print("Execute scope error: " .. tostring(err))
    table.insert(nodes, {
      type = "Text",
      props = { text = "Lua Error: " .. tostring(err), color = "#ff0000" },
      children = {}
    })
  end
  
  table.remove(activeNodeStack)
  table.remove(activeScopeStack)
  
  return nodes
end

-- 3. 通用组件生成器 (递归收集子布局，不创建额外子 Scope)
local function defineComponent(name)
  return function(props)
    props = props or {}
    
    if type(props) == "string" then
      props = { text = props }
    elseif type(props) == "function" then
      props = { content = props }
    end
    
    local node = {
      type = name,
      props = {},
      children = {}
    }
    
    local content = props.content
    props.content = nil
    
    -- 拷贝属性
    for k, v in pairs(props) do
      node.props[k] = v
    end
    
    -- 若有子布局函数，立即执行以递归填充 children
    if type(content) == "function" then
      table.insert(activeNodeStack, node.children)
      local success, err = pcall(content)
      if not success then
        print("Execute child content error: " .. tostring(err))
        table.insert(node.children, {
          type = "Text",
          props = { text = "Render Error: " .. tostring(err), color = "#ff0000" },
          children = {}
        })
      end
      table.remove(activeNodeStack)
    end
    
    -- 将自己挂载到父节点
    local parentNodes = currentNode()
    if parentNodes then
      table.insert(parentNodes, node)
    end
    
    return node
  end
end

-- 4. 注册内置常用组件
compose.Column = defineComponent("Column")
compose.Row = defineComponent("Row")
compose.Box = defineComponent("Box")
compose.Text = defineComponent("Text")
compose.Button = defineComponent("Button")
compose.ElevatedButton = defineComponent("ElevatedButton")
compose.FilledTonalButton = defineComponent("FilledTonalButton")
compose.OutlinedButton = defineComponent("OutlinedButton")
compose.TextButton = defineComponent("TextButton")
compose.IconButton = defineComponent("IconButton")
compose.Card = defineComponent("Card")
compose.ElevatedCard = defineComponent("ElevatedCard")
compose.OutlinedCard = defineComponent("OutlinedCard")
compose.Spacer = defineComponent("Spacer")
compose.Divider = defineComponent("Divider")
compose.TextField = defineComponent("TextField")
compose.OutlinedTextField = defineComponent("OutlinedTextField")
compose.Checkbox = defineComponent("Checkbox")
compose.Switch = defineComponent("Switch")
compose.RadioButton = defineComponent("RadioButton")
compose.CircularProgressIndicator = defineComponent("CircularProgressIndicator")
compose.LinearProgressIndicator = defineComponent("LinearProgressIndicator")
compose.Icon = defineComponent("Icon")
compose.LazyColumn = defineComponent("LazyColumn")

compose.Scope = Scope
compose.createState = createState

function compose.setContent(func)
  compose.rootContentFunc = func
end

return compose
