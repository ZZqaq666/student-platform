-- 知识节点表
CREATE TABLE knowledge_node (
    id          bigint auto_increment comment '知识节点ID'
        primary key,
    book_id     bigint                                not null comment '书籍ID',
    parent_id   bigint                                null comment '父节点ID',
    name        varchar(100)                          not null comment '知识点名称',
    description text                                  null comment '知识点描述',
    content     text                                  null comment '知识点详细内容',
    level       int         default 1                 not null comment '层级',
    sort_order  int         default 0                 not null comment '排序',
    status      varchar(20) default 'ACTIVE'          not null comment '状态：ACTIVE/DISABLED',
    created_at  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint knowledge_node_ibfk_1
        foreign key (book_id) references book (id)
            on delete cascade,
    constraint knowledge_node_ibfk_2
        foreign key (parent_id) references knowledge_node (id)
            on delete set null
) comment '知识节点表';

CREATE INDEX idx_book_id
    on knowledge_node (book_id);

CREATE INDEX idx_parent_id
    on knowledge_node (parent_id);

-- 知识关系表
CREATE TABLE knowledge_relation (
    id             bigint auto_increment comment '关系ID'
        primary key,
    source_node_id bigint                             not null comment '源节点ID',
    target_node_id bigint                             not null comment '目标节点ID',
    relation_type  varchar(50)                        not null comment '关系类型：PREREQUISITE/RELEVANT/SUCCESSOR/PART_OF',
    description    varchar(255)                       null comment '关系描述',
    created_at     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint UKpuhokpjpmrvdilxxstmmlx8wn
        unique (source_node_id, target_node_id, relation_type),
    constraint uk_source_target
        unique (source_node_id, target_node_id, relation_type),
    constraint knowledge_relation_ibfk_1
        foreign key (source_node_id) references knowledge_node (id)
            on delete cascade,
    constraint knowledge_relation_ibfk_2
        foreign key (target_node_id) references knowledge_node (id)
            on delete cascade
) comment '知识关系表';

CREATE INDEX idx_source_node_id
    on knowledge_relation (source_node_id);

CREATE INDEX idx_target_node_id
    on knowledge_relation (target_node_id);

-- 插入示例数据
-- 高等数学
INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(205, null, '高等数学', '大学数学基础课程', 0);
SET @math_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(205, @math_id, '微积分', '研究函数的微分和积分', 1),
(205, @math_id, '线性代数', '研究向量空间和线性变换', 1),
(205, @math_id, '概率论', '研究随机现象', 1),
(205, @math_id, '常微分方程', '研究含有导数的方程', 1);

SET @calc_id = LAST_INSERT_ID() - 3;
SET @linear_id = LAST_INSERT_ID() - 2;
SET @prob_id = LAST_INSERT_ID() - 1;
SET @ode_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(205, @calc_id, '导数', '函数的变化率', 2),
(205, @calc_id, '积分', '导数的逆运算', 2),
(205, @linear_id, '矩阵', '矩形数组', 2),
(205, @linear_id, '行列式', '方阵的标量值', 2),
(205, @prob_id, '随机变量', '取值随机的变量', 2),
(205, @prob_id, '概率分布', '随机变量的取值规律', 2),
(205, @ode_id, '一阶微分方程', '只含一阶导数的方程', 2),
(205, @ode_id, '二阶微分方程', '含二阶导数的方程', 2);

-- 计算机网络
INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(212, null, '计算机网络', '计算机通信技术', 0);
SET @network_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(212, @network_id, '网络协议', '网络通信规则', 1),
(212, @network_id, '网络拓扑', '网络结构', 1),
(212, @network_id, '网络安全', '网络防护技术', 1),
(212, @network_id, '网络管理', '网络运维', 1);

SET @protocol_id = LAST_INSERT_ID() - 3;
SET @topology_id = LAST_INSERT_ID() - 2;
SET @security_id = LAST_INSERT_ID() - 1;

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(212, @protocol_id, 'TCP/IP', '互联网协议', 2),
(212, @protocol_id, 'HTTP/HTTPS', 'Web协议', 2),
(212, @topology_id, '路由算法', '数据包转发算法', 2),
(212, @security_id, '防火墙', '网络安全设备', 2);

-- 操作系统原理
INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(213, null, '操作系统原理', '计算机系统管理', 0);
SET @os_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(213, @os_id, '进程管理', '进程的创建和调度', 1),
(213, @os_id, '内存管理', '内存分配和回收', 1),
(213, @os_id, '文件系统', '文件的组织和管理', 1),
(213, @os_id, '设备管理', '硬件设备的管理', 1);

SET @process_id = LAST_INSERT_ID() - 3;
SET @memory_id = LAST_INSERT_ID() - 2;
SET @file_id = LAST_INSERT_ID() - 1;
SET @device_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(213, @process_id, '进程调度', '进程的优先级调度', 2),
(213, @process_id, '进程同步', '进程间的协调', 2),
(213, @memory_id, '分页存储', '内存分页管理', 2),
(213, @memory_id, '分段存储', '内存分段管理', 2),
(213, @file_id, '文件结构', '文件的组织方式', 2),
(213, @file_id, '文件操作', '文件的读写操作', 2),
(213, @device_id, '设备驱动', '硬件驱动程序', 2),
(213, @device_id, 'I/O控制', '输入输出控制', 2);

-- 数据库系统概论
INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(214, null, '数据库系统概论', '数据库管理系统', 0);
SET @db_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(214, @db_id, '数据库模型', '数据的组织方式', 1),
(214, @db_id, 'SQL语言', '数据库查询语言', 1),
(214, @db_id, '事务管理', '数据操作的原子性', 1),
(214, @db_id, '数据库设计', '数据库的规划和设计', 1);

SET @model_id = LAST_INSERT_ID() - 3;
SET @sql_id = LAST_INSERT_ID() - 2;
SET @tx_id = LAST_INSERT_ID() - 1;
SET @design_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(214, @model_id, '关系模型', '基于表的模型', 2),
(214, @model_id, 'ER模型', '实体关系模型', 2),
(214, @sql_id, '查询语句', '数据查询', 2),
(214, @sql_id, '数据定义', '表结构定义', 2),
(214, @tx_id, 'ACID特性', '事务的四个特性', 2),
(214, @tx_id, '并发控制', '多用户访问控制', 2),
(214, @design_id, '范式理论', '数据规范化', 2),
(214, @design_id, '数据库规划', '数据库的规划和设计', 2);

-- 数据结构与算法
INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(211, null, '数据结构与算法', '数据组织和算法设计', 0);
SET @ds_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(211, @ds_id, '线性结构', '线性数据组织', 1),
(211, @ds_id, '树形结构', '层次数据组织', 1),
(211, @ds_id, '图结构', '网络数据组织', 1),
(211, @ds_id, '排序算法', '数据排序方法', 1);

SET @linear_struct_id = LAST_INSERT_ID() - 3;
SET @tree_struct_id = LAST_INSERT_ID() - 2;
SET @graph_struct_id = LAST_INSERT_ID() - 1;
SET @sort_id = LAST_INSERT_ID();

INSERT INTO knowledge_node (book_id, parent_id, name, description, level) VALUES
(211, @linear_struct_id, '数组', '连续存储结构', 2),
(211, @linear_struct_id, '链表', '链式存储结构', 2),
(211, @tree_struct_id, '二叉树', '二叉树形结构', 2),
(211, @tree_struct_id, '平衡树', '平衡的树形结构', 2),
(211, @graph_struct_id, '图的遍历', '图的访问方法', 2),
(211, @graph_struct_id, '最短路径', '图中最短路径算法', 2),
(211, @sort_id, '快速排序', '高效排序算法', 2),
(211, @sort_id, '归并排序', '稳定排序算法', 2);

-- 知识关系
-- 高等数学关系
INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@math_id, @calc_id, 'PART_OF', '微积分是高等数学的一部分'),
(@math_id, @linear_id, 'PART_OF', '线性代数是高等数学的一部分'),
(@math_id, @prob_id, 'PART_OF', '概率论是高等数学的一部分'),
(@math_id, @ode_id, 'PART_OF', '常微分方程是高等数学的一部分');

-- 获取导数和积分的ID
SET @derivative_id = LAST_INSERT_ID() - 3 + 4;
SET @integral_id = @derivative_id + 1;
SET @matrix_id = @integral_id + 1;
SET @determinant_id = @matrix_id + 1;
SET @random_var_id = @determinant_id + 1;
SET @prob_dist_id = @random_var_id + 1;
SET @first_order_ode_id = @prob_dist_id + 1;
SET @second_order_ode_id = @first_order_ode_id + 1;

INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@calc_id, @derivative_id, 'PART_OF', '导数是微积分的一部分'),
(@calc_id, @integral_id, 'PART_OF', '积分是微积分的一部分'),
(@linear_id, @matrix_id, 'PART_OF', '矩阵是线性代数的一部分'),
(@linear_id, @determinant_id, 'PART_OF', '行列式是线性代数的一部分'),
(@prob_id, @random_var_id, 'PART_OF', '随机变量是概率论的一部分'),
(@prob_id, @prob_dist_id, 'PART_OF', '概率分布是概率论的一部分'),
(@ode_id, @first_order_ode_id, 'PART_OF', '一阶微分方程是常微分方程的一部分'),
(@ode_id, @second_order_ode_id, 'PART_OF', '二阶微分方程是常微分方程的一部分');

-- 计算机网络关系
INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@network_id, @protocol_id, 'PART_OF', '网络协议是计算机网络的一部分'),
(@network_id, @topology_id, 'PART_OF', '网络拓扑是计算机网络的一部分'),
(@network_id, @security_id, 'PART_OF', '网络安全是计算机网络的一部分');

-- 获取TCP/IP、HTTP/HTTPS、路由算法、防火墙的ID
SET @tcp_ip_id = LAST_INSERT_ID() - 2 + 3;
SET @http_id = @tcp_ip_id + 1;
SET @routing_id = @http_id + 1;
SET @firewall_id = @routing_id + 1;

INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@protocol_id, @tcp_ip_id, 'PART_OF', 'TCP/IP是网络协议的一部分'),
(@protocol_id, @http_id, 'PART_OF', 'HTTP/HTTPS是网络协议的一部分'),
(@topology_id, @routing_id, 'PART_OF', '路由算法是网络拓扑的一部分'),
(@security_id, @firewall_id, 'PART_OF', '防火墙是网络安全的一部分');

-- 操作系统原理关系
INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@os_id, @process_id, 'PART_OF', '进程管理是操作系统原理的一部分'),
(@os_id, @memory_id, 'PART_OF', '内存管理是操作系统原理的一部分'),
(@os_id, @file_id, 'PART_OF', '文件系统是操作系统原理的一部分'),
(@os_id, @device_id, 'PART_OF', '设备管理是操作系统原理的一部分');

-- 获取进程调度、进程同步、分页存储、分段存储、文件结构、文件操作、设备驱动、I/O控制的ID
SET @scheduling_id = LAST_INSERT_ID() - 3 + 4;
SET @sync_id = @scheduling_id + 1;
SET @paging_id = @sync_id + 1;
SET @segmentation_id = @paging_id + 1;
SET @file_struct_id = @segmentation_id + 1;
SET @file_op_id = @file_struct_id + 1;
SET @driver_id = @file_op_id + 1;
SET @io_id = @driver_id + 1;

INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@process_id, @scheduling_id, 'PART_OF', '进程调度是进程管理的一部分'),
(@process_id, @sync_id, 'PART_OF', '进程同步是进程管理的一部分'),
(@memory_id, @paging_id, 'PART_OF', '分页存储是内存管理的一部分'),
(@memory_id, @segmentation_id, 'PART_OF', '分段存储是内存管理的一部分'),
(@file_id, @file_struct_id, 'PART_OF', '文件结构是文件系统的一部分'),
(@file_id, @file_op_id, 'PART_OF', '文件操作是文件系统的一部分'),
(@device_id, @driver_id, 'PART_OF', '设备驱动是设备管理的一部分'),
(@device_id, @io_id, 'PART_OF', 'I/O控制是设备管理的一部分');

-- 数据库系统概论关系
INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@db_id, @model_id, 'PART_OF', '数据库模型是数据库系统概论的一部分'),
(@db_id, @sql_id, 'PART_OF', 'SQL语言是数据库系统概论的一部分'),
(@db_id, @tx_id, 'PART_OF', '事务管理是数据库系统概论的一部分'),
(@db_id, @design_id, 'PART_OF', '数据库设计是数据库系统概论的一部分');

-- 获取关系模型、ER模型、查询语句、数据定义、ACID特性、并发控制、范式理论、数据库规划的ID
SET @relational_model_id = LAST_INSERT_ID() - 3 + 4;
SET @er_model_id = @relational_model_id + 1;
SET @query_stmt_id = @er_model_id + 1;
SET @ddl_id = @query_stmt_id + 1;
SET @acid_id = @ddl_id + 1;
SET @concurrency_id = @acid_id + 1;
SET @normalization_id = @concurrency_id + 1;
SET @planning_id = @normalization_id + 1;

INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@model_id, @relational_model_id, 'PART_OF', '关系模型是数据库模型的一部分'),
(@model_id, @er_model_id, 'PART_OF', 'ER模型是数据库模型的一部分'),
(@sql_id, @query_stmt_id, 'PART_OF', '查询语句是SQL语言的一部分'),
(@sql_id, @ddl_id, 'PART_OF', '数据定义是SQL语言的一部分'),
(@tx_id, @acid_id, 'PART_OF', 'ACID特性是事务管理的一部分'),
(@tx_id, @concurrency_id, 'PART_OF', '并发控制是事务管理的一部分'),
(@design_id, @normalization_id, 'PART_OF', '范式理论是数据库设计的一部分'),
(@design_id, @planning_id, 'PART_OF', '数据库规划是数据库设计的一部分');

-- 数据结构与算法关系
INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@ds_id, @linear_struct_id, 'PART_OF', '线性结构是数据结构与算法的一部分'),
(@ds_id, @tree_struct_id, 'PART_OF', '树形结构是数据结构与算法的一部分'),
(@ds_id, @graph_struct_id, 'PART_OF', '图结构是数据结构与算法的一部分'),
(@ds_id, @sort_id, 'PART_OF', '排序算法是数据结构与算法的一部分');

-- 获取数组、链表、二叉树、平衡树、图的遍历、最短路径、快速排序、归并排序的ID
SET @array_id = LAST_INSERT_ID() - 3 + 4;
SET @linked_list_id = @array_id + 1;
SET @binary_tree_id = @linked_list_id + 1;
SET @balanced_tree_id = @binary_tree_id + 1;
SET @graph_traversal_id = @balanced_tree_id + 1;
SET @shortest_path_id = @graph_traversal_id + 1;
SET @quick_sort_id = @shortest_path_id + 1;
SET @merge_sort_id = @quick_sort_id + 1;

INSERT INTO knowledge_relation (source_node_id, target_node_id, relation_type, description) VALUES
(@linear_struct_id, @array_id, 'PART_OF', '数组是线性结构的一部分'),
(@linear_struct_id, @linked_list_id, 'PART_OF', '链表是线性结构的一部分'),
(@tree_struct_id, @binary_tree_id, 'PART_OF', '二叉树是树形结构的一部分'),
(@tree_struct_id, @balanced_tree_id, 'PART_OF', '平衡树是树形结构的一部分'),
(@graph_struct_id, @graph_traversal_id, 'PART_OF', '图的遍历是图结构的一部分'),
(@graph_struct_id, @shortest_path_id, 'PART_OF', '最短路径是图结构的一部分'),
(@sort_id, @quick_sort_id, 'PART_OF', '快速排序是排序算法的一部分'),
(@sort_id, @merge_sort_id, 'PART_OF', '归并排序是排序算法的一部分');