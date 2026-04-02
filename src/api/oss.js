import request from '@/utils/request'

/**
 * OSS文件上传API
 */

/**
 * 获取OSS上传签名
 * @param {string} fileType 文件类型：cover-封面/content-内容
 * @returns {Promise}
 */
export function getUploadSignature(fileType = 'general') {
  return request({
    url: '/oss/signature',
    method: 'get',
    params: { fileType }
  })
}

/**
 * 获取OSS上传签名（带文件名）
 * @param {string} filename 原始文件名
 * @param {string} fileType 文件类型
 * @returns {Promise}
 */
export function getUploadSignatureWithFilename(filename, fileType = 'general') {
  return request({
    url: '/oss/signature/with-filename',
    method: 'get',
    params: { filename, fileType }
  })
}

/**
 * 直传文件到OSS
 * @param {Object} params 上传参数
 * @param {File} file 文件对象
 * @returns {Promise}
 */
export function uploadToOss(params, file) {
  const formData = new FormData()
  formData.append('key', params.dir + file.name)
  formData.append('policy', params.policy)
  formData.append('OSSAccessKeyId', params.accessid)
  formData.append('success_action_status', '200')
  formData.append('signature', params.signature)
  formData.append('file', file)

  return fetch(params.host, {
    method: 'POST',
    body: formData
  }).then(response => {
    if (response.ok || response.status === 200) {
      return {
        code: 200,
        data: {
          url: params.host + '/' + params.dir + file.name,
          filename: file.name
        }
      }
    }
    throw new Error('上传失败')
  })
}
