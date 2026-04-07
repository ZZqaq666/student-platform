// 按钮组件的mock数据
// 包含所有页面按钮的状态、行为和响应数据

export const buttonMockData = {
  // 练习页面按钮
  practice: {
    backBtn: {
      id: 'practice-back-btn',
      label: '返回',
      disabled: false,
      action: 'navigate',
      target: '/knowledge-graph',
      response: {
        success: true,
        message: '返回知识图谱页面'
      }
    },
    exitBtn: {
      id: 'practice-exit-btn',
      label: '退出练习',
      disabled: false,
      action: 'confirm',
      confirmMessage: '确定要退出练习吗？未完成的练习将不会保存。',
      response: {
        success: true,
        message: '已退出练习'
      }
    },
    prevBtn: {
      id: 'practice-prev-btn',
      label: '上一题',
      disabled: true, // 第一题时禁用
      action: 'navigateQuestion',
      direction: 'prev',
      response: {
        success: true,
        message: '已切换到上一题'
      }
    },
    nextBtn: {
      id: 'practice-next-btn',
      label: '下一题',
      disabled: false,
      action: 'navigateQuestion',
      direction: 'next',
      response: {
        success: true,
        message: '已切换到下一题'
      }
    },
    submitBtn: {
      id: 'practice-submit-btn',
      label: '提交练习',
      disabled: false,
      action: 'submit',
      response: {
        success: true,
        message: '练习提交成功',
        data: {
          score: 85,
          correctCount: 17,
          totalCount: 20,
          wrongQuestions: [2, 5, 8, 12]
        }
      }
    },
    reviewWrongBtn: {
      id: 'practice-review-wrong-btn',
      label: '查看错题',
      disabled: false,
      action: 'review',
      type: 'wrong',
      response: {
        success: true,
        message: '正在查看错题'
      }
    },
    reviewAllBtn: {
      id: 'practice-review-all-btn',
      label: '查看全部题',
      disabled: false,
      action: 'review',
      type: 'all',
      response: {
        success: true,
        message: '正在查看全部题目'
      }
    },
    addToWrongBookBtn: {
      id: 'practice-add-wrong-book-btn',
      label: '确定添加',
      disabled: false,
      action: 'addWrongBook',
      response: {
        success: true,
        message: '已成功添加到错题本'
      }
    },
    cancelWrongBookBtn: {
      id: 'practice-cancel-wrong-book-btn',
      label: '取消',
      disabled: false,
      action: 'closeDialog',
      response: {
        success: true,
        message: '已取消添加'
      }
    }
  },
  
  // 知识图谱页面按钮
  knowledgeGraph: {
    homeBtn: {
      id: 'kg-home-btn',
      label: '返回首页',
      disabled: false,
      action: 'navigate',
      target: '/',
      response: {
        success: true,
        message: '返回首页'
      }
    },
    bookSelectorBtn: {
      id: 'kg-book-selector-btn',
      label: '选择书籍',
      disabled: false,
      action: 'toggle',
      target: 'bookSelector',
      response: {
        success: true,
        message: '书籍选择器已切换'
      }
    },
    exerciseFilterBtns: {
      basic: {
        id: 'kg-filter-basic',
        label: '基础巩固',
        active: true,
        disabled: false,
        action: 'filter',
        filterType: 'basic',
        response: {
          success: true,
          message: '已筛选基础巩固习题'
        }
      },
      advanced: {
        id: 'kg-filter-advanced',
        label: '进阶提升',
        active: false,
        disabled: false,
        action: 'filter',
        filterType: 'advanced',
        response: {
          success: true,
          message: '已筛选进阶提升习题'
        }
      },
      exam: {
        id: 'kg-filter-exam',
        label: '真题冲刺',
        active: false,
        disabled: false,
        action: 'filter',
        filterType: 'exam',
        response: {
          success: true,
          message: '已筛选真题冲刺习题'
        }
      },
      competition: {
        id: 'kg-filter-competition',
        label: '竞赛拓展',
        active: false,
        disabled: false,
        action: 'filter',
        filterType: 'competition',
        response: {
          success: true,
          message: '已筛选竞赛拓展习题'
        }
      }
    },
    exerciseTypeFilterBtns: {
      all: {
        id: 'kg-type-all',
        label: '全部类型',
        active: true,
        disabled: false,
        action: 'filterType',
        type: 'all',
        response: {
          success: true,
          message: '已筛选全部类型习题'
        }
      },
      concept: {
        id: 'kg-type-concept',
        label: '概念题',
        active: false,
        disabled: false,
        action: 'filterType',
        type: '概念题',
        response: {
          success: true,
          message: '已筛选概念题'
        }
      },
      application: {
        id: 'kg-type-application',
        label: '应用题',
        active: false,
        disabled: false,
        action: 'filterType',
        type: '应用题',
        response: {
          success: true,
          message: '已筛选应用题'
        }
      },
      analysis: {
        id: 'kg-type-analysis',
        label: '分析题',
        active: false,
        disabled: false,
        action: 'filterType',
        type: '分析题',
        response: {
          success: true,
          message: '已筛选分析题'
        }
      },
      programming: {
        id: 'kg-type-programming',
        label: '编程题',
        active: false,
        disabled: false,
        action: 'filterType',
        type: '编程题',
        response: {
          success: true,
          message: '已筛选编程题'
        }
      },
      design: {
        id: 'kg-type-design',
        label: '设计题',
        active: false,
        disabled: false,
        action: 'filterType',
        type: '设计题',
        response: {
          success: true,
          message: '已筛选设计题'
        }
      }
    },
    batchAddWrongBookBtn: {
      id: 'kg-batch-add-wrong-book',
      label: '批量加入错题本',
      disabled: true, // 未选择习题时禁用
      action: 'batchAdd',
      response: {
        success: true,
        message: '已批量加入错题本',
        data: {
          count: 3
        }
      }
    },
    selectAllBtn: {
      id: 'kg-select-all',
      label: '全选',
      disabled: false,
      action: 'selectAll',
      response: {
        success: true,
        message: '已全选习题'
      }
    },
    startPracticeBtn: {
      id: 'kg-start-practice',
      label: '开始练习',
      disabled: true, // 未选择习题时禁用
      action: 'startPractice',
      response: {
        success: true,
        message: '开始练习',
        data: {
          exerciseCount: 5,
          practiceId: 'practice_12345'
        }
      }
    }
  },
  
  // 问答中心页面按钮
  qa: {
    backToStudyBtn: {
      id: 'qa-back-to-study',
      label: '返回学习',
      disabled: false,
      action: 'navigate',
      target: '/study',
      response: {
        success: true,
        message: '返回学习页面'
      }
    },
    backHomeBtn: {
      id: 'qa-back-home',
      label: '返回首页',
      disabled: false,
      action: 'navigate',
      target: '/',
      response: {
        success: true,
        message: '返回首页'
      }
    },
    tabBtns: {
      intelligent: {
        id: 'qa-tab-intelligent',
        label: '智能答疑',
        active: true,
        disabled: false,
        action: 'switchTab',
        tab: '智能答疑',
        response: {
          success: true,
          message: '已切换到智能答疑'
        }
      },
      exam: {
        id: 'qa-tab-exam',
        label: '考研/考证问答',
        active: false,
        disabled: false,
        action: 'switchTab',
        tab: '考研/考证问答',
        response: {
          success: true,
          message: '已切换到考研/考证问答'
        }
      }
    },
    sortBtns: {
      importance: {
        id: 'qa-sort-importance',
        label: '重要性',
        disabled: false,
        action: 'sort',
        sortBy: 'importance',
        response: {
          success: true,
          message: '已按重要性排序'
        }
      },
      difficulty: {
        id: 'qa-sort-difficulty',
        label: '难度',
        disabled: false,
        action: 'sort',
        sortBy: 'difficulty',
        response: {
          success: true,
          message: '已按难度排序'
        }
      },
      studyTime: {
        id: 'qa-sort-study-time',
        label: '学习时间',
        disabled: false,
        action: 'sort',
        sortBy: 'studyTime',
        response: {
          success: true,
          message: '已按学习时间排序'
        }
      }
    },
    viewModeBtn: {
      id: 'qa-view-mode',
      label: '卡片视图',
      disabled: false,
      action: 'toggleView',
      currentMode: 'list',
      nextMode: 'card',
      response: {
        success: true,
        message: '已切换视图模式'
      }
    },
    startPracticeBtn: {
      id: 'qa-start-practice',
      label: '开始练习',
      disabled: false,
      action: 'startPractice',
      response: {
        success: true,
        message: '开始练习',
        data: {
          paperId: 'paper_12345',
          questionCount: 10
        }
      }
    },
    refreshRecommendBtn: {
      id: 'qa-refresh-recommend',
      label: '重新推荐',
      disabled: false,
      action: 'refresh',
      response: {
        success: true,
        message: '已重新推荐真题'
      }
    },
    submitQuestionBtn: {
      id: 'qa-submit-question',
      label: '提交问题',
      disabled: true, // 问题为空时禁用
      action: 'submitQuestion',
      response: {
        success: true,
        message: '问题提交成功',
        data: {
          questionId: 'q_12345',
          answerTime: '2026-04-05 15:00:00'
        }
      }
    }
  },
  
  // 书架页面按钮
  bookshelf: {
    prevPageBtn: {
      id: 'bookshelf-prev-page',
      label: '上一页',
      disabled: true, // 第一页时禁用
      action: 'page',
      direction: 'prev',
      response: {
        success: true,
        message: '已切换到上一页'
      }
    },
    nextPageBtn: {
      id: 'bookshelf-next-page',
      label: '下一页',
      disabled: false,
      action: 'page',
      direction: 'next',
      response: {
        success: true,
        message: '已切换到下一页'
      }
    },
    pageNumberBtns: {
      '1': {
        id: 'bookshelf-page-1',
        label: '1',
        active: true,
        disabled: false,
        action: 'goToPage',
        page: 1,
        response: {
          success: true,
          message: '已切换到第1页'
        }
      },
      '2': {
        id: 'bookshelf-page-2',
        label: '2',
        active: false,
        disabled: false,
        action: 'goToPage',
        page: 2,
        response: {
          success: true,
          message: '已切换到第2页'
        }
      },
      '3': {
        id: 'bookshelf-page-3',
        label: '3',
        active: false,
        disabled: false,
        action: 'goToPage',
        page: 3,
        response: {
          success: true,
          message: '已切换到第3页'
        }
      }
    },
    closeModalBtn: {
      id: 'bookshelf-close-modal',
      label: '×',
      disabled: false,
      action: 'closeModal',
      response: {
        success: true,
        message: '已关闭模态框'
      }
    }
  },
  
  // 错题本页面按钮
  wrongBook: {
    reviewBtn: {
      id: 'wrong-book-review',
      label: '复习',
      disabled: false,
      action: 'review',
      response: {
        success: true,
        message: '开始复习错题'
      }
    },
    redoBtn: {
      id: 'wrong-book-redo',
      label: '重做',
      disabled: false,
      action: 'redo',
      response: {
        success: true,
        message: '开始重做错题'
      }
    },
    editBtn: {
      id: 'wrong-book-edit',
      label: '编辑',
      disabled: false,
      action: 'edit',
      response: {
        success: true,
        message: '打开编辑对话框'
      }
    },
    deleteBtn: {
      id: 'wrong-book-delete',
      label: '删除',
      disabled: false,
      action: 'delete',
      confirmMessage: '确定要删除这道错题吗？',
      response: {
        success: true,
        message: '错题已删除'
      }
    },
    statsBtn: {
      id: 'wrong-book-stats',
      label: '数据统计',
      disabled: false,
      action: 'showStats',
      response: {
        success: true,
        message: '显示数据统计'
      }
    },
    exportBtn: {
      id: 'wrong-book-export',
      label: '导出',
      disabled: false,
      action: 'export',
      response: {
        success: true,
        message: '开始导出错题'
      }
    }
  }
};

// 按钮状态更新函数
export const updateButtonState = (page, buttonId, updates) => {
  const pageData = buttonMockData[page];
  if (pageData) {
    // 处理嵌套按钮结构
    if (buttonId.includes('.')) {
      const [parentId, childId] = buttonId.split('.');
      if (pageData[parentId] && pageData[parentId][childId]) {
        Object.assign(pageData[parentId][childId], updates);
        return true;
      }
    } else if (pageData[buttonId]) {
      Object.assign(pageData[buttonId], updates);
      return true;
    }
  }
  return false;
};

// 获取按钮数据
export const getButtonData = (page, buttonId) => {
  const pageData = buttonMockData[page];
  if (pageData) {
    if (buttonId.includes('.')) {
      const [parentId, childId] = buttonId.split('.');
      return pageData[parentId]?.[childId];
    }
    return pageData[buttonId];
  }
  return null;
};

// 模拟按钮点击
export const simulateButtonClick = (page, buttonId) => {
  const buttonData = getButtonData(page, buttonId);
  if (buttonData && !buttonData.disabled) {
    // 模拟异步操作
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(buttonData.response);
      }, 300);
    });
  }
  return Promise.resolve({ success: false, message: '按钮不可用' });
};