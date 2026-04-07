# 按钮组件 Mock 数据说明

## 概述

本文件提供了前端界面中所有按钮组件的mock数据，用于模拟按钮的状态、行为和响应。

## 目录结构

```
src/mock/
├── buttonMock.js  # 按钮组件的mock数据
└── README.md      # 本说明文件
```

## 数据结构

### 按钮数据结构

每个按钮的数据结构包含以下字段：

| 字段名 | 类型 | 描述 |
|-------|------|------|
| id | String | 按钮唯一标识符 |
| label | String | 按钮显示文本 |
| disabled | Boolean | 按钮是否禁用 |
| action | String | 按钮点击后的操作类型 |
| response | Object | 按钮点击后的响应数据 |
| [其他字段] | 任意 | 特定按钮的附加属性 |

### 响应数据结构

```javascript
{
  success: Boolean,  // 操作是否成功
  message: String,   // 操作结果消息
  data: Object       // 附加数据（可选）
}
```

## 支持的页面

1. **practice** - 练习页面
2. **knowledgeGraph** - 知识图谱页面
3. **qa** - 问答中心页面
4. **bookshelf** - 书架页面
5. **wrongBook** - 错题本页面

## 使用方法

### 1. 导入mock数据

```javascript
import { buttonMockData, simulateButtonClick, updateButtonState } from '@/mock/buttonMock';
```

### 2. 模拟按钮点击

```javascript
// 模拟点击练习页面的提交按钮
const response = await simulateButtonClick('practice', 'submitBtn');
if (response.success) {
  // 处理成功响应
  console.log(response.message);
  if (response.data) {
    console.log('得分:', response.data.score);
  }
} else {
  // 处理失败响应
  console.error(response.message);
}
```

### 3. 更新按钮状态

```javascript
// 更新练习页面上一题按钮的禁用状态
updateButtonState('practice', 'prevBtn', { disabled: false });

// 更新知识图谱页面的筛选按钮状态
updateButtonState('knowledgeGraph', 'exerciseFilterBtns.basic', { active: false });
updateButtonState('knowledgeGraph', 'exerciseFilterBtns.advanced', { active: true });
```

### 4. 获取按钮数据

```javascript
import { getButtonData } from '@/mock/buttonMock';

// 获取练习页面提交按钮的数据
const submitBtnData = getButtonData('practice', 'submitBtn');
console.log('提交按钮状态:', submitBtnData.disabled);
```

## 示例

### 练习页面按钮使用示例

```javascript
// 在PracticePage.vue中使用
import { simulateButtonClick, updateButtonState } from '@/mock/buttonMock';

// 处理提交按钮点击
const handleSubmit = async () => {
  const response = await simulateButtonClick('practice', 'submitBtn');
  if (response.success) {
    ElMessage.success(response.message);
    // 处理提交成功后的逻辑
    showAnswer.value = true;
  } else {
    ElMessage.error(response.message);
  }
};

// 更新上一题按钮状态
const updatePrevButtonState = () => {
  updateButtonState('practice', 'prevBtn', { 
    disabled: currentIndex.value === 0 
  });
};
```

### 知识图谱页面按钮使用示例

```javascript
// 在KnowledgeGraph.vue中使用
import { simulateButtonClick, updateButtonState } from '@/mock/buttonMock';

// 处理筛选按钮点击
const handleFilterClick = async (filterType) => {
  // 更新按钮状态
  Object.keys(buttonMockData.knowledgeGraph.exerciseFilterBtns).forEach(key => {
    updateButtonState('knowledgeGraph', `exerciseFilterBtns.${key}`, {
      active: key === filterType
    });
  });
  
  // 模拟点击操作
  const response = await simulateButtonClick('knowledgeGraph', `exerciseFilterBtns.${filterType}`);
  if (response.success) {
    // 处理筛选逻辑
    exerciseFilter.value = filterType;
  }
};
```

## 注意事项

1. **异步操作**：`simulateButtonClick` 函数返回Promise，模拟实际的异步请求
2. **状态管理**：按钮状态会在内存中更新，刷新页面后会重置为初始状态
3. **响应模拟**：所有响应都是模拟数据，实际项目中需要替换为真实的API调用
4. **扩展**：可以根据需要添加新的按钮或修改现有按钮的行为

## 维护说明

- 当添加新页面或新按钮时，需要在 `buttonMock.js` 中添加相应的mock数据
- 当按钮行为发生变化时，需要更新对应的mock数据
- 建议定期检查mock数据与实际UI的一致性