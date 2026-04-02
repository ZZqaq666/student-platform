import request from '@/utils/request'

/**
 * 书籍管理API
 */

/**
 * 分页查询书籍列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getBookList(params) {
  return request({
    url: '/admin/books',
    method: 'get',
    params
  })
}

/**
 * 获取书籍详情
 * @param {number} id 书籍ID
 * @returns {Promise}
 */
export function getBookDetail(id) {
  return request({
    url: `/admin/books/${id}`,
    method: 'get'
  })
}

/**
 * 创建书籍
 * @param {Object} data 书籍数据
 * @returns {Promise}
 */
export function createBook(data) {
  return request({
    url: '/admin/books',
    method: 'post',
    data
  })
}

/**
 * 更新书籍
 * @param {number} id 书籍ID
 * @param {Object} data 书籍数据
 * @returns {Promise}
 */
export function updateBook(id, data) {
  return request({
    url: `/admin/books/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除书籍
 * @param {number} id 书籍ID
 * @returns {Promise}
 */
export function deleteBook(id) {
  return request({
    url: `/admin/books/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除书籍
 * @param {Array} ids 书籍ID列表
 * @returns {Promise}
 */
export function batchDeleteBooks(ids) {
  return request({
    url: '/admin/books/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 批量更新分类
 * @param {Object} data { ids: [], category: '' }
 * @returns {Promise}
 */
export function batchUpdateCategory(data) {
  return request({
    url: '/admin/books/batch/category',
    method: 'put',
    data
  })
}

/**
 * 批量更新状态
 * @param {Object} data { ids: [], status: '' }
 * @returns {Promise}
 */
export function batchUpdateStatus(data) {
  return request({
    url: '/admin/books/batch/status',
    method: 'put',
    data
  })
}

/**
 * 获取章节列表
 * @param {number} bookId 书籍ID
 * @returns {Promise}
 */
export function getChapterList(bookId) {
  return request({
    url: `/admin/books/${bookId}/chapters`,
    method: 'get'
  })
}

/**
 * 创建章节
 * @param {number} bookId 书籍ID
 * @param {Object} data 章节数据
 * @returns {Promise}
 */
export function createChapter(bookId, data) {
  return request({
    url: `/admin/books/${bookId}/chapters`,
    method: 'post',
    data
  })
}

/**
 * 更新章节
 * @param {number} bookId 书籍ID
 * @param {number} chapterId 章节ID
 * @param {Object} data 章节数据
 * @returns {Promise}
 */
export function updateChapter(bookId, chapterId, data) {
  return request({
    url: `/admin/books/${bookId}/chapters/${chapterId}`,
    method: 'put',
    data
  })
}

/**
 * 删除章节
 * @param {number} bookId 书籍ID
 * @param {number} chapterId 章节ID
 * @returns {Promise}
 */
export function deleteChapter(bookId, chapterId) {
  return request({
    url: `/admin/books/${bookId}/chapters/${chapterId}`,
    method: 'delete'
  })
}

/**
 * 更新章节排序
 * @param {number} bookId 书籍ID
 * @param {Array} chapters 章节列表
 * @returns {Promise}
 */
export function updateChapterSort(bookId, chapters) {
  return request({
    url: `/admin/books/${bookId}/chapters/sort`,
    method: 'put',
    data: chapters
  })
}

/**
 * 设置主图
 * @param {number} bookId 书籍ID
 * @param {number} imageId 图片ID
 * @returns {Promise}
 */
export function setPrimaryImage(bookId, imageId) {
  return request({
    url: `/admin/books/${bookId}/images/${imageId}/primary`,
    method: 'put'
  })
}

/**
 * 删除图片
 * @param {number} bookId 书籍ID
 * @param {number} imageId 图片ID
 * @returns {Promise}
 */
export function deleteImage(bookId, imageId) {
  return request({
    url: `/admin/books/${bookId}/images/${imageId}`,
    method: 'delete'
  })
}

/**
 * 批量删除图片
 * @param {number} bookId 书籍ID
 * @param {Array} imageIds 图片ID列表
 * @returns {Promise}
 */
export function batchDeleteImages(bookId, imageIds) {
  return request({
    url: `/admin/books/${bookId}/images/batch`,
    method: 'delete',
    data: imageIds
  })
}
