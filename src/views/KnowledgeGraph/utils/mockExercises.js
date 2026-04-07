// 生成与练习页面兼容的模拟习题数据

// 生成选择题的辅助函数
const generateChoiceQuestion = (id, title, content, options, correctIndex, analysis, knowledgePoint, difficulty = 'MEDIUM') => {
  return {
    id: id,
    title: title,
    content: content,
    type: '选择题',
    questionType: '选择题',
    options: options,
    correctAnswer: correctIndex,
    analysis: analysis,
    knowledgePoint: knowledgePoint,
    difficulty: difficulty,
    difficultyText: difficulty === 'EASY' ? '简单' : difficulty === 'MEDIUM' ? '中等' : '困难',
    score: 10
  }
}

// 生成填空题的辅助函数
const generateFillBlankQuestion = (id, title, content, correctAnswer, analysis, knowledgePoint, difficulty = 'MEDIUM') => {
  return {
    id: id,
    title: title,
    content: content,
    type: '填空题',
    questionType: '填空题',
    correctAnswer: correctAnswer,
    analysis: analysis,
    knowledgePoint: knowledgePoint,
    difficulty: difficulty,
    difficultyText: difficulty === 'EASY' ? '简单' : difficulty === 'MEDIUM' ? '中等' : '困难',
    score: 10
  }
}

// 生成解答题的辅助函数
const generateEssayQuestion = (id, title, content, correctAnswer, analysis, knowledgePoint, difficulty = 'MEDIUM') => {
  return {
    id: id,
    title: title,
    content: content,
    type: '解答题',
    questionType: '解答题',
    correctAnswer: correctAnswer,
    analysis: analysis,
    knowledgePoint: knowledgePoint,
    difficulty: difficulty,
    difficultyText: difficulty === 'EASY' ? '简单' : difficulty === 'MEDIUM' ? '中等' : '困难',
    score: 10
  }
}

// 计算机网络习题
const networkExercises = [
  generateChoiceQuestion(
    'net_101',
    'TCP/IP协议层次结构',
    'TCP/IP协议栈分为几个层次？',
    ['4层', '5层', '6层', '7层'],
    0,
    'TCP/IP协议栈分为应用层、传输层、网络层和网络接口层，共4层。',
    '计算机网络/TCP/IP',
    'EASY'
  ),
  generateChoiceQuestion(
    'net_102',
    'HTTP与HTTPS端口',
    'HTTP和HTTPS默认使用的端口号分别是？',
    ['80和443', '443和80', '8080和8443', '21和22'],
    0,
    'HTTP默认使用80端口，HTTPS默认使用443端口。',
    '计算机网络/网络协议',
    'EASY'
  ),
  generateChoiceQuestion(
    'net_103',
    'IP地址分类',
    '以下哪个是私有IP地址范围？',
    ['10.0.0.0/8', '172.32.0.0/16', '192.169.0.0/16', '224.0.0.0/4'],
    0,
    '10.0.0.0/8、172.16.0.0/12和192.168.0.0/16是私有IP地址范围。',
    '计算机网络/IP协议',
    'MEDIUM'
  ),
  generateFillBlankQuestion(
    'net_201',
    '子网掩码计算',
    'IP地址192.168.1.100/24的子网掩码是______。',
    '255.255.255.0',
    '/24表示子网掩码有24个1，即255.255.255.0。',
    '计算机网络/子网划分',
    'MEDIUM'
  ),
  generateEssayQuestion(
    'net_301',
    'TCP三次握手',
    '请描述TCP三次握手的过程，并说明为什么需要三次握手。',
    '第一次握手：客户端发送SYN包；第二次握手：服务器发送SYN+ACK包；第三次握手：客户端发送ACK包。三次握手可以确保双方的发送和接收能力都正常，防止历史连接初始化造成的问题。',
    'TCP三次握手是建立连接的过程：第一次客户端发送SYN，第二次服务器回复SYN+ACK，第三次客户端发送ACK。需要三次是为了确认双方的发送和接收能力，防止已失效的连接请求报文突然到达服务器造成错误。',
    '计算机网络/TCP协议',
    'HARD'
  )
]

// 操作系统习题
const osExercises = [
  generateChoiceQuestion(
    'os_101',
    '进程状态',
    '进程的三种基本状态是？',
    ['就绪、运行、阻塞', '创建、运行、终止', '挂起、运行、等待', '新建、就绪、运行'],
    0,
    '进程的三种基本状态是就绪（Ready）、运行（Running）和阻塞（Blocked）。',
    '操作系统/进程管理',
    'EASY'
  ),
  generateChoiceQuestion(
    'os_102',
    '死锁条件',
    '产生死锁的四个必要条件不包括？',
    ['互斥', '请求与保持', '不剥夺', '并发'],
    3,
    '产生死锁的四个必要条件是：互斥、请求与保持、不剥夺和循环等待。',
    '操作系统/死锁',
    'MEDIUM'
  ),
  generateFillBlankQuestion(
    'os_201',
    '页面置换',
    '在请求分页系统中，当内存空间不够时，需要进行______。',
    '页面置换',
    '当内存空间不足时，系统需要选择一个页面将其换出到磁盘，以便为新的页面腾出空间，这个过程称为页面置换。',
    '操作系统/内存管理',
    'MEDIUM'
  ),
  generateEssayQuestion(
    'os_301',
    '进程与线程区别',
    '请说明进程和线程的主要区别，以及各自的使用场景。',
    '进程是资源分配的基本单位，线程是CPU调度的基本单位。进程拥有独立的地址空间，线程共享进程的地址空间。进程切换开销大，线程切换开销小。',
    '进程是资源分配的基本单位，拥有独立的地址空间和资源；线程是CPU调度的基本单位，共享进程的资源。进程切换需要切换页表，开销大；线程切换只需保存寄存器，开销小。多线程适合计算密集型任务，多进程适合IO密集型任务。',
    '操作系统/进程管理',
    'HARD'
  )
]

// 数据库习题
const dbExercises = [
  generateChoiceQuestion(
    'db_101',
    'SQL语句',
    '以下哪个SQL语句用于删除表中的所有数据但保留表结构？',
    ['DELETE FROM table', 'DROP TABLE table', 'TRUNCATE TABLE table', 'REMOVE FROM table'],
    2,
    'TRUNCATE TABLE删除所有数据但保留表结构，比DELETE更快，但不能回滚。',
    '数据库/SQL',
    'EASY'
  ),
  generateChoiceQuestion(
    'db_102',
    '事务特性',
    'ACID特性中的I代表？',
    ['独立性(Isolation)', '完整性(Integrity)', '一致性(Consistency)', '隔离性(Isolation)'],
    3,
    'ACID分别代表：原子性(Atomicity)、一致性(Consistency)、隔离性(Isolation)、持久性(Durability)。',
    '数据库/事务管理',
    'MEDIUM'
  ),
  generateFillBlankQuestion(
    'db_201',
    '范式',
    '在关系数据库中，第二范式要求满足第一范式且______。',
    '不存在非主属性对主键的部分函数依赖',
    '第二范式(2NF)要求满足第一范式，并且不存在非主属性对主键的部分函数依赖。',
    '数据库/范式',
    'HARD'
  ),
  generateEssayQuestion(
    'db_301',
    '索引原理',
    '请说明数据库索引的工作原理，以及B+树索引的优点。',
    '索引是一种数据结构，通过建立关键字到数据位置的映射来加快查询速度。B+树索引的优点是：1. 所有数据都在叶子节点，查询性能稳定；2. 叶子节点通过指针连接，适合范围查询；3. 树的高度低，IO次数少。',
    '索引通过建立关键字到数据位置的映射来加速查询。B+树索引的优点包括：所有数据存储在叶子节点，查询性能稳定；叶子节点通过链表连接，支持高效的范围查询；树高度较低，减少磁盘IO次数；节点大小与磁盘页对齐，提高读取效率。',
    '数据库/索引',
    'HARD'
  )
]

// 数据结构与算法习题
const algoExercises = [
  generateChoiceQuestion(
    'algo_101',
    '时间复杂度',
    '快速排序的平均时间复杂度是？',
    ['O(n)', 'O(n log n)', 'O(n²)', 'O(log n)'],
    1,
    '快速排序的平均时间复杂度为O(n log n)，最坏情况下为O(n²)。',
    '数据结构/排序算法',
    'EASY'
  ),
  generateChoiceQuestion(
    'algo_102',
    '栈的应用',
    '以下哪种数据结构最适合实现函数调用栈？',
    ['队列', '栈', '链表', '树'],
    1,
    '栈的后进先出(LIFO)特性非常适合实现函数调用栈。',
    '数据结构/栈',
    'EASY'
  ),
  generateFillBlankQuestion(
    'algo_201',
    '二叉树遍历',
    '二叉树的中序遍历顺序是：左子树 -> ______ -> 右子树。',
    '根节点',
    '中序遍历的顺序是：先遍历左子树，然后访问根节点，最后遍历右子树。',
    '数据结构/二叉树',
    'MEDIUM'
  ),
  generateEssayQuestion(
    'algo_301',
    '动态规划',
    '请解释动态规划的基本思想，并说明使用动态规划解决问题的步骤。',
    '动态规划的基本思想是将复杂问题分解为子问题，通过保存子问题的解来避免重复计算。步骤：1. 定义状态；2. 找出状态转移方程；3. 确定初始条件；4. 按顺序计算状态。',
    '动态规划的基本思想是最优子结构和重叠子问题。通过将大问题分解为小问题，并保存小问题的解来避免重复计算。解题步骤：1. 分析问题的最优子结构；2. 定义状态表示；3. 建立状态转移方程；4. 确定边界条件；5. 确定计算顺序（自底向上或记忆化搜索）。',
    '算法/动态规划',
    'HARD'
  )
]

// 高等数学习题
const mathExercises = [
  generateChoiceQuestion(
    'math_101',
    '导数计算',
    '函数f(x) = x²的导数是？',
    ['f\'(x) = x', 'f\'(x) = 2x', 'f\'(x) = x²', 'f\'(x) = 2'],
    1,
    '根据幂函数求导法则，(x^n)\' = n*x^(n-1)，所以(x²)\' = 2x。',
    '高等数学/导数',
    'EASY'
  ),
  generateChoiceQuestion(
    'math_102',
    '定积分',
    '∫[0,1] x dx 的值是？',
    ['0', '0.5', '1', '2'],
    1,
    '∫[0,1] x dx = [x²/2][0,1] = 1/2 - 0 = 0.5',
    '高等数学/积分',
    'EASY'
  ),
  generateFillBlankQuestion(
    'math_201',
    '矩阵运算',
    '若A是n阶方阵，|A|表示A的行列式，则|kA| = ______（k为常数）。',
    'kⁿ|A|',
    'n阶方阵乘以常数k后，行列式的值为k的n次方乘以原行列式。',
    '高等数学/线性代数',
    'MEDIUM'
  ),
  generateEssayQuestion(
    'math_301',
    '微积分基本定理',
    '请阐述微积分基本定理的内容及其意义。',
    '微积分基本定理建立了微分和积分的联系，指出定积分可以通过求原函数来计算。',
    '微积分基本定理包括两部分：第一部分指出如果f在[a,b]上连续，则F(x)=∫[a,x]f(t)dt在[a,b]上可导，且F\'(x)=f(x)；第二部分指出如果F是f的一个原函数，则∫[a,b]f(x)dx=F(b)-F(a)。该定理建立了微分和积分的互逆关系，是微积分的理论基础。',
    '高等数学/微积分',
    'HARD'
  )
]

// 生成旧版的模拟习题（知识图谱推荐用）
export const generateMockExercisesOld = (nodeName, exerciseFilter = 'all', exerciseTypeFilter = 'all') => {
  const exercises = []
  
  // 确保 nodeName 不为空
  const safeNodeName = nodeName || '知识节点'
  
  // 计算机网络相关习题
  if (safeNodeName.includes('计算机网络') || safeNodeName.includes('网络')) {
    exercises.push(
      {
        id: 101,
        title: 'TCP/IP协议栈的层次结构',
        description: '请描述TCP/IP协议栈的层次结构，并说明各层的主要功能。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['网络协议', 'TCP/IP', '网络层次']
      },
      {
        id: 102,
        title: 'HTTP与HTTPS的区别',
        description: '请详细说明HTTP与HTTPS的区别，以及HTTPS的工作原理。',
        difficulty: '进阶提升',
        type: '比较题',
        knowledgePoints: ['网络协议', 'HTTP/HTTPS', '网络安全']
      },
      {
        id: 103,
        title: '路由算法的选择',
        description: '请比较不同路由算法的优缺点，并说明在不同网络环境下如何选择合适的路由算法。',
        difficulty: '进阶提升',
        type: '分析题',
        knowledgePoints: ['网络拓扑', '路由算法', '网络层']
      },
      {
        id: 104,
        title: '网络安全防护措施',
        description: '请列举常见的网络安全威胁，并说明相应的防护措施。',
        difficulty: '真题冲刺',
        type: '应用题',
        knowledgePoints: ['网络安全', '防火墙', '加密技术']
      },
      {
        id: 105,
        title: '网络拓扑设计',
        description: '请设计一个企业网络拓扑结构，并说明设计理由。',
        difficulty: '竞赛拓展',
        type: '设计题',
        knowledgePoints: ['网络拓扑', '网络结构', '网络规划']
      }
    )
  } else if (safeNodeName.includes('操作系统原理') || safeNodeName.includes('操作系统')) {
    // 操作系统原理相关习题
    exercises.push(
      {
        id: 201,
        title: '进程管理与调度',
        description: '请解释进程的概念和进程调度的基本原理。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['进程管理', '进程调度', 'CPU调度']
      },
      {
        id: 202,
        title: '内存管理',
        description: '请说明分页存储管理和分段存储管理的区别。',
        difficulty: '进阶提升',
        type: '比较题',
        knowledgePoints: ['内存管理', '分页存储', '分段存储']
      },
      {
        id: 203,
        title: '文件系统',
        description: '请描述文件系统的基本功能和常见的文件系统类型。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['文件系统', '文件结构', '存储管理']
      },
      {
        id: 204,
        title: '设备管理',
        description: '请解释设备驱动程序的作用和工作原理。',
        difficulty: '真题冲刺',
        type: '应用题',
        knowledgePoints: ['设备管理', '设备驱动', 'I/O操作']
      },
      {
        id: 205,
        title: '死锁',
        description: '请分析死锁产生的必要条件和预防死锁的方法。',
        difficulty: '竞赛拓展',
        type: '分析题',
        knowledgePoints: ['死锁', '死锁预防', '进程同步']
      }
    )
  } else if (safeNodeName.includes('数据库系统概论') || safeNodeName.includes('数据库')) {
    // 数据库系统概论相关习题
    exercises.push(
      {
        id: 301,
        title: '数据库模型',
        description: '请解释关系模型的基本概念和特点。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['数据库模型', '关系模型', 'SQL']
      },
      {
        id: 302,
        title: 'SQL查询',
        description: '请编写SQL语句查询员工表中工资大于5000的员工信息。',
        difficulty: '进阶提升',
        type: '编程题',
        knowledgePoints: ['SQL语言', '查询语句', '关系模型']
      },
      {
        id: 303,
        title: '事务管理',
        description: '请解释事务的ACID特性及其重要性。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['事务管理', 'ACID特性', '并发控制']
      },
      {
        id: 304,
        title: '数据库设计',
        description: '请设计一个图书馆管理系统的数据库 schema。',
        difficulty: '真题冲刺',
        type: '设计题',
        knowledgePoints: ['数据库设计', '范式理论', 'ER图']
      },
      {
        id: 305,
        title: '并发控制',
        description: '请分析并发操作可能导致的数据不一致问题及解决方法。',
        difficulty: '竞赛拓展',
        type: '分析题',
        knowledgePoints: ['事务管理', '并发控制', '锁机制']
      }
    )
  } else if (safeNodeName.includes('数据结构与算法') || safeNodeName.includes('数据结构') || safeNodeName.includes('算法')) {
    // 数据结构与算法相关习题
    exercises.push(
      {
        id: 401,
        title: '线性表',
        description: '请实现一个单链表的反转操作。',
        difficulty: '基础巩固',
        type: '编程题',
        knowledgePoints: ['线性结构', '链表', '数据结构']
      },
      {
        id: 402,
        title: '树结构',
        description: '请解释二叉树的遍历方法及其时间复杂度。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['树形结构', '二叉树', '图遍历']
      },
      {
        id: 403,
        title: '排序算法',
        description: '请比较快速排序和归并排序的优缺点。',
        difficulty: '进阶提升',
        type: '比较题',
        knowledgePoints: ['排序算法', '快速排序', '归并排序', '时间复杂度']
      },
      {
        id: 404,
        title: '图算法',
        description: '请实现Dijkstra算法求解最短路径问题。',
        difficulty: '真题冲刺',
        type: '编程题',
        knowledgePoints: ['图结构', '最短路径', '贪心算法']
      },
      {
        id: 405,
        title: '动态规划',
        description: '请使用动态规划解决最长公共子序列问题。',
        difficulty: '竞赛拓展',
        type: '编程题',
        knowledgePoints: ['动态规划', '算法', '状态转移']
      }
    )
  } else if (safeNodeName.includes('高等数学') || safeNodeName.includes('数学')) {
    // 高等数学相关习题
    exercises.push(
      {
        id: 501,
        title: '导数计算',
        description: '请计算函数f(x) = x^3 + 2x^2 - 3x + 1的导数。',
        difficulty: '基础巩固',
        type: '计算题',
        knowledgePoints: ['微积分', '导数', '函数']
      },
      {
        id: 502,
        title: '定积分',
        description: '请计算从0到1的x^2的定积分。',
        difficulty: '进阶提升',
        type: '计算题',
        knowledgePoints: ['微积分', '定积分', '反导数']
      },
      {
        id: 503,
        title: '矩阵运算',
        description: '请计算两个3x3矩阵的乘积。',
        difficulty: '进阶提升',
        type: '计算题',
        knowledgePoints: ['线性代数', '矩阵', '矩阵运算']
      },
      {
        id: 504,
        title: '微分方程',
        description: '请求解一阶线性微分方程y + y = e^x。',
        difficulty: '真题冲刺',
        type: '计算题',
        knowledgePoints: ['常微分方程', '一阶微分方程', '积分因子']
      },
      {
        id: 505,
        title: '概率论',
        description: '请计算正态分布的概率密度函数在特定区间的积分。',
        difficulty: '竞赛拓展',
        type: '计算题',
        knowledgePoints: ['概率论', '概率分布', '正态分布']
      }
    )
  } else if (safeNodeName.includes('体育与健康')) {
    // 体育与健康相关习题
    exercises.push(
      {
        id: 601,
        title: '运动生理学',
        description: '请解释运动对心血管系统的影响。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['运动生理学', '心肺功能', '心血管系统']
      },
      {
        id: 602,
        title: '运动技能',
        description: '请描述篮球投篮的基本动作要领。',
        difficulty: '进阶提升',
        type: '应用题',
        knowledgePoints: ['运动技能', '球类运动', '篮球']
      },
      {
        id: 603,
        title: '健康知识',
        description: '请说明合理饮食对健康的重要性。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['健康知识', '营养学', '合理饮食']
      },
      {
        id: 604,
        title: '体育赛事',
        description: '请分析奥运会对城市发展的影响。',
        difficulty: '真题冲刺',
        type: '分析题',
        knowledgePoints: ['体育赛事', '奥运会', '城市发展']
      },
      {
        id: 605,
        title: '运动损伤',
        description: '请说明常见运动损伤的预防和处理方法。',
        difficulty: '竞赛拓展',
        type: '应用题',
        knowledgePoints: ['运动损伤', '损伤预防', '急救处理']
      }
    )
  } else if (safeNodeName.includes('英语') || safeNodeName.includes('新视野大学英语')) {
    // 英语相关习题
    exercises.push(
      {
        id: 701,
        title: '词汇',
        description: '请解释下列单词的含义并举例说明：ambitious, significant, implement。',
        difficulty: '基础巩固',
        type: '词汇题',
        knowledgePoints: ['词汇', '核心词汇', '英语学习']
      },
      {
        id: 702,
        title: '语法',
        description: '请分析并纠正下列句子的语法错误：He have been working here for five years.。',
        difficulty: '进阶提升',
        type: '语法题',
        knowledgePoints: ['语法', '时态', '语态', '英语学习']
      },
      {
        id: 703,
        title: '阅读理解',
        description: '请阅读以下短文并回答问题：...（短文内容）。',
        difficulty: '进阶提升',
        type: '阅读题',
        knowledgePoints: ['阅读', '精读', '英语学习']
      },
      {
        id: 704,
        title: '写作',
        description: '请写一篇关于环境保护的短文，不少于150词。',
        difficulty: '真题冲刺',
        type: '写作题',
        knowledgePoints: ['写作', '议论文', '英语学习']
      },
      {
        id: 705,
        title: '口语',
        description: '请准备一段关于大学生活的口语表达，时间2分钟。',
        difficulty: '竞赛拓展',
        type: '口语题',
        knowledgePoints: ['口语', '话题讨论', '英语学习']
      }
    )
  } else {
    // 其他知识点的默认习题
    exercises.push(
      {
        id: 1,
        title: '知识点理解',
        description: `请解释${safeNodeName}的基本概念和应用场景。`,
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: [safeNodeName]
      },
      {
        id: 2,
        title: '知识点应用',
        description: `请举例说明${safeNodeName}在实际问题中的应用。`,
        difficulty: '进阶提升',
        type: '应用题',
        knowledgePoints: [safeNodeName]
      },
      {
        id: 3,
        title: '知识点综合',
        description: `请结合其他相关知识点，分析${safeNodeName}的重要性。`,
        difficulty: '真题冲刺',
        type: '分析题',
        knowledgePoints: [safeNodeName]
      },
      {
        id: 4,
        title: '知识点拓展',
        description: `请探讨${safeNodeName}的最新发展趋势和未来应用。`,
        difficulty: '竞赛拓展',
        type: '拓展题',
        knowledgePoints: [safeNodeName]
      }
    )
  }

  // 根据难度和类型筛选
  let filteredExercises = exercises
  
  if (exerciseFilter !== 'all') {
    const filterMap = {
      'basic': '基础巩固',
      'advanced': '进阶提升',
      'exam': '真题冲刺',
      'competition': '竞赛拓展'
    }
    filteredExercises = filteredExercises.filter(e => e.difficulty === filterMap[exerciseFilter])
  }
  
  if (exerciseTypeFilter !== 'all') {
    filteredExercises = filteredExercises.filter(e => e.type === exerciseTypeFilter)
  }
  
  return filteredExercises
}

// 生成与练习页面兼容的模拟习题
export const generateMockExercisesForPractice = (nodeName, exerciseFilter = 'all', exerciseTypeFilter = 'all') => {
  const safeNodeName = nodeName || '知识节点'
  let exercises = []
  
  // 根据节点名称选择对应的习题集
  if (safeNodeName.includes('计算机网络') || safeNodeName.includes('网络')) {
    exercises = [...networkExercises]
  } else if (safeNodeName.includes('操作系统')) {
    exercises = [...osExercises]
  } else if (safeNodeName.includes('数据库')) {
    exercises = [...dbExercises]
  } else if (safeNodeName.includes('数据结构') || safeNodeName.includes('算法')) {
    exercises = [...algoExercises]
  } else if (safeNodeName.includes('数学') || safeNodeName.includes('微积分') || safeNodeName.includes('线性代数')) {
    exercises = [...mathExercises]
  } else {
    // 默认使用所有类型的习题混合
    exercises = [
      ...networkExercises.slice(0, 2),
      ...osExercises.slice(0, 2),
      ...dbExercises.slice(0, 2),
      ...algoExercises.slice(0, 2),
      ...mathExercises.slice(0, 2)
    ]
  }
  
  // 根据难度筛选
  if (exerciseFilter !== 'all') {
    const difficultyMap = {
      'basic': 'EASY',
      'intermediate': 'MEDIUM',
      'advanced': 'HARD'
    }
    const targetDifficulty = difficultyMap[exerciseFilter]
    if (targetDifficulty) {
      exercises = exercises.filter(ex => ex.difficulty === targetDifficulty)
    }
  }
  
  // 根据类型筛选
  if (exerciseTypeFilter !== 'all') {
    const typeMap = {
      'choice': '选择题',
      'fill': '填空题',
      'essay': '解答题'
    }
    const targetType = typeMap[exerciseTypeFilter]
    if (targetType) {
      exercises = exercises.filter(ex => ex.type === targetType)
    }
  }
  
  // 如果筛选后为空，返回所有习题
  if (exercises.length === 0) {
    exercises = [...networkExercises, ...osExercises, ...dbExercises, ...algoExercises, ...mathExercises]
  }
  
  return exercises
}

// 根据ID获取单个习题
export const getMockExerciseById = (id) => {
  const allExercises = [
    ...networkExercises,
    ...osExercises,
    ...dbExercises,
    ...algoExercises,
    ...mathExercises
  ]
  
  // 先尝试精确匹配
  let exercise = allExercises.find(ex => ex.id === id)
  
  // 如果没有找到，根据ID前缀返回对应类型的随机习题
  if (!exercise) {
    if (id.toString().startsWith('net') || id.toString().includes('网络')) {
      exercise = networkExercises[0]
    } else if (id.toString().startsWith('os') || id.toString().includes('操作系统')) {
      exercise = osExercises[0]
    } else if (id.toString().startsWith('db') || id.toString().includes('数据库')) {
      exercise = dbExercises[0]
    } else if (id.toString().startsWith('algo') || id.toString().includes('算法')) {
      exercise = algoExercises[0]
    } else if (id.toString().startsWith('math') || id.toString().includes('数学')) {
      exercise = mathExercises[0]
    } else {
      // 默认返回第一个习题
      exercise = allExercises[0]
    }
    
    // 创建一个新的对象，保留原始ID
    exercise = { ...exercise, id: id }
  }
  
  return exercise
}
