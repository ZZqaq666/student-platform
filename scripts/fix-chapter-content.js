const fs = require('fs')

function genContent(bookTitle, chapterTitle, sectionTitle) {
  return `<div class="content-section">
  <h2>${sectionTitle}</h2>
  <p>本节是"${bookTitle}"中"${chapterTitle}"的重要内容。通过本节学习，你将掌握${sectionTitle}的核心概念和基本原理，为后续学习奠定基础。</p>
</div>
<div class="content-section">
  <h3>一、知识要点</h3>
  <ul>
    <li>理解${sectionTitle}的基本概念和定义</li>
    <li>掌握相关的定理、公式及适用条件</li>
    <li>学会运用所学知识分析和解决实际问题</li>
  </ul>
  <div class="highlight"><strong>学习提示：</strong>本节内容较为基础但十分重要，建议结合例题加深理解。</div>
</div>
<div class="content-section">
  <h3>二、核心内容</h3>
  <p>${sectionTitle}是${bookTitle}课程中的关键知识模块。学习时需要注意概念之间的内在联系，理解每个知识点在整个知识体系中的位置和作用。</p>
</div>
<div class="content-section">
  <h3>三、典型例题</h3>
  <div class="example"><strong>例题：</strong>请结合本节所学知识，思考${sectionTitle}在实际问题中的应用。</div>
</div>
<div class="content-section">
  <h3>四、本节小结</h3>
  <p>本节学习了${sectionTitle}的主要内容。</p>
  <div class="highlight"><strong>重点回顾：</strong><ul><li>掌握基本概念</li><li>理解核心原理</li><li>能够运用知识解决问题</li></ul></div>
</div>`
}

// Only books that actually exist in DB
const books = [
  { id: 213, title: '操作系统原理', chapters: [
    { title: '第一章 操作系统概述', sections: ['第一节 操作系统的定义与功能', '第二节 操作系统的发展历史', '第三节 操作系统的结构设计'] },
    { title: '第二章 进程管理', sections: ['第一节 进程的概念与状态', '第二节 进程调度算法', '第三节 进程同步与互斥', '第四节 死锁'] },
    { title: '第三章 内存管理', sections: ['第一节 内存管理的基本概念', '第二节 分页与分段管理', '第三节 虚拟内存技术', '第四节 页面置换算法'] },
    { title: '第四章 文件系统', sections: ['第一节 文件与文件系统', '第二节 文件的逻辑与物理结构', '第三节 文件存储空间管理'] },
    { title: '第五章 设备管理', sections: ['第一节 I/O硬件与软件层次', '第二节 设备驱动程序', '第三节 磁盘调度算法'] },
  ]},
  { id: 214, title: '数据库系统概论', chapters: [
    { title: '第一章 数据库系统概述', sections: ['第一节 数据库的基本概念', '第二节 数据模型', '第三节 数据库系统的结构'] },
    { title: '第二章 关系数据库', sections: ['第一节 关系数据结构', '第二节 关系的完整性', '第三节 关系代数'] },
    { title: '第三章 SQL语言', sections: ['第一节 数据定义语言', '第二节 数据查询语言', '第三节 数据操纵语言', '第四节 视图与索引'] },
    { title: '第四章 数据库设计', sections: ['第一节 需求分析', '第二节 概念结构设计', '第三节 逻辑结构设计', '第四节 规范化理论'] },
    { title: '第五章 事务管理', sections: ['第一节 事务的基本概念', '第二节 并发控制', '第三节 数据库恢复技术'] },
  ]},
  { id: 216, title: '离散数学', chapters: [
    { title: '第一章 集合论基础', sections: ['第一节 集合的基本概念', '第二节 集合的运算', '第三节 关系与函数'] },
    { title: '第二章 命题逻辑', sections: ['第一节 命题与联结词', '第二节 命题公式与真值表', '第三节 逻辑等价与蕴含'] },
    { title: '第三章 谓词逻辑', sections: ['第一节 谓词与量词', '第二节 谓词公式', '第三节 推理理论'] },
    { title: '第四章 图论', sections: ['第一节 图的基本概念', '第二节 欧拉图与哈密顿图', '第三节 树与生成树', '第四节 最短路径问题'] },
    { title: '第五章 代数系统', sections: ['第一节 代数系统的基本概念', '第二节 群与半群', '第三节 环与域', '第四节 格与布尔代数'] },
  ]},
  { id: 217, title: '经济学原理', chapters: [
    { title: '第一章 经济学导论', sections: ['第一节 经济学的定义与基本问题', '第二节 机会成本与边际分析', '第三节 经济模型'] },
    { title: '第二章 供求理论', sections: ['第一节 需求定理', '第二节 供给定理', '第三节 市场均衡', '第四节 弹性理论'] },
    { title: '第三章 消费者行为', sections: ['第一节 效用与边际效用', '第二节 预算约束与消费者选择', '第三节 消费者剩余'] },
    { title: '第四章 生产者行为', sections: ['第一节 生产函数', '第二节 成本理论', '第三节 利润最大化'] },
    { title: '第五章 市场结构', sections: ['第一节 完全竞争市场', '第二节 垄断市场', '第三节 垄断竞争与寡头'] },
  ]},
  { id: 221, title: '马克思主义基本原理', chapters: [
    { title: '第一章 世界的物质性', sections: ['第一节 物质世界的客观存在', '第二节 物质世界的普遍联系', '第三节 对立统一规律', '第四节 质量互变规律'] },
    { title: '第二章 认识论', sections: ['第一节 实践是认识的基础', '第二节 认识的辩证过程', '第三节 真理与价值'] },
    { title: '第三章 人类社会', sections: ['第一节 社会基本矛盾', '第二节 社会形态更替', '第三节 人民群众的作用'] },
    { title: '第四章 资本主义', sections: ['第一节 商品与货币', '第二节 剩余价值理论', '第三节 资本主义经济危机'] },
  ]},
]

let sql = `-- Fix chapter content for books 213, 214, 216, 217, 221
-- Generated for actual DB book IDs and titles

`
let nextId = 10000

for (const book of books) {
  for (const chapter of book.chapters) {
    const chapterId = nextId++
    const sortOrder = parseInt(chapter.sections.length > 0 ? String(Math.floor(Math.random() * 90) + 10) : '10')

    const chapterContent = genContent(book.title, chapter.title, chapter.title)
    sql += `INSERT INTO book_chapter (id, book_id, title, content, sort_order, parent_id, status, created_at, updated_at) VALUES (${chapterId}, ${book.id}, '${chapter.title}', '${escapeSql(chapterContent)}', ${sortOrder}, NULL, 'ACTIVE', NOW(), NOW());\n`

    for (let i = 0; i < chapter.sections.length; i++) {
      const sectionId = nextId++
      const sectionTitle = chapter.sections[i]
      const sectionContent = genContent(book.title, chapter.title, sectionTitle)
      sql += `INSERT INTO book_chapter (id, book_id, title, content, sort_order, parent_id, status, created_at, updated_at) VALUES (${sectionId}, ${book.id}, '${sectionTitle}', '${escapeSql(sectionContent)}', ${(i + 1) * 10}, ${chapterId}, 'ACTIVE', NOW(), NOW());\n`
    }
  }
  sql += '\n'
}

const outputPath = 'student-platform-backend/src/main/resources/sql/fix_chapter_content.sql'
fs.writeFileSync(outputPath, sql)
console.log(`Fix SQL written to: ${outputPath}`)
console.log(`Generated statements for books: ${books.map(b => b.id).join(', ')}`)

function escapeSql(str) {
  return str.replace(/'/g, "''").replace(/\n/g, '\\n')
}
