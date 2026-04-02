// 学长学姐答疑模块的mock数据

// 学长学姐列表
export const seniors = [
  {
    id: 1,
    name: '张学长',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=professional%20college%20student%20male%20avatar%20portrait&image_size=square',
    major: '计算机科学与技术',
    grade: '2020级',
    bio: '擅长算法和数据结构，曾获得ACM竞赛奖项',
    answerCount: 128,
    acceptanceRate: 92,
    rating: 4.9
  },
  {
    id: 2,
    name: '李学姐',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=professional%20college%20student%20female%20avatar%20portrait&image_size=square',
    major: '电子信息工程',
    grade: '2019级',
    bio: '专注于嵌入式系统和物联网开发',
    answerCount: 96,
    acceptanceRate: 88,
    rating: 4.7
  },
  {
    id: 3,
    name: '王学长',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=professional%20college%20student%20male%20avatar%20portrait%20glasses&image_size=square',
    major: '软件工程',
    grade: '2020级',
    bio: '前端开发专家，熟悉React、Vue等框架',
    answerCount: 156,
    acceptanceRate: 95,
    rating: 4.8
  },
  {
    id: 4,
    name: '陈学姐',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=professional%20college%20student%20female%20avatar%20portrait%20smiling&image_size=square',
    major: '数据科学与大数据技术',
    grade: '2019级',
    bio: '数据分析和机器学习爱好者',
    answerCount: 87,
    acceptanceRate: 90,
    rating: 4.6
  }
];

// 问题分类
export const categories = [
  { id: 'all', name: '全部' },
  { id: 'computer', name: '计算机科学' },
  { id: 'electronics', name: '电子信息' },
  { id: 'software', name: '软件工程' },
  { id: 'data', name: '数据科学' },
  { id: 'math', name: '数学' },
  { id: 'physics', name: '物理' },
  { id: 'other', name: '其他' }
];

// 问题列表
export const questions = [
  {
    id: 1,
    title: '如何理解计算机网络中的TCP/IP协议栈？',
    content: '刚学计算机网络，对TCP/IP协议栈的层次结构和各层功能不太理解，特别是传输层和网络层的区别，希望学长学姐能详细解释一下。',
    category: 'computer',
    categoryText: '计算机科学',
    seniorId: 1,
    seniorName: '张学长',
    status: 'solved',
    likes: 12,
    viewCount: 156,
    answerCount: 3,
    createdAt: '2026-03-28T10:00:00Z',
    updatedAt: '2026-03-28T14:30:00Z',
    answers: [
      {
        id: 101,
        content: 'TCP/IP协议栈是互联网的基础，分为四层：网络接口层、网络层、传输层和应用层。网络层主要负责IP地址和路由，传输层负责端到端的通信，TCP是面向连接的可靠传输协议...',
        seniorId: 1,
        seniorName: '张学长',
        likes: 8,
        isAccepted: true,
        createdAt: '2026-03-28T11:20:00Z'
      },
      {
        id: 102,
        content: '补充一下，TCP和UDP的区别在于TCP是可靠的、面向连接的，而UDP是不可靠的、无连接的，适合实时应用。',
        seniorId: 3,
        seniorName: '王学长',
        likes: 4,
        isAccepted: false,
        createdAt: '2026-03-28T13:15:00Z'
      }
    ]
  },
  {
    id: 2,
    title: 'Vue3的Composition API相比Options API有什么优势？',
    content: '最近开始学习Vue3，看到有Composition API和Options API两种写法，想了解一下它们的区别和各自的优势，特别是在大型项目中的应用。',
    category: 'software',
    categoryText: '软件工程',
    seniorId: 3,
    seniorName: '王学长',
    status: 'solved',
    likes: 18,
    viewCount: 203,
    answerCount: 2,
    createdAt: '2026-03-27T16:45:00Z',
    updatedAt: '2026-03-28T09:20:00Z',
    answers: [
      {
        id: 201,
        content: 'Composition API的主要优势在于：1. 更好的代码组织，相关逻辑可以放在一起；2. 更好的类型推断；3. 更灵活的复用逻辑；4. 更适合大型项目。Options API则更适合小型项目和初学者。',
        seniorId: 3,
        seniorName: '王学长',
        likes: 12,
        isAccepted: true,
        createdAt: '2026-03-27T17:30:00Z'
      }
    ]
  },
  {
    id: 3,
    title: '嵌入式系统中如何优化功耗？',
    content: '正在做一个基于STM32的嵌入式项目，需要长时间运行，电池供电，想了解一些有效的功耗优化方法。',
    category: 'electronics',
    categoryText: '电子信息',
    seniorId: 2,
    seniorName: '李学姐',
    status: 'unsolved',
    likes: 9,
    viewCount: 128,
    answerCount: 1,
    createdAt: '2026-03-29T08:15:00Z',
    updatedAt: '2026-03-29T08:15:00Z',
    answers: [
      {
        id: 301,
        content: '嵌入式系统功耗优化可以从以下几个方面入手：1. 选择低功耗的MCU；2. 合理使用休眠模式；3. 优化时钟频率；4. 减少外设的使用时间；5. 使用低功耗的传感器。',
        seniorId: 2,
        seniorName: '李学姐',
        likes: 5,
        isAccepted: false,
        createdAt: '2026-03-29T09:45:00Z'
      }
    ]
  },
  {
    id: 4,
    title: '如何用Python实现简单的线性回归？',
    content: '正在学习机器学习，想了解如何用Python实现一个简单的线性回归模型，包括数据预处理、模型训练和评估。',
    category: 'data',
    categoryText: '数据科学',
    seniorId: 4,
    seniorName: '陈学姐',
    status: 'solved',
    likes: 15,
    viewCount: 187,
    answerCount: 2,
    createdAt: '2026-03-26T14:30:00Z',
    updatedAt: '2026-03-26T16:45:00Z',
    answers: [
      {
        id: 401,
        content: '可以使用scikit-learn库来实现线性回归：1. 导入必要的库；2. 准备数据；3. 分割训练集和测试集；4. 创建并训练模型；5. 评估模型性能。也可以手动实现梯度下降算法来加深理解。',
        seniorId: 4,
        seniorName: '陈学姐',
        likes: 10,
        isAccepted: true,
        createdAt: '2026-03-26T15:15:00Z'
      }
    ]
  },
  {
    id: 5,
    title: '算法题：如何高效判断一个数是否为回文数？',
    content: '遇到一个算法题，要求判断一个整数是否为回文数，除了转换为字符串的方法外，有没有更高效的数学方法？',
    category: 'computer',
    categoryText: '计算机科学',
    seniorId: 1,
    seniorName: '张学长',
    status: 'solved',
    likes: 11,
    viewCount: 142,
    answerCount: 2,
    createdAt: '2026-03-25T11:20:00Z',
    updatedAt: '2026-03-25T13:40:00Z',
    answers: [
      {
        id: 501,
        content: '可以使用数学方法，通过取模和整除来反转数字的后半部分，然后与前半部分比较。这样避免了字符串转换，空间复杂度为O(1)。需要注意处理负数和末尾有0的情况。',
        seniorId: 1,
        seniorName: '张学长',
        likes: 7,
        isAccepted: true,
        createdAt: '2026-03-25T12:05:00Z'
      }
    ]
  },
  {
    id: 6,
    title: 'React中如何优化组件性能？',
    content: '开发React应用时，发现某些组件渲染速度较慢，想了解一些常见的性能优化方法。',
    category: 'software',
    categoryText: '软件工程',
    seniorId: 3,
    seniorName: '王学长',
    status: 'solved',
    likes: 16,
    viewCount: 198,
    answerCount: 3,
    createdAt: '2026-03-24T09:45:00Z',
    updatedAt: '2026-03-24T11:30:00Z',
    answers: [
      {
        id: 601,
        content: 'React性能优化方法包括：1. 使用useMemo和useCallback缓存计算结果和函数；2. 使用React.memo避免不必要的渲染；3. 合理使用key属性；4. 优化状态管理；5. 虚拟滚动处理长列表；6. 代码分割减少初始加载时间。',
        seniorId: 3,
        seniorName: '王学长',
        likes: 12,
        isAccepted: true,
        createdAt: '2026-03-24T10:20:00Z'
      }
    ]
  },
  {
    id: 7,
    title: 'STM32的中断优先级如何设置？',
    content: '在使用STM32开发时，需要设置多个中断的优先级，想了解NVIC的优先级分组和具体设置方法。',
    category: 'electronics',
    categoryText: '电子信息',
    seniorId: 2,
    seniorName: '李学姐',
    status: 'unsolved',
    likes: 8,
    viewCount: 115,
    answerCount: 1,
    createdAt: '2026-03-29T10:30:00Z',
    updatedAt: '2026-03-29T10:30:00Z',
    answers: [
      {
        id: 701,
        content: 'STM32的中断优先级由抢占优先级和响应优先级组成，通过NVIC_PriorityGroupConfig函数设置优先级分组，然后使用NVIC_Init函数设置具体中断的优先级。数字越小，优先级越高。',
        seniorId: 2,
        seniorName: '李学姐',
        likes: 4,
        isAccepted: false,
        createdAt: '2026-03-29T11:15:00Z'
      }
    ]
  },
  {
    id: 8,
    title: '如何使用Pandas进行数据清洗？',
    content: '拿到一份CSV数据，里面有很多缺失值和异常值，想了解Pandas中常用的数据清洗方法。',
    category: 'data',
    categoryText: '数据科学',
    seniorId: 4,
    seniorName: '陈学姐',
    status: 'solved',
    likes: 14,
    viewCount: 167,
    answerCount: 2,
    createdAt: '2026-03-23T15:20:00Z',
    updatedAt: '2026-03-23T17:45:00Z',
    answers: [
      {
        id: 801,
        content: 'Pandas数据清洗常用方法：1. 处理缺失值：dropna()或fillna()；2. 处理重复值：drop_duplicates()；3. 处理异常值：使用条件筛选或IQR方法；4. 数据类型转换：astype()；5. 字符串处理：str方法；6. 数据标准化：使用scikit-learn的StandardScaler。',
        seniorId: 4,
        seniorName: '陈学姐',
        likes: 9,
        isAccepted: true,
        createdAt: '2026-03-23T16:05:00Z'
      }
    ]
  }
];

// 统计数据
export const stats = {
  seniorCount: 24,
  totalQuestions: 156,
  solvedRate: 87,
  avgRating: 4.7
};

// Mock API函数
export const seniorMockApi = {
  // 获取学长学姐列表
  getSeniors: () => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(seniors);
      }, 300);
    });
  },
  
  // 获取学长学姐详情
  getSeniorById: (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const senior = seniors.find(s => s.id === id);
        resolve(senior || null);
      }, 200);
    });
  },
  
  // 获取问题列表
  getQuestions: (params) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        let filtered = [...questions];
        
        // 根据参数筛选
        if (params.category && params.category !== 'all') {
          filtered = filtered.filter(q => q.category === params.category);
        }
        
        if (params.seniorId) {
          filtered = filtered.filter(q => q.seniorId === params.seniorId);
        }
        
        if (params.filter === 'solved') {
          filtered = filtered.filter(q => q.status === 'solved');
        } else if (params.filter === 'unsolved') {
          filtered = filtered.filter(q => q.status === 'unsolved');
        }
        
        // 排序
        if (params.sort === 'latest') {
          filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        } else if (params.sort === 'popular') {
          filtered.sort((a, b) => b.likes - a.likes);
        }
        
        // 分页
        const page = params.page || 1;
        const pageSize = params.pageSize || 10;
        const start = (page - 1) * pageSize;
        const end = start + pageSize;
        const paginated = filtered.slice(start, end);
        
        resolve({
          data: {
            list: paginated,
            total: filtered.length,
            page,
            pageSize
          }
        });
      }, 300);
    });
  },
  
  // 获取问题详情
  getQuestionById: (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const question = questions.find(q => q.id === id);
        resolve({
          data: question || null
        });
      }, 200);
    });
  },
  
  // 发布问题
  publishQuestion: (data) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const newQuestion = {
          id: questions.length + 1,
          title: data.title,
          content: data.content,
          category: data.category,
          categoryText: categories.find(c => c.id === data.category)?.name || '其他',
          seniorId: data.seniorId || 1,
          seniorName: seniors.find(s => s.id === data.seniorId)?.name || '张学长',
          status: 'unsolved',
          likes: 0,
          viewCount: 0,
          answerCount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          answers: []
        };
        
        questions.unshift(newQuestion);
        resolve({
          data: newQuestion,
          message: '问题发布成功'
        });
      }, 500);
    });
  },
  
  // 点赞回答
  likeAnswer: (answerId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        // 找到对应的回答并增加点赞数
        for (const question of questions) {
          const answer = question.answers.find(a => a.id === answerId);
          if (answer) {
            answer.likes += 1;
            break;
          }
        }
        resolve({ message: '点赞成功' });
      }, 200);
    });
  },
  
  // 采纳回答
  acceptAnswer: (questionId, answerId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const question = questions.find(q => q.id === questionId);
        if (question) {
          question.status = 'solved';
          // 取消其他回答的采纳状态
          for (const answer of question.answers) {
            answer.isAccepted = answer.id === answerId;
          }
        }
        resolve({ message: '采纳成功' });
      }, 200);
    });
  },
  
  // 提交追问
  submitFollowUp: (questionId, content) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const question = questions.find(q => q.id === questionId);
        if (question) {
          // 这里可以添加追问逻辑，简化处理
          question.updatedAt = new Date().toISOString();
        }
        resolve({ message: '追问提交成功' });
      }, 300);
    });
  }
};
