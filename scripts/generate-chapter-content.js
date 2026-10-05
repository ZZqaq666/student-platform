const fs = require('fs');
const path = require('path');

function escapeSql(str) {
  return str.replace(/'/g, "''");
}

function generateSectionContent(bookTitle, chapterTitle, sectionTitle, isParent) {
  if (isParent) {
    return `<div class="content-section">
  <h2>${chapterTitle}</h2>
  <p>本章是"${bookTitle}"的重要组成部分，系统介绍了${sectionTitle.replace(/^[第节]+[一二三四五六七八九十百零\d]+[章节课]+\s*/, '')}的核心知识。通过本章的学习，学生将掌握相关基本概念、基本原理和基本方法，为后续章节的学习奠定坚实的基础。</p>
  <div class="highlight">
    <p><strong>学习目标：</strong></p>
    <ul>
      <li>理解本章的基本概念和定义</li>
      <li>掌握核心定理和公式</li>
      <li>能够运用所学知识解决实际问题</li>
      <li>培养逻辑思维和数学建模能力</li>
    </ul>
  </div>
  <p>本章内容安排由浅入深，循序渐进，注重理论与实践的结合。各节内容既相互独立又紧密联系，建议学习者按顺序学习，确保知识的系统性和完整性。</p>
</div>`;
  }

  const sectionClean = sectionTitle.replace(/^[第节]+[一二三四五六七八九十百零\d]+[章节课]\s*/, '');
  return `<div class="content-section">
  <h2>${sectionTitle}</h2>

  <h3>本节导学</h3>
  <p>本节是"${bookTitle}"中"${chapterTitle}"的重要环节，主要讲解${sectionClean}的相关知识。本节内容在整章中起到承上启下的作用，是理解后续内容的基础。学习本节需要掌握基本概念和基本方法，注重理论联系实际。</p>

  <h3>知识要点</h3>
  <div class="highlight">
    <ul>
      <li>掌握${sectionClean}的基本概念与定义</li>
      <li>理解${sectionClean}的核心定理与性质</li>
      <li>熟悉${sectionClean}的基本计算方法</li>
      <li>了解${sectionClean}在实际问题中的应用</li>
      <li>培养分析和解决相关问题的能力</li>
    </ul>
  </div>

  <h3>核心内容</h3>
  <p>本节将从以下几个方面展开详细讲解：</p>
  <p>首先，介绍${sectionClean}的基本概念，包括定义、性质和几何意义。这些基本概念是理解整个章节的基石，需要深刻领会。</p>
  <p>其次，讨论相关的定理和公式，给出严格的数学表述和证明思路。定理的掌握不仅要记住结论，更要理解其适用条件和推导过程。</p>
  <p>最后，通过典型例题演示${sectionClean}的应用方法，帮助学习者将理论知识转化为实际解题能力。</p>

  <h3>典型例题</h3>
  <div class="example">
    <p><strong>例题1：</strong>请根据所学知识，分析并解决以下问题。</p>
    <p>【分析】本题主要考查${sectionClean}的基本概念和基本方法的掌握情况。解题的关键在于正确理解题意，灵活运用所学公式和定理。</p>
    <p>【解答】按照解题步骤，首先确定问题类型，然后选择合适的方法，最后进行严谨的推导和计算。</p>
  </div>
  <div class="example">
    <p><strong>例题2：</strong>综合应用题。</p>
    <p>【分析】本题综合性强，需要灵活运用多个知识点。建议先回顾相关基础知识，再逐步分析题目条件。</p>
    <p>【解答】详细解答过程体现了综合运用知识的能力，展示了规范的解题思路和方法。</p>
  </div>

  <h3>本节小结</h3>
  <p>本节学习了${sectionClean}的主要内容，包括基本概念、核心定理和典型应用。重点掌握以下内容：（1）${sectionClean}的定义和基本性质；（2）相关的计算方法与技巧；（3）在实际问题中的应用思路。建议课后及时复习，通过练习巩固所学知识。</p>

  <h3>课后思考</h3>
  <ul>
    <li>${sectionClean}的核心思想是什么？与其他章节有什么联系？</li>
    <li>尝试总结${sectionClean}的解题方法和技巧。</li>
    <li>思考${sectionClean}在现实生活中有哪些应用场景？</li>
  </ul>
</div>`;
}

const books = [
  {
    id: 205, title: "高等数学（上册）",
    chapters: [
      { title: "第一章 函数与极限", sections: [
        { title: "第一节 映射与函数" }, { title: "第二节 数列的极限" },
        { title: "第三节 函数的极限" }, { title: "第四节 极限运算法则" },
        { title: "第五节 极限存在准则" }, { title: "第六节 无穷小的比较" },
        { title: "第七节 函数的连续性与间断点" }, { title: "第八节 闭区间上连续函数的性质" }
      ]},
      { title: "第二章 导数与微分", sections: [
        { title: "第一节 导数概念" }, { title: "第二节 函数的求导法则" },
        { title: "第三节 高阶导数" }, { title: "第四节 隐函数及由参数方程所确定的函数的导数" },
        { title: "第五节 函数的微分" }
      ]},
      { title: "第三章 中值定理与导数的应用", sections: [
        { title: "第一节 微分中值定理" }, { title: "第二节 洛必达法则" },
        { title: "第三节 泰勒公式" }, { title: "第四节 函数的单调性与凹凸性" },
        { title: "第五节 函数的极值与最值" }
      ]}
    ]
  },
  {
    id: 206, title: "高等数学（下册）",
    chapters: [
      { title: "第八章 向量代数与空间解析几何", sections: [
        { title: "第一节 向量及其线性运算" }, { title: "第二节 数量积与向量积" },
        { title: "第三节 平面与直线方程" }, { title: "第四节 曲面与空间曲线" }
      ]},
      { title: "第九章 多元函数微分法及其应用", sections: [
        { title: "第一节 多元函数的基本概念" }, { title: "第二节 偏导数与全微分" },
        { title: "第三节 多元复合函数与隐函数求导" }, { title: "第四节 方向导数与梯度" },
        { title: "第五节 多元函数的极值" }
      ]},
      { title: "第十章 重积分", sections: [
        { title: "第一节 二重积分的概念与性质" }, { title: "第二节 二重积分的计算" },
        { title: "第三节 三重积分的概念与计算" }, { title: "第四节 重积分的应用" }
      ]},
      { title: "第十一章 曲线积分与曲面积分", sections: [
        { title: "第一节 曲线积分的概念与计算" }, { title: "第二节 格林公式及其应用" },
        { title: "第三节 曲面积分的概念与计算" }
      ]},
      { title: "第十二章 无穷级数", sections: [
        { title: "第一节 常数项级数的概念与性质" }, { title: "第二节 正项级数的审敛法" },
        { title: "第三节 幂级数" }, { title: "第四节 傅里叶级数" }
      ]}
    ]
  },
  {
    id: 207, title: "线性代数",
    chapters: [
      { title: "第一章 行列式", sections: [
        { title: "第一节 二阶与三阶行列式" }, { title: "第二节 n阶行列式的定义" },
        { title: "第三节 行列式的性质与计算" }, { title: "第四节 克莱默法则" }
      ]},
      { title: "第二章 矩阵及其运算", sections: [
        { title: "第一节 矩阵的概念" }, { title: "第二节 矩阵的运算" },
        { title: "第三节 逆矩阵" }, { title: "第四节 分块矩阵" }
      ]},
      { title: "第三章 矩阵的秩与线性方程组", sections: [
        { title: "第一节 矩阵的秩" }, { title: "第二节 线性方程组的解" },
        { title: "第三节 向量组的线性相关性" }
      ]},
      { title: "第四章 向量空间与线性变换", sections: [
        { title: "第一节 向量空间的定义与性质" }, { title: "第二节 基维数与坐标" },
        { title: "第三节 线性变换" }
      ]},
      { title: "第五章 特征值与特征向量", sections: [
        { title: "第一节 特征值与特征向量的概念" }, { title: "第二节 相似矩阵与矩阵的对角化" },
        { title: "第三节 实对称矩阵的对角化" }
      ]}
    ]
  },
  {
    id: 208, title: "概率论与数理统计",
    chapters: [
      { title: "第一章 概率论的基本概念", sections: [
        { title: "第一节 随机事件与样本空间" }, { title: "第二节 概率的定义与性质" },
        { title: "第三节 条件概率与乘法公式" }, { title: "第四节 全概率公式与贝叶斯公式" }
      ]},
      { title: "第二章 随机变量及其分布", sections: [
        { title: "第一节 随机变量的概念" }, { title: "第二节 离散型随机变量" },
        { title: "第三节 连续型随机变量" }, { title: "第四节 随机变量函数的分布" }
      ]},
      { title: "第三章 多维随机变量", sections: [
        { title: "第一节 二维随机变量" }, { title: "第二节 边缘分布与条件分布" },
        { title: "第三节 随机变量的独立性" }, { title: "第四节 两个随机变量函数的分布" }
      ]},
      { title: "第四章 随机变量的数字特征", sections: [
        { title: "第一节 数学期望" }, { title: "第二节 方差" },
        { title: "第三节 协方差与相关系数" }, { title: "第四节 大数定律与中心极限定理" }
      ]}
    ]
  },
  {
    id: 209, title: "大学物理（上册）",
    chapters: [
      { title: "第一章 质点运动学", sections: [
        { title: "第一节 参考系与坐标系" }, { title: "第二节 位移速度与加速度" },
        { title: "第三节 圆周运动与角量描述" }, { title: "第四节 相对运动" }
      ]},
      { title: "第二章 牛顿运动定律", sections: [
        { title: "第一节 牛顿三定律" }, { title: "第二节 力学中常见的力" },
        { title: "第三节 牛顿定律的应用" }, { title: "第四节 惯性系与非惯性系" }
      ]},
      { title: "第三章 功与能", sections: [
        { title: "第一节 功与功率" }, { title: "第二节 动能定理" },
        { title: "第三节 保守力与势能" }, { title: "第四节 机械能守恒定律" }
      ]},
      { title: "第四章 动量与角动量", sections: [
        { title: "第一节 动量与动量定理" }, { title: "第二节 动量守恒定律" },
        { title: "第三节 角动量与角动量守恒" }
      ]},
      { title: "第五章 振动与波动", sections: [
        { title: "第一节 简谐振动" }, { title: "第二节 阻尼振动与受迫振动" },
        { title: "第三节 机械波的产生与传播" }, { title: "第四节 波的干涉与衍射" }
      ]}
    ]
  },
  {
    id: 210, title: "大学物理（下册）",
    chapters: [
      { title: "第九章 静电场", sections: [
        { title: "第一节 电荷与电场" }, { title: "第二节 高斯定理" },
        { title: "第三节 电势与电势差" }, { title: "第四节 静电场中的导体" }
      ]},
      { title: "第十章 恒定磁场", sections: [
        { title: "第一节 磁感应强度" }, { title: "第二节 毕奥萨伐尔定律" },
        { title: "第三节 安培环路定理" }, { title: "第四节 磁场对电流的作用" }
      ]},
      { title: "第十一章 电磁感应", sections: [
        { title: "第一节 法拉第电磁感应定律" }, { title: "第二节 动生电动势与感生电动势" },
        { title: "第三节 自感与互感" }, { title: "第四节 麦克斯韦方程组" }
      ]},
      { title: "第十二章 波动光学", sections: [
        { title: "第一节 光的干涉" }, { title: "第二节 光的衍射" },
        { title: "第三节 光的偏振" }
      ]}
    ]
  },
  {
    id: 211, title: "数据结构与算法",
    chapters: [
      { title: "第一章 绪论", sections: [
        { title: "第一节 数据结构的基本概念" }, { title: "第二节 算法与算法分析" },
        { title: "第三节 数据结构的发展简史" }
      ]},
      { title: "第二章 线性表", sections: [
        { title: "第一节 线性表的定义与基本操作" }, { title: "第二节 顺序表" },
        { title: "第三节 链表" }, { title: "第四节 线性表的应用" }
      ]},
      { title: "第三章 栈与队列", sections: [
        { title: "第一节 栈" }, { title: "第二节 队列" },
        { title: "第三节 栈与队列的应用" }
      ]},
      { title: "第四章 树与二叉树", sections: [
        { title: "第一节 树的基本概念" }, { title: "第二节 二叉树" },
        { title: "第三节 二叉树的遍历" }, { title: "第四节 哈夫曼树" }
      ]},
      { title: "第五章 图", sections: [
        { title: "第一节 图的基本概念" }, { title: "第二节 图的存储结构" },
        { title: "第三节 图的遍历" }, { title: "第四节 最短路径与最小生成树" }
      ]},
      { title: "第六章 查找与排序", sections: [
        { title: "第一节 顺序查找与二分查找" }, { title: "第二节 哈希表" },
        { title: "第三节 插入排序与选择排序" }, { title: "第四节 快速排序与归并排序" }
      ]}
    ]
  },
  {
    id: 212, title: "计算机网络",
    chapters: [
      { title: "第一章 计算机网络概述", sections: [
        { title: "第一节 计算机网络的定义与发展" }, { title: "第二节 网络体系结构与协议" },
        { title: "第三节 网络性能指标" }
      ]},
      { title: "第二章 物理层", sections: [
        { title: "第一节 信号与编码" }, { title: "第二节 传输介质" },
        { title: "第三节 信道复用技术" }
      ]},
      { title: "第三章 数据链路层", sections: [
        { title: "第一节 数据链路层的功能" }, { title: "第二节 差错控制" },
        { title: "第三节 流量控制与可靠传输" }, { title: "第四节 局域网与以太网" }
      ]},
      { title: "第四章 网络层", sections: [
        { title: "第一节 IP协议与IP地址" }, { title: "第二节 路由算法" },
        { title: "第三节 ICMP与ARP" }, { title: "第四节 IPv6" }
      ]},
      { title: "第五章 传输层", sections: [
        { title: "第一节 TCP与UDP协议" }, { title: "第二节 TCP可靠传输机制" },
        { title: "第三节 TCP拥塞控制" }
      ]},
      { title: "第六章 应用层", sections: [
        { title: "第一节 DNS域名系统" }, { title: "第二节 HTTP协议" },
        { title: "第三节 电子邮件与FTP" }
      ]}
    ]
  },
  {
    id: 213, title: "计算机操作系统",
    chapters: [
      { title: "第一章 操作系统概述", sections: [
        { title: "第一节 操作系统的定义与功能" }, { title: "第二节 操作系统的发展历史" },
        { title: "第三节 操作系统的结构设计" }
      ]},
      { title: "第二章 进程管理", sections: [
        { title: "第一节 进程的概念与状态" }, { title: "第二节 进程调度" },
        { title: "第三节 进程同步与互斥" }, { title: "第四节 死锁" }
      ]},
      { title: "第三章 内存管理", sections: [
        { title: "第一节 内存管理的基本概念" }, { title: "第二节 分页与分段存储管理" },
        { title: "第三节 虚拟内存" }, { title: "第四节 页置换算法" }
      ]},
      { title: "第四章 文件系统", sections: [
        { title: "第一节 文件与文件系统" }, { title: "第二节 文件的逻辑结构与物理结构" },
        { title: "第三节 文件存储空间管理" }, { title: "第四节 文件系统的可靠性" }
      ]},
      { title: "第五章 输入输出管理", sections: [
        { title: "第一节 IO硬件与软件层次" }, { title: "第二节 设备驱动程序" },
        { title: "第三节 磁盘调度算法" }
      ]}
    ]
  },
  {
    id: 214, title: "C语言程序设计",
    chapters: [
      { title: "第一章 C语言概述", sections: [
        { title: "第一节 程序设计与C语言" }, { title: "第二节 基本语法元素" },
        { title: "第三节 数据类型与常量变量" }
      ]},
      { title: "第二章 基本语句与顺序结构", sections: [
        { title: "第一节 输入输出语句" }, { title: "第二节 顺序结构程序设计" },
        { title: "第三节 运算符与表达式" }
      ]},
      { title: "第三章 选择结构", sections: [
        { title: "第一节 if语句" }, { title: "第二节 switch语句" },
        { title: "第三节 条件运算符" }
      ]},
      { title: "第四章 循环结构", sections: [
        { title: "第一节 while循环" }, { title: "第二节 for循环" },
        { title: "第三节 循环嵌套与breakcontinue" }
      ]},
      { title: "第五章 数组", sections: [
        { title: "第一节 一维数组" }, { title: "第二节 二维数组" },
        { title: "第三节 字符数组与字符串" }
      ]},
      { title: "第六章 函数", sections: [
        { title: "第一节 函数的定义与调用" }, { title: "第二节 函数参数与返回值" },
        { title: "第三节 递归函数" }, { title: "第四节 变量作用域与存储类别" }
      ]},
      { title: "第七章 指针", sections: [
        { title: "第一节 指针的概念" }, { title: "第二节 指针与数组" },
        { title: "第三节 指针与函数" }, { title: "第四节 动态内存分配" }
      ]}
    ]
  },
  {
    id: 215, title: "经济学原理（微观）",
    chapters: [
      { title: "第一章 经济学导论", sections: [
        { title: "第一节 经济学的定义与基本问题" }, { title: "第二节 机会成本与边际分析" },
        { title: "第三节 经济模型与图表" }
      ]},
      { title: "第二章 供求理论", sections: [
        { title: "第一节 需求与供给" }, { title: "第二节 市场均衡" },
        { title: "第三节 弹性及其应用" }
      ]},
      { title: "第三章 消费者行为理论", sections: [
        { title: "第一节 效用与边际效用" }, { title: "第二节 预算约束与消费者选择" },
        { title: "第三节 消费者剩余" }
      ]},
      { title: "第四章 生产者行为理论", sections: [
        { title: "第一节 生产函数" }, { title: "第二节 成本曲线" },
        { title: "第三节 利润最大化" }
      ]},
      { title: "第五章 市场结构", sections: [
        { title: "第一节 完全竞争市场" }, { title: "第二节 垄断市场" },
        { title: "第三节 垄断竞争与寡头垄断" }
      ]}
    ]
  },
  {
    id: 216, title: "马克思主义基本原理",
    chapters: [
      { title: "第一章 世界的物质性及其发展规律", sections: [
        { title: "第一节 物质世界的客观存在" }, { title: "第二节 物质世界的普遍联系与发展" },
        { title: "第三节 对立统一规律" }, { title: "第四节 质量互变规律与否定之否定规律" }
      ]},
      { title: "第二章 认识的本质及其发展规律", sections: [
        { title: "第一节 实践是认识的基础" }, { title: "第二节 认识的辩证过程" },
        { title: "第三节 真理与价值" }
      ]},
      { title: "第三章 人类社会及其发展规律", sections: [
        { title: "第一节 社会基本矛盾" }, { title: "第二节 社会形态更替" },
        { title: "第三节 人民群众在历史发展中的作用" }
      ]},
      { title: "第四章 资本主义的形成及其本质", sections: [
        { title: "第一节 商品与货币" }, { title: "第二节 剩余价值的生产" },
        { title: "第三节 资本主义的经济危机" }
      ]}
    ]
  },
  {
    id: 217, title: "新视野大学英语（第三册）",
    chapters: [
      { title: "Unit 1 Ways of Learning", sections: [
        { title: "Section A Learning Chinese-Style" }, { title: "Section B The Art of Parenting" }
      ]},
      { title: "Unit 2 Values", sections: [
        { title: "Section A A Life of Values" }, { title: "Section B The Power of Kindness" }
      ]},
      { title: "Unit 3 The Internet", sections: [
        { title: "Section A The Information Superhighway" }, { title: "Section B Online Communication" }
      ]},
      { title: "Unit 4 Creativity", sections: [
        { title: "Section A Nurturing Creativity" }, { title: "Section B Creative Thinking" }
      ]},
      { title: "Unit 5 American Dream", sections: [
        { title: "Section A The American Dream" }, { title: "Section B Pursuing Happiness" }
      ]},
      { title: "Unit 6 Globalization", sections: [
        { title: "Section A A Globalized World" }, { title: "Section B Cultural Exchange" }
      ]}
    ]
  },
  {
    id: 218, title: "毛泽东思想概论",
    chapters: [
      { title: "第一章 毛泽东思想的形成与发展", sections: [
        { title: "第一节 毛泽东思想产生的时代背景" }, { title: "第二节 毛泽东思想的科学内涵" },
        { title: "第三节 毛泽东思想的历史地位" }
      ]},
      { title: "第二章 新民主主义革命理论", sections: [
        { title: "第一节 新民主主义革命总路线" }, { title: "第二节 新民主主义革命的基本纲领" },
        { title: "第三节 农村包围城市的道路" }
      ]},
      { title: "第三章 社会主义改造理论", sections: [
        { title: "第一节 过渡时期的总路线" }, { title: "第二节 农业与手工业的社会主义改造" },
        { title: "第三节 资本主义工商业的社会主义改造" }
      ]}
    ]
  },
  {
    id: 219, title: "体育与健康",
    chapters: [
      { title: "第一章 体育理论基础", sections: [
        { title: "第一节 体育的概念与功能" }, { title: "第二节 运动生理学基础" },
        { title: "第三节 运动损伤的预防与处理" }
      ]},
      { title: "第二章 球类运动", sections: [
        { title: "第一节 篮球" }, { title: "第二节 排球" },
        { title: "第三节 足球" }, { title: "第四节 乒乓球" }
      ]},
      { title: "第三章 田径运动", sections: [
        { title: "第一节 短跑与接力" }, { title: "第二节 跳远与跳高" },
        { title: "第三节 投掷项目" }
      ]}
    ]
  },
  {
    id: 220, title: "形势与政策",
    chapters: [
      { title: "第一章 当前国际形势", sections: [
        { title: "第一节 国际格局的变化" }, { title: "第二节 大国关系" },
        { title: "第三节 全球性挑战" }
      ]},
      { title: "第二章 中国外交政策", sections: [
        { title: "第一节 独立自主的和平外交" }, { title: "第二节 一带一路倡议" },
        { title: "第三节 人类命运共同体" }
      ]},
      { title: "第三章 国内发展形势", sections: [
        { title: "第一节 经济发展新常态" }, { title: "第二节 科技创新战略" },
        { title: "第三节 生态文明建设" }
      ]}
    ]
  },
  {
    id: 221, title: "Python程序设计基础",
    chapters: [
      { title: "第一章 Python语言概述", sections: [
        { title: "第一节 Python简介" }, { title: "第二节 开发环境搭建" },
        { title: "第三节 第一个Python程序" }
      ]},
      { title: "第二章 数据类型与运算符", sections: [
        { title: "第一节 基本数据类型" }, { title: "第二节 变量与赋值" },
        { title: "第三节 运算符与表达式" }
      ]},
      { title: "第三章 流程控制", sections: [
        { title: "第一节 条件语句" }, { title: "第二节 循环语句" },
        { title: "第三节 列表与字典" }
      ]},
      { title: "第四章 函数与模块", sections: [
        { title: "第一节 函数的定义与调用" }, { title: "第二节 参数与返回值" },
        { title: "第三节 模块与包" }
      ]}
    ]
  },
  {
    id: 222, title: "新视野大学英语（第四册）",
    chapters: [
      { title: "Unit 1 Love and Friendship", sections: [
        { title: "Section A The Nature of Love" }, { title: "Section B True Friendship" }
      ]},
      { title: "Unit 2 Environment", sections: [
        { title: "Section A Climate Change" }, { title: "Section B Protecting Our Planet" }
      ]},
      { title: "Unit 3 Technology and Society", sections: [
        { title: "Section A The Digital Age" }, { title: "Section B Social Media" }
      ]},
      { title: "Unit 4 Cultural Heritage", sections: [
        { title: "Section A Preserving Culture" }, { title: "Section B Cultural Diversity" }
      ]}
    ]
  },
  {
    id: 223, title: "大学生心理健康教育",
    chapters: [
      { title: "第一章 心理健康概述", sections: [
        { title: "第一节 心理健康的标准" }, { title: "第二节 大学生心理发展特点" },
        { title: "第三节 心理咨询与求助" }
      ]},
      { title: "第二章 自我认知与人格", sections: [
        { title: "第一节 自我意识" }, { title: "第二节 人格特征与塑造" },
        { title: "第三节 情绪管理" }
      ]},
      { title: "第三章 人际交往", sections: [
        { title: "第一节 人际关系的建立" }, { title: "第二节 沟通技巧" },
        { title: "第三节 冲突处理" }
      ]},
      { title: "第四章 压力与挫折应对", sections: [
        { title: "第一节 压力管理" }, { title: "第二节 挫折应对" },
        { title: "第三节 时间管理" }
      ]}
    ]
  },
  {
    id: 224, title: "体育与健康（实践）",
    chapters: [
      { title: "第一章 体能训练", sections: [
        { title: "第一节 力量训练" }, { title: "第二节 耐力训练" },
        { title: "第三节 柔韧性与协调性" }
      ]},
      { title: "第二章 运动技能", sections: [
        { title: "第一节 游泳" }, { title: "第二节 武术" },
        { title: "第三节 健美操" }
      ]},
      { title: "第三章 健康管理", sections: [
        { title: "第一节 营养与健康" }, { title: "第二节 运动处方" },
        { title: "第三节 健康生活方式" }
      ]}
    ]
  }
];

// Book 205 existing chapter IDs mapping (from the task description)
const book205ExistingIDs = {
  "第一章 函数与极限": 4,
  "第一节 映射与函数": 5,
  "第二节 数列的极限": 6,
  "第三节 函数的极限": 7,
  "第四节 极限运算法则": 8,
  "第五节 极限存在准则": 9,
  "第六节 无穷小的比较": 10,
  "第七节 函数的连续性与间断点": 11,
  "第八节 闭区间上连续函数的性质": 12,
  "第二章 导数与微分": 13,
  "第一节 导数概念": 14,
  "第二节 函数的求导法则": 15,
  "第三节 高阶导数": 16,
  "第四节 隐函数及由参数方程所确定的函数的导数": 17,
  "第五节 函数的微分": 18,
  "第三章 中值定理与导数的应用": 19,
  "第一节 微分中值定理": 20,
  "第二节 洛必达法则": 21,
  "第三节 泰勒公式": 22,
  "第四节 函数的单调性与凹凸性": 23,
  "第五节 函数的极值与最值": 24
};

function parseChapterIndex(title) {
  const match = title.match(/^[第]?([一二三四五六七八九十百零\d]+)[章课]/);
  if (!match) return null;
  const numStr = match[1];
  const numMap = {
    '一': 1, '二': 2, '三': 3, '四': 4, '五': 5,
    '六': 6, '七': 7, '八': 8, '九': 9, '十': 10,
    '十一': 11, '十二': 12, '十三': 13, '十四': 14, '十五': 15,
    '十六': 16, '十七': 17, '十八': 18, '十九': 19, '二十': 20
  };
  if (numMap[numStr] !== undefined) return numMap[numStr];
  const n = parseInt(numStr, 10);
  if (!isNaN(n)) return n;

  // handle "Unit N" - return N
  const unitMatch = title.match(/^Unit\s+(\d+)/i);
  if (unitMatch) return parseInt(unitMatch[1], 10);

  return null;
}

function generateSQL() {
  const now = "NOW()";
  const lines = [];
  let nextId = 413;
  let updateCount = 0;
  let insertCount = 0;

  lines.push("-- ============================================================");
  lines.push("-- Chapter Content Data SQL");
  lines.push("-- Generated for book_chapter table");
  lines.push("-- Contains content updates for Book 205 and full inserts for Books 206-224");
  lines.push("-- ============================================================");
  lines.push("");
  lines.push("LOCK TABLES `book_chapter` WRITE;");
  lines.push("/*!40000 ALTER TABLE `book_chapter` DISABLE KEYS */;");
  lines.push("");

  for (const book of books) {
    lines.push(`-- ========================================`);
    lines.push(`-- Book: ${book.title} (ID: ${book.id})`);
    lines.push(`-- ========================================`);
    lines.push("");

    for (let ci = 0; ci < book.chapters.length; ci++) {
      const ch = book.chapters[ci];
      const chOrder = (ci + 1) * 10;

      if (book.id === 205) {
        // UPDATE existing chapters for book 205
        const chId = book205ExistingIDs[ch.title];
        if (!chId) {
          console.error(`WARNING: Could not find existing ID for book 205 chapter "${ch.title}"`);
          continue;
        }
        const content = generateSectionContent(book.title, ch.title, ch.title, true);
        lines.push(`UPDATE \`book_chapter\` SET \`content\` = '${escapeSql(content)}' WHERE \`id\` = ${chId};`);
        updateCount++;

        for (let si = 0; si < ch.sections.length; si++) {
          const sec = ch.sections[si];
          const secId = book205ExistingIDs[sec.title];
          if (!secId) {
            console.error(`WARNING: Could not find existing ID for book 205 section "${sec.title}"`);
            continue;
          }
          const secContent = generateSectionContent(book.title, ch.title, sec.title, false);
          lines.push(`UPDATE \`book_chapter\` SET \`content\` = '${escapeSql(secContent)}' WHERE \`id\` = ${secId};`);
          updateCount++;
        }
      } else {
        // INSERT new chapters for books 206-224
        const chId = nextId++;
        const content = generateSectionContent(book.title, ch.title, ch.title, true);
        lines.push(`INSERT INTO \`book_chapter\` VALUES (${chId},${book.id},'${escapeSql(ch.title)}','${escapeSql(content)}',${chOrder},NULL,'ACTIVE',${now},${now});`);
        insertCount++;

        for (let si = 0; si < ch.sections.length; si++) {
          const sec = ch.sections[si];
          const secId = nextId++;
          const secOrder = chOrder + (si + 1);
          const secContent = generateSectionContent(book.title, ch.title, sec.title, false);
          lines.push(`INSERT INTO \`book_chapter\` VALUES (${secId},${book.id},'${escapeSql(sec.title)}','${escapeSql(secContent)}',${secOrder},${chId},'ACTIVE',${now},${now});`);
          insertCount++;
        }
      }
      lines.push("");
    }
  }

  lines.push("/*!40000 ALTER TABLE `book_chapter` ENABLE KEYS */;");
  lines.push("UNLOCK TABLES;");

  return { sql: lines.join("\n"), updateCount, insertCount };
}

const result = generateSQL();

const outDir = "D:\\code\\chuanzhibei\\student-platform-backend\\src\\main\\resources\\sql";
if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}
const outPath = path.join(outDir, "chapter_content_data.sql");
fs.writeFileSync(outPath, result.sql, "utf8");

const stats = fs.statSync(outPath);
console.log(`SQL file generated successfully!`);
console.log(`File: ${outPath}`);
console.log(`Size: ${(stats.size / 1024).toFixed(1)} KB`);
console.log(`UPDATE statements: ${result.updateCount}`);
console.log(`INSERT statements: ${result.insertCount}`);
